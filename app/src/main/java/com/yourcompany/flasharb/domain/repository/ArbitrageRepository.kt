package com.yourcompany.flasharb.domain.repository

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

/**
 * @title ArbitrageRepository
 * @dev Domain-layer interface for scanning and executing arbitrage opportunities.
 */
interface ArbitrageRepository {
    /**
     * Scans for arbitrage opportunities for a specific token pair.
     * @param pair The token pair to scan (e.g., WMATIC/USDT).
     * @param loanAmount The amount to borrow for the flash loan.
     * @return A Flow emitting Loading, Success, or Error states.
     */
    suspend fun scanOpportunities(pair: TokenPair, loanAmount: BigDecimal): Flow<Resource<List<Opportunity>>>

    /**
     * Executes the arbitrage trade on-chain.
     * @param opportunity The opportunity to execute.
     * @param privateKey The user's private key (securely retrieved).
     * @param contractAddress The address of the deployed FlashLoanArbitrage contract.
     * @param loanAmount The amount to borrow.
     */
    suspend fun executeArbitrage(
        opportunity: Opportunity, 
        privateKey: String, 
        contractAddress: String,
        loanAmount: BigDecimal
    ): Flow<Resource<TransactionResult>>

    /**
     * Retrieves the history of executed transactions from the local database.
     */
    suspend fun getTransactionHistory(): Flow<List<TransactionHistory>>
}
