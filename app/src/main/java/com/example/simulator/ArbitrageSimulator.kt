package com.example.simulator

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
    val network: String = "Polygon (MATIC)",
    val nativeGasBalance: Double = 3.48, // ~$2 worth of MATIC
    val profitUSD: Double = 0.0,
    val totalScans: Int = 0,
    val totalTrades: Int = 0,
    val successfulTrades: Int = 0,
    val failedTrades: Int = 0, // Aborted due to high gas/low profit
    val frontrunnedTrades: Int = 0, // Sandwich or MEV frontrun
    val lastTradeProfitUSD: Double = 0.0,
    val borrowAmountUSD: Double = 5000.0, // Slider control
    val gasPriceGwei: Int = 55, // Slider control
    val slippageTolerance: Double = 0.5, // %
    val speedMs: Long = 1500L
)

class ArbitrageSimulator {
    private val _state = MutableStateFlow(SimState())
    val state: StateFlow<SimState> = _state.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private var simJob: Job? = null
    private val simScope = CoroutineScope(Dispatchers.Default)
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    fun start(scope: CoroutineScope) {
        if (_state.value.isRunning) return
        _state.update { it.copy(isRunning = true) }
        addLog(LogType.INFO, "বট চালু করা হচ্ছে... নেটওয়ার্ক: ${_state.value.network}")
        addLog(LogType.INFO, "ডিসেন্ট্রালাইজড পুল মনিটরিং শুরু করা হয়েছে (QuickSwap, SushiSwap, Uniswap V3)...")
        
        simJob = scope.launch {
            while (_state.value.isRunning) {
                runScanCycle()
                delay(_state.value.speedMs)
            }
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
        
        val dexA = "QuickSwap"
        val dexB = "SushiSwap"
        val token = "MATIC-USDC"
        
        // Randomly simulate price differences
        val basePrice = 0.582
        val priceDiffPercent = Random.nextDouble(-0.8, 1.2) // Range of price discrepancy
        val priceA = basePrice
        val priceB = basePrice * (1.0 + (priceDiffPercent / 100.0))
        
        addLog(
            LogType.SCAN, 
            "স্ক্যানিং পুল: [DEX A: $dexA - $priceA$] vs [DEX B: $dexB - ${String.format("%.4f", priceB)}$] (পার্থক্য: ${String.format("%.3f", priceDiffPercent)}%)"
        )
        
        delay(300)
        
        if (priceDiffPercent > 0.15) { // Potential arbitrage found!
            executeTradeSimulation(priceDiffPercent, priceA, priceB)
        } else {
            // No trade executed, micro update gas balance sometimes due to continuous scanning if real, but let's keep it static
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
        
        // Deduct actual gas from gas balance (even if trade fails/reverts, gas is paid!)
        _state.update { 
            val updatedGas = (it.nativeGasBalance - gasPriceInNative).coerceAtLeast(0.0)
            it.copy(nativeGasBalance = updatedGas)
        }
        
        addLog(LogType.INFO, "ট্রানজেকশন গ্যাস খরচ: ${String.format("%.6f", gasPriceInNative)} ${_state.value.network.substringBefore(" ")} (~$${String.format("%.4f", gasCostUSD)} USD)")

        if (nativeGas < gasPriceInNative) {
            addLog(LogType.ERROR, "❌ লেনদেন ব্যর্থ! ওয়ালেটে পর্যাপ্ত গ্যাস ফি নেই (প্রয়োজন: ${String.format("%.6f", gasPriceInNative)} MATIC, আপনার আছে: ${String.format("%.6f", nativeGas)} MATIC)")
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

        if (netProfitUSD <= 0) {
            // Profit does not cover gas fee + flash loan fee
            addLog(LogType.WARNING, "⚠️ লেনদেন বাতিল! মোট লাভ ($${String.format("%.4f", grossProfitUSD)}) গ্যাস ও লোন ফি এর চেয়ে কম।")
            addLog(LogType.REPAY, "ফ্ল্যাশ লোন ফেরত দেওয়া হয়েছে। কোন পরিবর্তন হয়নি।")
            _state.update { it.copy(failedTrades = it.failedTrades + 1) }
        } else {
            // Successful Trade!
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
