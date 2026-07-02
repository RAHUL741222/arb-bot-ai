package com.yourcompany.flasharb.data.pref

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecurePreferenceManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun savePrivateKey(key: String) {
        sharedPreferences.edit().putString("private_key", key).apply()
    }

    fun getPrivateKey(): String? {
        return sharedPreferences.getString("private_key", null)
    }

    fun saveWalletAddress(address: String) {
        sharedPreferences.edit().putString("wallet_address", address).apply()
    }

    fun getWalletAddress(): String? {
        return sharedPreferences.getString("wallet_address", null)
    }

    fun saveContractAddress(address: String) {
        sharedPreferences.edit().putString("contract_address", address).apply()
    }

    fun getContractAddress(): String? {
        return sharedPreferences.getString("contract_address", null)
    }
}
