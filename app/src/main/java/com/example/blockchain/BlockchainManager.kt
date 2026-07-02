package com.yourcompany.flasharb.blockchain

import android.util.Log
import com.yourcompany.flasharb.blockchain.engine.DynamicGasOracle
import com.yourcompany.flasharb.blockchain.engine.PolygonalRpcClient
import com.yourcompany.flasharb.blockchain.engine.TransactionMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.generated.Uint24
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.tx.RawTransactionManager
import java.math.BigDecimal
import java.math.BigInteger

class BlockchainManager(private val rpcUrls: List<String>) {
    private val rpcClient = PolygonalRpcClient(rpcUrls)
    private val gasOracle = DynamicGasOracle(rpcClient)
    private val txMonitor = TransactionMonitor(rpcClient)

    suspend fun executeFlashLoan(
        privateKey: String,
        contractAddress: String,
        tokenAddress: String,
        amount: BigInteger,
        tokenToBuy: String,
        minProfit: BigInteger
    ): String = rpcClient.executeWithFallback { web3j ->
        try {
            val credentials = Credentials.create(privateKey)
            val transactionManager = RawTransactionManager(web3j, credentials)
            
            // requestFlashLoan(address,uint256,address,uint256)
            val function = org.web3j.abi.datatypes.Function(
                "requestFlashLoan",
                listOf(Address(tokenAddress), Uint256(amount), Address(tokenToBuy), Uint256(minProfit)),
                emptyList()
            )
            
            val encodedFunction = FunctionEncoder.encode(function)
            val ethGasPrice = gasOracle.getOptimizedGasPrice()
            val gasLimit = BigInteger.valueOf(1500000) 
            
            val ethSendTransaction = transactionManager.sendTransaction(
                ethGasPrice,
                gasLimit,
                contractAddress,
                encodedFunction,
                BigInteger.ZERO
            )

            if (ethSendTransaction.hasError()) {
                "Error: ${ethSendTransaction.error.message}"
            } else {
                ethSendTransaction.transactionHash
            }
        } catch (e: Exception) {
            Log.e("BlockchainManager", "Trade Execution Failed", e)
            "Failed: ${e.localizedMessage}"
        }
    }

    suspend fun getNetworkStatus(): String = rpcClient.executeWithFallback { web3j ->
        try {
            val clientVersion = web3j.web3ClientVersion().send()
            "ব্লকচেইন নোড সফলভাবে সংযুক্ত: ${clientVersion.web3ClientVersion}"
        } catch (e: Exception) {
            Log.e("BlockchainManager", "Error connecting to RPC", e)
            "কানেকশন ব্যর্থ হয়েছে: ${e.localizedMessage}"
        }
    }

    suspend fun getNativeBalance(walletAddress: String): Double = rpcClient.executeWithFallback { web3j ->
        try {
            val balanceResponse = web3j.ethGetBalance(walletAddress, DefaultBlockParameterName.LATEST).send()
            if (balanceResponse.hasError()) {
                Log.e("BlockchainManager", "Balance error: ${balanceResponse.error.message}")
                return@executeWithFallback 0.0
            }
            val balanceWei = balanceResponse.balance
            balanceWei.toBigDecimal().divide(BigDecimal.TEN.pow(18)).toDouble()
        } catch (e: Exception) {
            Log.e("BlockchainManager", "Error fetching balance", e)
            0.0
        }
    }

    suspend fun getTokenBalance(walletAddress: String, tokenAddress: String): Double = rpcClient.executeWithFallback { web3j ->
        try {
            val function = org.web3j.abi.datatypes.Function(
                "balanceOf",
                listOf(Address(walletAddress)),
                listOf(object : TypeReference<Uint256>() {})
            )
            val encodedFunction = FunctionEncoder.encode(function)
            val response = web3j.ethCall(
                Transaction.createEthCallTransaction(null, tokenAddress, encodedFunction),
                DefaultBlockParameterName.LATEST
            ).send()

            if (response.hasError() || response.value == null) return@executeWithFallback 0.0

            val results = FunctionReturnDecoder.decode(response.value, function.outputParameters)
            if (results.isNotEmpty()) {
                val balance = results[0].value as BigInteger
                val decimals = if (tokenAddress.lowercase() == "0xc2132d05d31c914a87c6611c10748aeb04b58e8f") 6 else 18
                balance.toBigDecimal().divide(BigDecimal.TEN.pow(decimals)).toDouble()
            } else {
                0.0
            }
        } catch (e: Exception) {
            Log.e("BlockchainManager", "Error fetching token balance", e)
            0.0
        }
    }

    suspend fun getUniswapV3Price(
        tokenIn: String,
        tokenOut: String,
        fee: Int = 3000,
        amountIn: BigInteger = BigInteger.TEN.pow(18)
    ): Double = rpcClient.executeWithFallback { web3j ->
        try {
            val quoterAddress = "0xb27308f9f90d607463bb33ea1bebb41c27ce5ab6"
            
            val function = org.web3j.abi.datatypes.Function(
                "quoteExactInputSingle",
                listOf(Address(tokenIn), Address(tokenOut), Uint24(fee.toLong()), Uint256(amountIn), Uint256(0L)),
                listOf(object : TypeReference<Uint256>() {})
            )
            
            val encodedFunction = FunctionEncoder.encode(function)
            val response = web3j.ethCall(
                Transaction.createEthCallTransaction(null, quoterAddress, encodedFunction),
                DefaultBlockParameterName.LATEST
            ).send()

            if (response.hasError() || response.value == null) return@executeWithFallback 0.0

            val results = FunctionReturnDecoder.decode(response.value, function.outputParameters)
            if (results.isNotEmpty()) {
                val amountOut = results[0].value as BigInteger
                amountOut.toBigDecimal().divide(BigDecimal.TEN.pow(18)).toDouble()
            } else {
                0.0
            }
        } catch (e: Exception) {
            Log.e("BlockchainManager", "Error fetching Uniswap price", e)
            0.0
        }
    }

    suspend fun getQuickSwapPrice(
        tokenIn: String,
        tokenOut: String,
        amountIn: BigInteger = BigInteger.TEN.pow(18)
    ): Double = rpcClient.executeWithFallback { web3j ->
        try {
            val routerAddress = "0xa5E0829CaCEd8fFDD4De3c43696c57F7D7A678ff"
            
            val path = org.web3j.abi.datatypes.DynamicArray(
                Address::class.java,
                listOf(Address(tokenIn), Address(tokenOut))
            )
            
            val function = org.web3j.abi.datatypes.Function(
                "getAmountsOut",
                listOf(Uint256(amountIn), path),
                listOf(object : TypeReference<org.web3j.abi.datatypes.DynamicArray<Uint256>>() {})
            )
            
            val encodedFunction = FunctionEncoder.encode(function)
            val response = web3j.ethCall(
                Transaction.createEthCallTransaction(null, routerAddress, encodedFunction),
                DefaultBlockParameterName.LATEST
            ).send()

            if (response.hasError() || response.value == null) return@executeWithFallback 0.0

            val results = FunctionReturnDecoder.decode(response.value, function.outputParameters)
            if (results.isNotEmpty()) {
                val amounts = results[0].value as List<Uint256>
                amounts.last().value.toBigDecimal().divide(BigDecimal.TEN.pow(18)).toDouble()
            } else {
                0.0
            }
        } catch (e: Exception) {
            Log.e("BlockchainManager", "Error fetching QuickSwap price", e)
            0.0
        }
    }
}
