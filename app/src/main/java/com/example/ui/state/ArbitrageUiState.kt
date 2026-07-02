package com.example.ui.state

import com.example.domain.repository.Opportunity

sealed interface ArbitrageUiState {
    object Idle : ArbitrageUiState
    object Loading : ArbitrageUiState
    data class Success(val opportunities: List<Opportunity>, val balance: String) : ArbitrageUiState
    data class Error(val message: String) : ArbitrageUiState
}
