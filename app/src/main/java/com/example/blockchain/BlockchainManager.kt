package com.example.blockchain

import android.util.Log
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
import org.web3j.protocol.http.HttpService
import org.web3j.tx.RawTransactionManager
import java.math.BigDecimal
import java.math.BigInteger

class BlockchainManager(private val rpcUrl: String) {
    private val web3j: Web3j = Web3j.build(HttpService(rpcUrl))

    suspend fun executeFlashLoan(
        privateKey: String,
        contractAddress: String,
        tokenAddress: String,
        amount: BigInteger
    ): String = withContext(Dispatchers.IO) {
        try {
            val credentials = Credentials.create(privateKey)
            val transactionManager = RawTransactionManager(web3j, credentials)
            
            // Defining the function to match the smart contract: requestFlashLoan(address, uint256)
            val function = org.web3j.abi.datatypes.Function(
                "requestFlashLoan",
                listOf(Address(tokenAddress), Uint256(amount)),
                emptyList()
            )
            
            val encodedFunction = FunctionEncoder.encode(function)
            
            // Gas price and limit - Increased for complex flash loans
            val ethGasPrice = web3j.ethGasPrice().send().gasPrice
            val gasLimit = BigInteger.valueOf(1200000)
            
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

    suspend fun getNetworkStatus(): String = withContext(Dispatchers.IO) {
        try {
            val clientVersion = web3j.web3ClientVersion().send()
            "ব্লকচেইন নোড সফলভাবে সংযুক্ত: ${clientVersion.web3ClientVersion}"
        } catch (e: Exception) {
            Log.e("BlockchainManager", "Error connecting to RPC", e)
            "কানেকশন ব্যর্থ হয়েছে: ${e.localizedMessage}"
        }
    }

    suspend fun getNativeBalance(walletAddress: String): Double = withContext(Dispatchers.IO) {
        try {
            val balanceResponse = web3j.ethGetBalance(walletAddress, DefaultBlockParameterName.LATEST).send()
            if (balanceResponse.hasError()) {
                Log.e("BlockchainManager", "Balance error: ${balanceResponse.error.message}")
                return@withContext 0.0
            }
            val balanceWei = balanceResponse.balance
            balanceWei.toBigDecimal().divide(BigDecimal.TEN.pow(18)).toDouble()
        } catch (e: Exception) {
            Log.e("BlockchainManager", "Error fetching balance", e)
            0.0
        }
    }

    /**
     * ERC20/BEP20 টোকেন ব্যালেন্স (যেমন USDT) ফেচ করার মেথড
     */
    suspend fun getTokenBalance(walletAddress: String, tokenAddress: String): Double = withContext(Dispatchers.IO) {
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

            if (response.hasError() || response.value == null) return@withContext 0.0

            val results = FunctionReturnDecoder.decode(response.value, function.outputParameters)
            if (results.isNotEmpty()) {
                val balance = results[0].value as BigInteger
                // USDT usually has 6 decimals on Polygon, but 18 on BSC.
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
    ): Double = withContext(Dispatchers.IO) {
        try {
            // Uniswap V3 Quoter Address on Polygon
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

            if (response.hasError() || response.value == null) return@withContext 0.0

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
}
