package com.worksched.profile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.worksched.alarm.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProfileChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(
                Intent.ACTION_MANAGED_PROFILE_ADDED, Intent.ACTION_MANAGED_PROFILE_REMOVED,
                Intent.ACTION_MANAGED_PROFILE_AVAILABLE, Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE,
                Intent.ACTION_MANAGED_PROFILE_UNLOCKED
            )) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                if (intent.action == Intent.ACTION_MANAGED_PROFILE_UNLOCKED ||
                    intent.action == Intent.ACTION_MANAGED_PROFILE_REMOVED) {
                    WorkProfileUnlockNotifier.cancel(context.applicationContext)
                } else {
                    WorkProfileUnlockNotifier.refresh(context.applicationContext)
                }
            } finally {
                pending.finish()
            }
        }
        // A new work profile may have appeared or been removed.
        // Re-arm alarms so they pick up the (possibly new) profile handle on fire.
        AlarmScheduler.scheduleAllAsync(context.applicationContext)
    }
}
