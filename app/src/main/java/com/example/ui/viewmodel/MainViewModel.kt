package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiClient
import com.example.simulator.ArbitrageSimulator
import com.example.simulator.SimState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.example.domain.usecase.ArbitrageCalculator
import java.math.BigDecimal
import java.math.RoundingMode

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val searchQueries: List<String> = emptyList(),
    val searchSources: List<GeminiClient.SearchSource> = emptyList()
)

data class CalculatorInputs(
    val loanAmount: String = "10000",
    val buyPrice: String = "1.00",
    val sellPrice: String = "1.01",
    val gasPriceGwei: String = "55",
    val gasLimit: String = "250000",
    val network: String = "Polygon (MATIC)"
)

data class CalculatorResult(
    val grossProfit: Double = 0.0,
    val loanFee: Double = 0.0,
    val gasCost: Double = 0.0,
    val netProfit: Double = 0.0,
    val isProfitable: Boolean = false,
    val warningMessage: String? = null
)

class MainViewModel : ViewModel() {
    val simulator = ArbitrageSimulator()
    val simState: StateFlow<SimState> = simulator.state
    val simLogs = simulator.logs
    private val calculator = ArbitrageCalculator()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                isUser = false,
                text = "স্বাগতম! আমি আপনার DeFi এবং ফ্ল্যাশ লোন উপদেষ্টা। গুগল সার্চ গ্রাউন্ডিং যুক্ত হওয়ায় আমি রিয়েল-টাইম মার্কেট ফি, গ্যাস প্রাইস এবং আর্বিট্রেজ ট্রেন্ড নিয়ে উত্তর দিতে পারি।\n\nনিচের কোন একটি বাটনে ক্লিক করুন অথবা নিজে প্রশ্ন লিখুন। সোলিডিটি স্মার্ট কন্ট্রাক্ট জেনারেট করতে বললেও আমি স্ক্রিপ্ট তৈরি করে দেব!"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _chatInput = MutableStateFlow("")
    val chatInput: StateFlow<String> = _chatInput.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    private val _calcInputs = MutableStateFlow(CalculatorInputs())
    val calcInputs: StateFlow<CalculatorInputs> = _calcInputs.asStateFlow()

    private val _calcResult = MutableStateFlow(CalculatorResult())
    val calcResult: StateFlow<CalculatorResult> = _calcResult.asStateFlow()

    init {
        // Initial calculator run
        calculateArbitrage()
    }

    fun onChatInputChange(text: String) {
        _chatInput.value = text
    }

    fun updateCalcInputs(transform: (CalculatorInputs) -> CalculatorInputs) {
        _calcInputs.update(transform)
        calculateArbitrage()
    }

    fun sendChatMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        // Clear input
        _chatInput.value = ""

        // Add user message
        val userMsg = ChatMessage(isUser = true, text = trimmed)
        _chatMessages.update { it + userMsg }

        _isChatLoading.value = true

        viewModelScope.launch {
            try {
                val systemInstruction = """
                    You are FlashArb AI Advisor, an expert DeFi smart contract developer and arbitrage specialist.
                    You assist users with understanding and writing Flash Loan smart contracts (Aave, Uniswap), gas optimization, DEX price feeds, MEV protection, and deployment guides in Remix.
                    The user may speak Bengali (বাংলা) or English. Please reply in the language the user is using (preferably Bengali, since the app target audience is Bengali, but fallback or mix naturally if appropriate).
                    When requested to write Solidity smart contracts, provide high-quality, secure, and complete Solidity code inside standard code blocks with precise explanations of each function (borrow, execute, repay).
                    Be highly realistic: explain that while flash loan arbitrage is technically possible, a $2 budget on Polygon or BSC will only cover a few gas-failed attempts, and explain how MEV bots frontrun transactions in the public mempool.
                """.trimIndent()

                val response = GeminiClient.queryGemini(trimmed, systemInstruction)
                
                val assistantMsg = ChatMessage(
                    isUser = false,
                    text = response.text,
                    searchQueries = response.searchQueries,
                    searchSources = response.searchSources
                )
                _chatMessages.update { it + assistantMsg }
            } catch (e: Exception) {
                val errorMsg = ChatMessage(
                    isUser = false,
                    text = "দুঃখিত, উত্তর তৈরি করতে সমস্যা হয়েছে: ${e.localizedMessage}"
                )
                _chatMessages.update { it + errorMsg }
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    fun updateRpcUrl(url: String) {
        simulator.setRpcUrl(url)
    }

    fun updateWalletAddress(address: String) {
        simulator.updateWalletAddress(address)
    }

    fun updateContractAddress(address: String) {
        simulator.updateContractAddress(address)
    }

    fun updatePrivateKey(key: String) {
        simulator.updatePrivateKey(key)
    }

    fun toggleAutoTrade(enabled: Boolean) {
        simulator.toggleAutoTrade(enabled)
    }

    private fun calculateArbitrage() {
        val inputs = _calcInputs.value
        val loan = inputs.loanAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val buy = inputs.buyPrice.toBigDecimalOrNull() ?: BigDecimal.ONE
        val sell = inputs.sellPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val gasGwei = inputs.gasPriceGwei.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val gasLimit = inputs.gasLimit.toBigDecimalOrNull() ?: BigDecimal.ZERO
        
        if (loan <= BigDecimal.ZERO || buy <= BigDecimal.ZERO) {
            _calcResult.value = CalculatorResult(warningMessage = "অনুগ্রহ করে সঠিক সংখ্যা ইনপুট দিন।")
            return
        }

        // Gas Cost calculation
        val isPolygon = inputs.network.contains("Polygon")
        val nativePrice = if (isPolygon) BigDecimal("0.58") else BigDecimal("580.0")
        val gasCostNative = gasLimit.multiply(gasGwei).multiply(BigDecimal("1e-9"))
        val gasCostUSD = gasCostNative.multiply(nativePrice)

        val opportunity = calculator.calculateArbitrage(
            pool1Price = buy,
            pool2Price = sell,
            amount = loan,
            gasCost = gasCostUSD
        )

        // Warning generation
        val warning = when {
            !opportunity.isProfitable && opportunity.profit > BigDecimal.ZERO -> "আর্বিট্রেজ লাভজনক নয়! গ্যাস ও লোন ফি লাভের চেয়ে বেশি।"
            !opportunity.isProfitable -> "ট্রেড লোকসানে রয়েছে!"
            opportunity.isProfitable && loan >= BigDecimal("50000") && gasGwei < BigDecimal("120") -> "ঝুঁকি সতর্কবার্তা: মেমপুলে MEV ফ্রন্টরান হওয়ার সম্ভাবনা অনেক বেশি!"
            else -> null
        }

        _calcResult.value = CalculatorResult(
            grossProfit = opportunity.expectedAmountOut.subtract(loan).toDouble(),
            loanFee = loan.multiply(BigDecimal("0.0009")).toDouble(),
            gasCost = gasCostUSD.toDouble(),
            netProfit = opportunity.profit.toDouble(),
            isProfitable = opportunity.isProfitable,
            warningMessage = warning
        )
    }

    override fun onCleared() {
        super.onCleared()
        simulator.stop()
    }
}
