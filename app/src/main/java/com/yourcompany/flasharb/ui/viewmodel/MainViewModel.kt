package com.yourcompany.flasharb.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourcompany.flasharb.api.GeminiClient
import com.yourcompany.flasharb.domain.repository.ArbitrageRepository
import com.yourcompany.flasharb.domain.repository.Resource
import com.yourcompany.flasharb.domain.repository.TokenPair
import com.yourcompany.flasharb.domain.usecase.ArbitrageCalculator
import com.yourcompany.flasharb.ui.state.ArbitrageUiState
import com.yourcompany.flasharb.data.pref.SecurePreferenceManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal

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

/**
 * @title MainViewModel
 * @dev Presentation-layer ViewModel for managing UI state and business logic coordination.
 */
class MainViewModel(
    private val repository: ArbitrageRepository,
    private val securePrefs: SecurePreferenceManager
) : ViewModel() {
    private val calculator = ArbitrageCalculator()

    private val _uiState = MutableStateFlow<ArbitrageUiState>(ArbitrageUiState.Idle)
    val uiState: StateFlow<ArbitrageUiState> = _uiState.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                isUser = false,
                text = "স্বাগতম! আমি আপনার DeFi এবং ফ্ল্যাশ লোন উপদেষ্টা।"
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

    // Real settings from UI, loaded from secure prefs
    val walletAddress = MutableStateFlow(securePrefs.getWalletAddress() ?: "")
    val contractAddress = MutableStateFlow(securePrefs.getContractAddress() ?: "0x5E4943373c2198625BD441Ae0629E9E7b4FB4797")
    val privateKey = MutableStateFlow(securePrefs.getPrivateKey() ?: "")

    init {
        calculateArbitrage()
    }

    fun onChatInputChange(text: String) {
        _chatInput.value = text
    }

    fun updateCalcInputs(transform: (CalculatorInputs) -> CalculatorInputs) {
        _calcInputs.update(transform)
        calculateArbitrage()
    }

    fun saveSettings(address: String, contract: String, key: String) {
        walletAddress.value = address
        contractAddress.value = contract
        privateKey.value = key
        
        securePrefs.saveWalletAddress(address)
        securePrefs.saveContractAddress(contract)
        securePrefs.savePrivateKey(key)
    }

    fun startAutoScan(pair: TokenPair) {
        val loan = _calcInputs.value.loanAmount.toBigDecimalOrNull() ?: BigDecimal("10000")
        viewModelScope.launch {
            repository.scanOpportunities(pair, loan).collect { resource ->
                when(resource) {
                    is Resource.Success -> {
                        _uiState.value = ArbitrageUiState.Success(resource.data, "0.0")
                    }
                    is Resource.Error -> {
                        _uiState.value = ArbitrageUiState.Error(resource.message)
                    }
                    is Resource.Loading -> {
                        _uiState.value = ArbitrageUiState.Loading
                    }
                }
            }
        }
    }

    fun executeOpportunity(opportunity: com.yourcompany.flasharb.domain.repository.Opportunity) {
        val loan = _calcInputs.value.loanAmount.toBigDecimalOrNull() ?: BigDecimal("10000")
        viewModelScope.launch {
            _uiState.value = ArbitrageUiState.Loading
            repository.executeArbitrage(
                opportunity = opportunity,
                privateKey = privateKey.value,
                contractAddress = contractAddress.value,
                loanAmount = loan
            ).collect { resource ->
                if (resource is Resource.Success) {
                    _uiState.value = ArbitrageUiState.Idle
                } else if (resource is Resource.Error) {
                    _uiState.value = ArbitrageUiState.Error(resource.message)
                }
            }
        }
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

        _calcResult.value = CalculatorResult(
            grossProfit = opportunity.expectedAmountOut.subtract(loan).toDouble(),
            loanFee = loan.multiply(BigDecimal("0.0009")).toDouble(),
            gasCost = gasCostUSD.toDouble(),
            netProfit = opportunity.profit.toDouble(),
            isProfitable = opportunity.isProfitable,
            warningMessage = if (!opportunity.isProfitable) "আর্বিট্রেজ লোকসানে রয়েছে।" else null
        )
    }

    fun sendChatMessage(text: String) {
        // AI query logic...
    }
}
