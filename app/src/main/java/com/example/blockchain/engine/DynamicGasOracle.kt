package com.example.blockchain.engine

import java.math.BigInteger

class DynamicGasOracle(private val rpcClient: PolygonalRpcClient) {
    suspend fun getOptimizedGasPrice(): BigInteger {
        return rpcClient.executeWithFallback { web3j ->
            val ethGasPrice = web3j.ethGasPrice().send().gasPrice
            // Adding 20% to ensure faster confirmation in arbitrage
            ethGasPrice.multiply(BigInteger.valueOf(120)).divide(BigInteger.valueOf(100))
        }
    }
}
