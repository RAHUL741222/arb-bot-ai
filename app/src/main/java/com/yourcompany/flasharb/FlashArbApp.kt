package com.yourcompany.flasharb

import android.app.Application
import androidx.room.Room
import com.yourcompany.flasharb.blockchain.BlockchainManager
import com.yourcompany.flasharb.data.local.AppDatabase
import com.yourcompany.flasharb.data.pref.SecurePreferenceManager
import com.yourcompany.flasharb.data.repository.ArbitrageRepositoryImpl
import com.yourcompany.flasharb.domain.repository.ArbitrageRepository
import timber.log.Timber

/**
 * @title FlashArbApp
 * @dev Main Application class for initializing global libraries and dependencies.
 */
class FlashArbApp : Application() {
    
    lateinit var repository: ArbitrageRepository
    lateinit var securePrefs: SecurePreferenceManager

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        securePrefs = SecurePreferenceManager(applicationContext)
        
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "flasharb-db"
        ).build()
        
        repository = ArbitrageRepositoryImpl(
            BlockchainManager(listOf("https://polygon-rpc.com")),
            db.transactionDao()
        )
    }
}
