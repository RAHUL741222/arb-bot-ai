package com.example.domain.usecase

import java.math.BigDecimal
import java.math.RoundingMode

class ArbitrageCalculator {
    data class ArbitrageOpportunity(
        val tokenIn: String,
        val tokenOut: String,
        val amountIn: BigDecimal,
        val expectedAmountOut: BigDecimal,
        val profit: BigDecimal,
        val profitPercentage: BigDecimal,
        val isProfitable: Boolean
    )
    
    fun calculateArbitrage(
        pool1Price: BigDecimal,
        pool2Price: BigDecimal,
        amount: BigDecimal,
        gasCost: BigDecimal,
        flashLoanFee: BigDecimal = BigDecimal("0.0009") // 0.09%
    ): ArbitrageOpportunity {
        // expectedOutput = amount * (pool2Price / pool1Price)
        val expectedOutput = amount.multiply(pool2Price).divide(pool1Price, 18, RoundingMode.HALF_UP)
        val grossProfit = expectedOutput.subtract(amount)
        
        // netProfit = grossProfit - (amount * flashLoanFee) - gasCost
        val netProfit = grossProfit
            .subtract(amount.multiply(flashLoanFee))
            .subtract(gasCost)
        
        val profitPercent = if (amount > BigDecimal.ZERO) {
            netProfit.divide(amount, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100))
        } else {
            BigDecimal.ZERO
        }
            
        return ArbitrageOpportunity(
            tokenIn = "USDT",
            tokenOut = "WMATIC",
            amountIn = amount,
            expectedAmountOut = expectedOutput,
            profit = netProfit,
            profitPercentage = profitPercent,
            isProfitable = netProfit > BigDecimal.ZERO
        )
    }
}
