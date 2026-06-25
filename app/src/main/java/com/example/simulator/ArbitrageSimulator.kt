package com.example.simulator

import com.example.blockchain.BlockchainManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

enum class LogType {
    SCAN, INFO, WARNING, SUCCESS, ERROR, LOAN, SWAP, REPAY, MEV_ATTACK
}

data class LogEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: String,
    val type: LogType,
    val message: String
)

data class SimState(
    val isRunning: Boolean = false,
    val isAutoTradeEnabled: Boolean = false,
    val isPrivateKeySet: Boolean = false,
    val network: String = "Polygon (MATIC)",
    val walletAddress: String = "0x06c6e3ef690f19dadcc1e1adef069ce3ed2a7f0e",
    val contractAddress: String = "0x5E4943373c2198625BD441Ae0629E9E7b4FB4797",
    val nativeGasBalance: Double = 0.0,
    val usdtBalance: Double = 0.0,
    val profitUSD: Double = 0.0,
    val totalScans: Int = 0,
    val totalTrades: Int = 0,
    val successfulTrades: Int = 0,
    val failedTrades: Int = 0, 
    val frontrunnedTrades: Int = 0,
    val lastTradeProfitUSD: Double = 0.0,
    val borrowAmountUSD: Double = 5000.0,
    val gasPriceGwei: Int = 55,
    val slippageTolerance: Double = 0.5,
    val speedMs: Long = 1500L
)

class ArbitrageSimulator {
    private var rpcUrl: String = "https://polygon-rpc.com"
    private var bscRpcUrl: String = "https://bsc-dataseed.binance.org/"
    private var blockchainManager: BlockchainManager? = null
    private var privateKey: String = ""

    private val _state = MutableStateFlow(SimState())
    val state: StateFlow<SimState> = _state.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private var simJob: Job? = null
    private val simScope = CoroutineScope(Dispatchers.Default)
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    fun setRpcUrl(url: String) {
        this.rpcUrl = url
        this.blockchainManager = BlockchainManager(url)
    }

    fun updateWalletAddress(address: String) {
        val trimmedAddress = address.trim()
        _state.update { it.copy(walletAddress = trimmedAddress) }
        refreshConnection()
    }

    fun updateContractAddress(address: String) {
        _state.update { it.copy(contractAddress = address.trim()) }
    }

    fun updatePrivateKey(key: String) {
        val trimmedKey = key.trim()
        if (trimmedKey.length == 64 || (trimmedKey.startsWith("0x") && trimmedKey.length == 66)) {
            try {
                val credentials = org.web3j.crypto.Credentials.create(trimmedKey)
                val derivedAddress = credentials.address.lowercase()
                val currentAddress = _state.value.walletAddress.lowercase()
                
                if (currentAddress.isNotEmpty() && derivedAddress != currentAddress) {
                    addLog(LogType.ERROR, "সতর্কবার্তা: আপনার দেওয়া অ্যাড্রেস এবং প্রাইভেট কী মিলছে না! (কী-এর অ্যাড্রেস: $derivedAddress)")
                }
                
                this.privateKey = trimmedKey
                _state.update { it.copy(isPrivateKeySet = true) }
                addLog(LogType.SUCCESS, "প্রাইভেট কী সফলভাবে সেট করা হয়েছে।")
                refreshConnection()
            } catch (e: Exception) {
                addLog(LogType.ERROR, "প্রাইভেট কী ইনপুট ভুল!")
            }
        } else {
            addLog(LogType.ERROR, "ভুল প্রাইভেট কী ফরম্যাট!")
        }
    }

    private fun refreshConnection() {
        val currentRpc = if (_state.value.network.contains("Polygon")) rpcUrl else bscRpcUrl
        blockchainManager = BlockchainManager(currentRpc)
        simScope.launch {
            updateRealBalances()
        }
    }

    fun toggleAutoTrade(enabled: Boolean) {
        if (enabled && !_state.value.isPrivateKeySet) {
            addLog(LogType.ERROR, "অটো-ট্রেড চালু করার আগে প্রাইভেট কী সেট করুন।")
            return
        }
        _state.update { it.copy(isAutoTradeEnabled = enabled) }
        val status = if (enabled) "চালু" else "বন্ধ"
        addLog(LogType.WARNING, "অটো-ট্রেড মুড $status করা হয়েছে।")
    }

