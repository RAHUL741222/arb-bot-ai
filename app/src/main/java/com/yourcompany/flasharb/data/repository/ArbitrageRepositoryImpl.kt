package com.yourcompany.flasharb.data.repository

import com.yourcompany.flasharb.blockchain.BlockchainManager
import com.yourcompany.flasharb.data.local.TransactionDao
import com.yourcompany.flasharb.data.local.TransactionEntity
import com.yourcompany.flasharb.domain.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import java.math.BigDecimal
import java.math.BigInteger

class ArbitrageRepositoryImpl(
    private val blockchainManager: BlockchainManager,
    private val transactionDao: TransactionDao
) : ArbitrageRepository {
    
    override suspend fun scanOpportunities(pair: TokenPair, loanAmount: BigDecimal): Flow<Resource<List<Opportunity>>> = flow {
        emit(Resource.Loading)
        try {
            val uniPrice = BigDecimal.valueOf(blockchainManager.getUniswapV3Price(pair.token0, pair.token1))
            val quickPrice = BigDecimal.valueOf(blockchainManager.getQuickSwapPrice(pair.token0, pair.token1))
            
            if (uniPrice <= BigDecimal.ZERO || quickPrice <= BigDecimal.ZERO) {
                emit(Resource.Success(emptyList()))
                return@flow
            }

            val profit = quickPrice.subtract(uniPrice).multiply(loanAmount)
            
            if (profit > BigDecimal.TEN) {
                emit(Resource.Success(listOf(Opportunity(pair, profit, "Uniswap V3", "Quickswap"))))
            } else {
                emit(Resource.Success(emptyList()))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Scan failed"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun executeArbitrage(
        opportunity: Opportunity,
        privateKey: String,
        contractAddress: String,
        loanAmount: BigDecimal
    ): Flow<Resource<TransactionResult>> = flow {
        emit(Resource.Loading)
        try {
            // 1. Submit Transaction
            val decimals = if (opportunity.pair.token0.lowercase().contains("c2132d")) 6 else 18
            val amountWei = loanAmount.multiply(BigDecimal.TEN.pow(decimals)).toBigInteger()
            
            val txHash = blockchainManager.executeFlashLoan(
                privateKey = privateKey,
                contractAddress = contractAddress,
                tokenAddress = opportunity.pair.token0,
                amount = amountWei,
                tokenToBuy = opportunity.pair.token1,
                minProfit = opportunity.profit.toBigInteger()
            )
            
            if (txHash.startsWith("0x")) {
                // 2. Monitor Transaction
                val receipt = blockchainManager.txMonitor.waitForMining(txHash)
                val status = if (receipt.isStatusOK) "SUCCESS" else "FAILED"
                
                // 3. Save to local DB
                transactionDao.insert(TransactionEntity(
                    txHash = txHash,
                    amountIn = loanAmount.toString(),
                    amountOut = "", // Could be updated if we parse logs
                    profit = opportunity.profit.toString(),
                    timestamp = System.currentTimeMillis(),
                    status = status
                ))
                
                emit(Resource.Success(TransactionResult(txHash, status)))
            } else {
                emit(Resource.Error(txHash))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Execution failed"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getTransactionHistory(): Flow<List<TransactionHistory>> {
        return transactionDao.getAll().map { entities ->
            entities.map { 
                TransactionHistory(it.txHash, it.profit, it.timestamp, it.status)
            }
        }
    }
}
