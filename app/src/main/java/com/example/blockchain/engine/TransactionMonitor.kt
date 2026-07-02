package com.example.blockchain.engine

import kotlinx.coroutines.delay
import org.web3j.protocol.core.methods.response.TransactionReceipt
import java.util.concurrent.TimeoutException

class TransactionMonitor(private val rpcClient: PolygonalRpcClient) {
    suspend fun waitForMining(txHash: String): TransactionReceipt {
        var retries = 0
        while (retries < 30) {
            val receipt = rpcClient.executeWithFallback { web3j ->
                web3j.ethGetTransactionReceipt(txHash).send().transactionReceipt.orElse(null)
            }
            if (receipt != null) {
                return receipt
            }
            delay(2000)
            retries++
        }
        throw TimeoutException("Transaction $txHash not mined in time")
    }
}