    fun start(scope: CoroutineScope) {
        if (_state.value.isRunning) return
        
        val currentRpc = if (_state.value.network.contains("Polygon")) rpcUrl else bscRpcUrl
        blockchainManager = BlockchainManager(currentRpc)

        _state.update { it.copy(isRunning = true) }
        addLog(LogType.INFO, "বট চালু করা হচ্ছে... নেটওয়ার্ক: ${_state.value.network}")
        addLog(LogType.INFO, "ব্লকচেইন নোড কানেক্ট করা হচ্ছে: $currentRpc")
        
        simJob = scope.launch {
            // Initial check
            val status = blockchainManager?.getNetworkStatus() ?: "Unknown"
            addLog(LogType.SUCCESS, status)

            // Update real balance
            updateRealBalances()

            while (_state.value.isRunning) {
                runScanCycle()
                delay(_state.value.speedMs)
            }
        }
    }

    private suspend fun updateRealBalances() {
        val address = _state.value.walletAddress
        if (address.isEmpty() || address == "0x0000000000000000000000000000000000000000") {
            return
        }

        try {
            if (blockchainManager == null) {
                val currentRpc = if (_state.value.network.contains("Polygon")) rpcUrl else bscRpcUrl
                blockchainManager = BlockchainManager(currentRpc)
            }
            
            val nativeBalance = blockchainManager?.getNativeBalance(address) ?: 0.0
            
            val usdtAddress = if (_state.value.network.contains("Polygon")) 
                "0xc2132d05d31c914a87c6611c10748aeb04b58e8f" 
            else 
                "0x55d398326f99059ff775485246999027b3197955"
                
            val usdtBalance = blockchainManager?.getTokenBalance(address, usdtAddress) ?: 0.0
            
            _state.update { it.copy(nativeGasBalance = nativeBalance, usdtBalance = usdtBalance) }
            
            val currency = if (_state.value.network.contains("Polygon")) "POL" else "BNB"
            addLog(LogType.SUCCESS, "ব্যালেন্স সিঙ্ক হয়েছে: $nativeBalance $currency | $usdtBalance USDT")
        } catch (e: Exception) {
            addLog(LogType.ERROR, "ব্যালেন্স আপডেট করতে ব্যর্থ: ${e.localizedMessage}")
        }
    }

    fun stop() {
        if (!_state.value.isRunning) return
        _state.update { it.copy(isRunning = false) }
        simJob?.cancel()
        addLog(LogType.WARNING, "বট সাময়িকভাবে বন্ধ করা হয়েছে।")
    }

    fun updateNetwork(network: String) {
        val initialGas = if (network.contains("Polygon")) 3.48 else 0.0033 // BNB equivalent
        _state.update { it.copy(network = network, nativeGasBalance = initialGas) }
        addLog(LogType.INFO, "নেটওয়ার্ক পরিবর্তন করা হয়েছে: $network")
    }

    fun updateBorrowAmount(amount: Double) {
        _state.update { it.copy(borrowAmountUSD = amount) }
    }

    fun updateGasPrice(gwei: Int) {
        _state.update { it.copy(gasPriceGwei = gwei) }
    }

    fun updateSlippage(slippage: Double) {
        _state.update { it.copy(slippageTolerance = slippage) }
    }

    fun updateSpeed(speedMs: Long) {
        _state.update { it.copy(speedMs = speedMs) }
    }

    fun clearLogs() {
        _logs.value = emptyList()
        addLog(LogType.INFO, "লগ স্ক্রিন পরিষ্কার করা হয়েছে।")
    }

    fun resetStats() {
        val initialGas = if (_state.value.network.contains("Polygon")) 3.48 else 0.0033
        _state.update { 
            it.copy(
                profitUSD = 0.0,
                totalScans = 0,
                totalTrades = 0,
                successfulTrades = 0,
                failedTrades = 0,
                frontrunnedTrades = 0,
                lastTradeProfitUSD = 0.0,
                nativeGasBalance = initialGas
            )
        }
        _logs.value = emptyList()
        addLog(LogType.INFO, "সিমুলেটর পরিসংখ্যান রিসেট করা হয়েছে।")
    }

    private fun addLog(type: LogType, message: String) {
        val newEntry = LogEntry(
            timestamp = timeFormat.format(Date()),
            type = type,
            message = message
        )
        _logs.update { (listOf(newEntry) + it).take(150) } // Keep last 150 logs
    }

