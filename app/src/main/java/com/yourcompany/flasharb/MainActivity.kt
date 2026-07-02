package com.yourcompany.flasharb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.yourcompany.flasharb.ui.theme.MyApplicationTheme
import com.yourcompany.flasharb.ui.screens.MainAppScreen
import com.yourcompany.flasharb.ui.viewmodel.MainViewModel
import com.yourcompany.flasharb.worker.ArbitrageScanWorker
import java.util.concurrent.TimeUnit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.yourcompany.flasharb.data.repository.ArbitrageRepositoryImpl
import com.yourcompany.flasharb.blockchain.BlockchainManager
import com.yourcompany.flasharb.data.local.AppDatabase
import androidx.room.Room

import com.yourcompany.flasharb.data.pref.SecurePreferenceManager

import androidx.work.ExistingPeriodicWorkPolicy

class MainActivity : ComponentActivity() {
    
    private val mainViewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = application as FlashArbApp
                return MainViewModel(app.repository, app.securePrefs) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Start Background Scanner with Unique Work Policy
        val scanRequest = PeriodicWorkRequestBuilder<ArbitrageScanWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "arbitrage_scan",
            ExistingPeriodicWorkPolicy.KEEP,
            scanRequest
        )

        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = mainViewModel)
            }
        }
    }
}
