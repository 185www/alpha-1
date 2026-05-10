package com.focusintent.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BroadcastReceiver to restart the overlay service on device boot.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Optionally restart service if there was an active session
            // For V1, we don't auto-restart to respect user's explicit intent
        }
    }
}