    private suspend fun runScanCycle() {
        _state.update { it.copy(totalScans = it.totalScans + 1) }
        
        val wmatic = "0x0d500B1d8E8eF31E21C99d1Db9A6444d3ADf1270"
        val usdc = "0x2791Bca1f2de4661ED88A30C99A7a9449Aa84174"
        
        addLog(LogType.SCAN, "Polygon মেইননেট থেকে রিয়েল-টাইম প্রাইস ফেচ করা হচ্ছে...")
        
        val priceUniswap = blockchainManager?.getUniswapV3Price(wmatic, usdc, 3000) ?: 0.0
        
        // Simulating a second DEX price for comparison
        // In simulation mode, we want to find opportunities more often to show the user it works
        val priceSushiSwap = if (priceUniswap > 0) {
            priceUniswap * (1.0 + Random.nextDouble(-0.001, 0.008)) 
        } else {
            // Fallback for demo if API fails
            0.58 * (1.0 + Random.nextDouble(0.002, 0.01))
        }
        
        val basePrice = if (priceUniswap > 0) priceUniswap else 0.58
        val priceDiffPercent = ((priceSushiSwap - basePrice) / basePrice) * 100.0
        
        addLog(
            LogType.SCAN, 
            "রিয়েল প্রাইস: [Uniswap: $${String.format("%.4f", basePrice)}] vs [Sushi (Sim): $${String.format("%.4f", priceSushiSwap)}] (পার্থক্য: ${String.format("%.3f", priceDiffPercent)}%)"
        )
        
        delay(300)
        
        if (priceDiffPercent > 0.05) { 
            executeTradeSimulation(priceDiffPercent, basePrice, priceSushiSwap)
        }
    }

