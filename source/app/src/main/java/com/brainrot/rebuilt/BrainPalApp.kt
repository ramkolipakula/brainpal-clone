package com.brainrot.rebuilt

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.brainrot.rebuilt.data.PreferencesRepository
import com.brainrot.rebuilt.data.RoastMemeRepository
import com.brainrot.rebuilt.data.UsageStatsRepository

class BrainPalApp : Application() {

    lateinit var prefsRepository: PreferencesRepository
        private set

    lateinit var memeRepository: RoastMemeRepository
        private set

    lateinit var usageRepository: UsageStatsRepository
        private set

    companion object {
        const val CHANNEL_ID = "brainpal_monitor_channel"
        lateinit var instance: BrainPalApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        prefsRepository = PreferencesRepository(this)
        memeRepository = RoastMemeRepository(this)
        usageRepository = UsageStatsRepository(this)

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_desc)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
}

