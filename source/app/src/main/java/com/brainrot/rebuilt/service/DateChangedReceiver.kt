package com.brainrot.rebuilt.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.brainrot.rebuilt.BrainPalApp

class DateChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val action = intent.action
        if (action == Intent.ACTION_DATE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            try {
                val app = BrainPalApp.instance
                app.prefsRepository.checkAndPerformMidnightReset()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

