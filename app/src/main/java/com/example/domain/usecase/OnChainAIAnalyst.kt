package com.example.domain.usecase

import com.example.api.GeminiClient
import com.example.domain.repository.TokenPair
import org.json.JSONObject

class OnChainAIAnalyst(private val geminiClient: GeminiClient) {
    suspend fun getTradeAdvice(tokenPair: TokenPair, marketData: String): String {
        val prompt = """
            You are a DeFi arbitrage expert. Analyze this market data for ${tokenPair.symbol}:
            $marketData
            Provide a technical analysis and suggest if a flash loan trade is viable.
            Return a brief summary.
        """.trimIndent()
        
        return geminiClient.queryGemini(prompt).text
    }
}
