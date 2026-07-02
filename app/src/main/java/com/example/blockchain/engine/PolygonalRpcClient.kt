package com.yourcompany.flasharb.blockchain.engine

import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit

class PolygonalRpcClient(private val rpcUrls: List<String>) {
    suspend fun <T> executeWithFallback(block: suspend (Web3j) -> T): T {
        var lastError: Exception? = null
        for (url in rpcUrls.shuffled()) {
            try {
                val web3j = Web3j.build(HttpService(url))
                return withTimeout(10000) { block(web3j) }
            } catch (e: Exception) {
                lastError = e
                continue
            }
        }
        throw lastError ?: RuntimeException("All RPCs failed")
    }
}
