package com.example.domain.repository

import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

sealed class Resource<out T> {
    object Loading : Resource<Nothing>()
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
}

data class TokenPair(val token0: String, val token1: String, val symbol: String)
data class Opportunity(val pair: TokenPair, val profit: BigDecimal, val dexA: String, val dexB: String)
data class TransactionResult(val txHash: String, val status: String)
data class TransactionHistory(val txHash: String, val profit: String, val timestamp: Long, val status: String)

interface ArbitrageRepository {
    suspend fun scanOpportunities(pair: TokenPair): Flow<Resource<List<Opportunity>>>
    suspend fun executeArbitrage(opportunity: Opportunity): Flow<Resource<TransactionResult>>
    suspend fun getTransactionHistory(): Flow<List<TransactionHistory>>
}
