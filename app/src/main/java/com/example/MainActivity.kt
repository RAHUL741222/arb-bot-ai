package com.yourcompany.flasharb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.yourcompany.flasharb.ui.theme.MyApplicationTheme

import androidx.activity.viewModels
import com.yourcompany.flasharb.ui.screens.MainAppScreen
import com.yourcompany.flasharb.ui.viewmodel.MainViewModel

import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.yourcompany.flasharb.worker.ArbitrageScanWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
  private val mainViewModel: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Start Background Scanner
    val scanRequest = PeriodicWorkRequestBuilder<ArbitrageScanWorker>(15, TimeUnit.MINUTES).build()
    WorkManager.getInstance(applicationContext).enqueue(scanRequest)

    setContent {
      MyApplicationTheme {
        MainAppScreen(viewModel = mainViewModel)
      }
    }
  }
}
