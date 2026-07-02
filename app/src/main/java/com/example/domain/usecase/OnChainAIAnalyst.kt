package com.yourcompany.flasharb.domain.usecase

import com.yourcompany.flasharb.api.GeminiClient
import com.yourcompany.flasharb.domain.repository.TokenPair
import org.json.JSONObject

data class AIAnalysisResult(
    val confidence: Int,
    val suggestedAmount: String,
    val riskLevel: String,
    val summary: String
)

class OnChainAIAnalyst(private val geminiClient: GeminiClient) {
    suspend fun getTradeAdvice(tokenPair: TokenPair, marketData: String): AIAnalysisResult {
        val prompt = """
            You are a DeFi arbitrage expert. Analyze this market data for ${tokenPair.symbol}:
            $marketData
            Return ONLY a JSON object with: { "confidence": 0-100, "suggested_amount": "USD", "risk_level": "low/medium/high", "summary": "brief text" }
        """.trimIndent()
        
        val response = geminiClient.queryGemini(prompt).text
        return try {
            val json = JSONObject(response)
            AIAnalysisResult(
                confidence = json.optInt("confidence", 0),
                suggestedAmount = json.optString("suggested_amount", "0"),
                riskLevel = json.optString("risk_level", "unknown"),
                summary = json.optString("summary", "Analysis failed")
            )
        } catch (e: Exception) {
            AIAnalysisResult(0, "0", "error", "Response parsing failed: $response")
        }
    }
}
