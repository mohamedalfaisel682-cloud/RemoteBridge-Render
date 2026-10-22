package com.remotebridge

import android.app.Application
import com.remotebridge.core.data.BridgeDatabase
import androidx.room.Room

/**
 * فئة التطبيق. تُهيّئ قاعدة البيانات المشتركة.
 */
class BridgeApplication : Application() {

    val database: BridgeDatabase by lazy {
        Room.databaseBuilder(
            this,
            BridgeDatabase::class.java,
            "bridge.db"
        ).fallbackToDestructiveMigration().build()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: BridgeApplication
            private set
    }
}
