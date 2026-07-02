package com.yourcompany.flasharb.blockchain.engine

import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeout

class PolygonalRpcClient(private val rpcUrls: List<String>) {
    suspend fun <T> executeWithFallback(block: suspend (Web3j) -> T): T = withContext(Dispatchers.IO) {
        var lastError: Exception? = null
        for (url in rpcUrls.shuffled()) {
            try {
                val web3j = Web3j.build(HttpService(url))
                return@withContext withTimeout(10000) { block(web3j) }
            } catch (e: Exception) {
                lastError = e
                continue
            }
        }
        throw lastError ?: RuntimeException("All RPCs failed")
    }
}