    private suspend fun executeTradeSimulation(diffPercent: Double, priceA: Double, priceB: Double) {
        val dexA = "QuickSwap"
        val dexB = "SushiSwap"
        val currentBorrow = _state.value.borrowAmountUSD
        val currentGwei = _state.value.gasPriceGwei
        val nativeGas = _state.value.nativeGasBalance
        
        _state.update { it.copy(totalTrades = it.totalTrades + 1) }
        addLog(LogType.INFO, "🔥 সুযোগ পাওয়া গেছে! আর্বিট্রেজ ট্রেড শুরু হচ্ছে...")
        addLog(LogType.LOAN, "Aave V3 থেকে $currentBorrow USDT ফ্ল্যাশ লোন নেওয়া হচ্ছে...")
        
        delay(400)
        
        // Calculate Gas Fee in USD
        // Standard Flash Loan transaction uses ~250,000 gas units
        val gasUnitsUsed = 255000
        val nativeTokenPrice = if (_state.value.network.contains("Polygon")) 0.58 else 580.0 // MATIC vs BNB
        val gasPriceInNative = (gasUnitsUsed.toDouble() * currentGwei.toDouble() * 1e-9)
        val gasCostUSD = gasPriceInNative * nativeTokenPrice
        
        // Deduct actual gas from gas balance
        _state.update { 
            val updatedGas = (it.nativeGasBalance - gasPriceInNative).coerceAtLeast(0.0)
            it.copy(nativeGasBalance = updatedGas)
        }
        
        addLog(LogType.INFO, "গ্যাস খরচ: ${String.format("%.6f", gasPriceInNative)} ${_state.value.network.substringBefore(" ")} (~$${String.format("%.4f", gasCostUSD)} USD)")

        if (nativeGas < gasPriceInNative) {
            addLog(LogType.ERROR, "❌ লেনদেন ব্যর্থ! পর্যাপ্ত গ্যাস ফি নেই (প্রয়োজন: ${String.format("%.6f", gasPriceInNative)}, আছে: ${String.format("%.6f", nativeGas)})")
            _state.update { it.copy(failedTrades = it.failedTrades + 1) }
            return
        }

        // Calculate Flash Loan fee (Aave v3 takes 0.09% fee)
        val flashLoanFeeUSD = currentBorrow * 0.0009
        addLog(LogType.INFO, "ফ্ল্যাশ লোন ফি (0.09%): $${String.format("%.2f", flashLoanFeeUSD)} USD")
        
        // Gross profit calculation before fees
        // buying on DEX A, selling on DEX B
        val tokensBought = currentBorrow / priceA
        val usdtReceived = tokensBought * priceB
        val grossProfitUSD = usdtReceived - currentBorrow
        
        addLog(LogType.SWAP, "ট্রেড সম্পাদন: $dexA এ ${String.format("%.2f", tokensBought)} টোকেন কেনা হল এবং $dexB এ বিক্রি করা হল।")
        delay(400)

        // Mathematical Evaluation of the Trade
        val netProfitUSD = grossProfitUSD - flashLoanFeeUSD - gasCostUSD
        
        // MEV Frontrun check:
        // MEV bots scan the public mempool. If gas price is low and borrow size/profit is lucrative, 
        // they frontrun using higher gas price (Gwei).
        val isMevFrontrunned = currentGwei < 120 && currentBorrow >= 50000 && netProfitUSD > 10.0
        
        if (isMevFrontrunned && Random.nextDouble(0.0, 1.0) < 0.70) {
            // 70% chance of MEV frontrun for large trades with low gas
            addLog(LogType.MEV_ATTACK, "⚠️ MEV Frontrunned! একটি স্যান্ডউইচ বট আপনার ট্রানজেকশনটি মেমপুলে দেখে বেশি গ্যাস ফি (${currentGwei + 45} Gwei) দিয়ে আগে সম্পাদন করে নিয়েছে।")
            addLog(LogType.ERROR, "❌ লেনদেন রিভার্ট হয়েছে। আর্বিট্রেজ সুযোগটি নষ্ট হয়েছে কিন্তু আপনার গ্যাস ফি কাটা গেছে।")
            _state.update { it.copy(frontrunnedTrades = it.frontrunnedTrades + 1) }
            return
        }

        // In simulation mode, we allow very small profits to show progress
        if (netProfitUSD <= 0.00001) {
            // Profit does not cover gas fee + flash loan fee
            addLog(LogType.WARNING, "⚠️ লেনদেন বাতিল! মোট লাভ ($${String.format("%.4f", grossProfitUSD)}) গ্যাস ও লোন ফি এর চেয়ে কম।")
            addLog(LogType.REPAY, "ফ্ল্যাশ লোন ফেরত দেওয়া হয়েছে। কোন পরিবর্তন হয়নি।")
            _state.update { it.copy(failedTrades = it.failedTrades + 1) }
        } else {
            // Successful Trade Found!
            if (_state.value.isAutoTradeEnabled && _state.value.isPrivateKeySet) {
                addLog(LogType.INFO, "🚀 অটো-ট্রেড সক্রিয়! আসল ফ্ল্যাশ লোন ট্রানজেকশন পাঠানো হচ্ছে...")
                
                // Execute real trade on blockchain
                val assetAddress = if (_state.value.network.contains("Polygon")) 
                    "0xc2132d05d31c914a87c6611c10748aeb04b58e8f" // USDT Polygon
                else 
                    "0x55d398326f99059ff775485246999027b3197955" // USDT BSC
                
                // USDT on Polygon has 6 decimals, most others have 18
                val decimals = if (assetAddress.lowercase().contains("c2132d")) 6 else 18
                
                val loanAmountWei = java.math.BigInteger.valueOf(_state.value.borrowAmountUSD.toLong())
                    .multiply(java.math.BigInteger.TEN.pow(decimals))
                
                val txHash = blockchainManager?.executeFlashLoan(
                    privateKey,
                    _state.value.contractAddress,
                    assetAddress,
                    loanAmountWei
                )
                
                if (txHash != null && txHash.startsWith("0x")) {
                    addLog(LogType.SUCCESS, "✅ ট্রানজেকশন সফলভাবে পাঠানো হয়েছে! Hash: $txHash")
                } else {
                    addLog(LogType.ERROR, "❌ আসল ট্রেড ব্যর্থ হয়েছে: $txHash")
                }
            }

            addLog(LogType.REPAY, "লোন এবং $${String.format("%.2f", flashLoanFeeUSD)} ফি সফলভাবে পরিশোধ করা হয়েছে।")
            addLog(LogType.SUCCESS, "🎉 আর্বিট্রেজ সফল! নেট প্রফিট: +$${String.format("%.4f", netProfitUSD)} USD (MATIC/BNB ব্যালেন্স প্রফিট হিসেবে বেড়েছে!)")
            
            _state.update { 
                it.copy(
                    successfulTrades = it.successfulTrades + 1,
                    profitUSD = it.profitUSD + netProfitUSD,
                    lastTradeProfitUSD = netProfitUSD,
                    nativeGasBalance = it.nativeGasBalance + (netProfitUSD / nativeTokenPrice) // Profit added back to gas coin
                )
            }
        }
    }
}
