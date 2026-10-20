package com.worksched

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat
import com.worksched.alarm.AlarmScheduler
import com.worksched.profile.ProfileChangeReceiver
import com.worksched.profile.WorkProfileUnlockNotifier

class WorkSchedApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // Android sends this action only to runtime-registered receivers.
        ContextCompat.registerReceiver(
            this, ProfileChangeReceiver(), IntentFilter(Intent.ACTION_MANAGED_PROFILE_UNLOCKED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        // Re-arm alarms from persisted store; safe to call on every cold start.
        AlarmScheduler.scheduleAllAsync(this)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notif_channel_desc)
                setShowBadge(false)
            }
            nm?.createNotificationChannel(channel)
            nm?.createNotificationChannel(NotificationChannel(
                WorkProfileUnlockNotifier.CHANNEL_ID,
                getString(R.string.unlock_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.unlock_channel_description)
                lockscreenVisibility = android.app.Notification.VISIBILITY_SECRET
            })
        }
    }

    companion object {
        const val CHANNEL_ID = "worksched_toggle"
    }
}
