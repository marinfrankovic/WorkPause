package com.worksched.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnlockReminderPolicyTest {
    private fun state(
        enabled: Boolean = true,
        active: Boolean = true,
        profilePresent: Boolean = true,
        requested: Boolean = true,
        deviceLocked: Boolean = false,
        quiet: Boolean? = false,
        storageUnlocked: Boolean? = false
    ) = UnlockReminderPolicy.evaluate(enabled, active, profilePresent, requested, deviceLocked, quiet, storageUnlocked)

    @Test fun `off always removes a reminder`() {
        assertEquals(UnlockReminderState.NONE, state(enabled = false))
    }

    @Test fun `end of scheduled hours removes a reminder`() {
        assertEquals(UnlockReminderState.NONE, state(active = false))
    }

    @Test fun `removed profile removes a reminder`() {
        assertEquals(UnlockReminderState.NONE, state(profilePresent = false))
    }

    @Test fun `no reminder without an actual resume request`() {
        assertEquals(UnlockReminderState.NONE, state(requested = false))
    }

    @Test fun `phone locked defers the visible notification`() {
        assertEquals(UnlockReminderState.WAIT_FOR_PHONE_UNLOCK, state(deviceLocked = true))
    }

    @Test fun `resumed but storage locked needs user action`() {
        assertEquals(UnlockReminderState.UNLOCK_REQUIRED, state(quiet = false, storageUnlocked = false))
    }

    @Test fun `quiet profile still needs resume even with unlocked storage`() {
        assertEquals(UnlockReminderState.UNLOCK_REQUIRED, state(quiet = true, storageUnlocked = true))
    }

    @Test fun `unlocked storage does not claim the work challenge is unlocked`() {
        assertEquals(UnlockReminderState.OPEN_WORK_APP, state(storageUnlocked = true))
    }

    @Test fun `unknown status uses conditional language`() {
        assertEquals(UnlockReminderState.OPEN_WORK_APP, state(quiet = null, storageUnlocked = null))
    }

    @Test fun `already running unlocked profile does not create a new reminder`() {
        assertFalse(UnlockReminderPolicy.shouldRequest(quiet = false, storageUnlocked = true))
    }

    @Test fun `paused or credential locked profile records a reminder`() {
        assertTrue(UnlockReminderPolicy.shouldRequest(quiet = true, storageUnlocked = true))
        assertTrue(UnlockReminderPolicy.shouldRequest(quiet = false, storageUnlocked = false))
        assertTrue(UnlockReminderPolicy.shouldRequest(quiet = null, storageUnlocked = null))
    }

    @Test fun `off takes priority over phone lock`() {
        assertEquals(UnlockReminderState.NONE, state(enabled = false, deviceLocked = true))
    }
}
