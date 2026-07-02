package com.yourcompany.flasharb.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yourcompany.flasharb.domain.repository.ArbitrageRepository
import com.yourcompany.flasharb.domain.repository.TokenPair
import com.yourcompany.flasharb.domain.repository.Resource
import com.yourcompany.flasharb.domain.repository.Opportunity
import kotlinx.coroutines.flow.first

class ArbitrageScanWorker(
    context: Context,
    params: WorkerParameters,
    private val repository: ArbitrageRepository
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val pair = TokenPair("0x0d500B1d8E8eF31E21C99d1Db9A6444d3ADf1270", "0xc2132d05d31c914a87c6611c10748aeb04b58e8f", "WMATIC/USDT")
            val resource = repository.scanOpportunities(pair, java.math.BigDecimal("10000")).first { it !is Resource.Loading }
            
            if (resource is Resource.Success && resource.data.isNotEmpty()) {
                sendArbitrageNotification(resource.data.first())
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun sendArbitrageNotification(opportunity: Opportunity) {
        val channelId = "arbitrage_scan"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Arbitrage Alerts", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Arbitrage Opportunity Found!")
            .setContentText("Profit: $${opportunity.profit} on ${opportunity.pair.symbol}")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
            
        notificationManager.notify(1, notification)
    }
}
