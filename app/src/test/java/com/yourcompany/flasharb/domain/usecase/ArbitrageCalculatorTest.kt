package com.yourcompany.flasharb.domain.usecase

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class ArbitrageCalculatorTest {
    @Test
    fun `calculateArbitrage returns profit when price difference exists`() {
        val calculator = ArbitrageCalculator()
        val result = calculator.calculateArbitrage(
            pool1Price = BigDecimal("1.0"),
            pool2Price = BigDecimal("1.05"),
            amount = BigDecimal("1000"),
            gasCost = BigDecimal("0.5")
        )
        assertTrue(result.isProfitable)
        // Expected gross profit: 1000 * (1.05/1.0) - 1000 = 50
        // Expected fee: 1000 * 0.0009 = 0.9
        // Expected net profit: 50 - 0.9 - 0.5 = 48.6
        assertEquals(BigDecimal("48.600000000000000000"), result.profit)
    }
}
