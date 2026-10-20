package com.worksched.profile

enum class UnlockReminderState { NONE, WAIT_FOR_PHONE_UNLOCK, UNLOCK_REQUIRED, OPEN_WORK_APP }

object UnlockReminderPolicy {
    fun evaluate(
        enabled: Boolean,
        active: Boolean,
        profilePresent: Boolean,
        requested: Boolean,
        deviceLocked: Boolean,
        quiet: Boolean?,
        storageUnlocked: Boolean?
    ): UnlockReminderState = when {
        !enabled || !active || !profilePresent || !requested -> UnlockReminderState.NONE
        deviceLocked -> UnlockReminderState.WAIT_FOR_PHONE_UNLOCK
        quiet == true || storageUnlocked == false -> UnlockReminderState.UNLOCK_REQUIRED
        // Unlocked storage does not establish that a separate work-app challenge is unlocked.
        else -> UnlockReminderState.OPEN_WORK_APP
    }

    fun shouldRequest(quiet: Boolean?, storageUnlocked: Boolean?): Boolean =
        quiet != false || storageUnlocked != true
}
