package com.worksched.data

// Modified for WorkPause: validate daily schedules and disable automation on corrupt data.
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "worksched")

class ScheduleStore(private val context: Context) {
    private val scheduleKey = stringPreferencesKey("schedule_json")
    private val pendingResumeKey = booleanPreferencesKey("pending_resume")
    private val pendingResumeDeadlineKey = longPreferencesKey("pending_resume_deadline")
    private val geofenceArmedKey = booleanPreferencesKey("geofence_armed")
    private val json = Json { ignoreUnknownKeys = true }

    val scheduleFlow: Flow<Schedule> = context.dataStore.data.map { prefs ->
        prefs[scheduleKey]?.let { raw ->
            try {
                json.decodeFromString<Schedule>(raw).also {
                    require(it.validationError() == null) { requireNotNull(it.validationError()) }
                }
            } catch (e: IllegalArgumentException) {
                Log.e("WorkPauseStore", "Invalid saved schedule; automation disabled", e)
                Schedule.default().copy(enabled = false)
            }
        } ?: Schedule.default()
    }

    suspend fun save(schedule: Schedule) {
        require(schedule.validationError() == null) { requireNotNull(schedule.validationError()) }
        context.dataStore.edit { it[scheduleKey] = json.encodeToString(schedule) }
    }

    // ---- pending-resume state (deferred resume while device locked) ----

    val pendingResumeFlow: Flow<Boolean> = context.dataStore.data.map { it[pendingResumeKey] ?: false }

    suspend fun pendingResume(): Boolean =
        context.dataStore.data.first()[pendingResumeKey] ?: false

    suspend fun pendingResumeDeadline(): Long =
        context.dataStore.data.first()[pendingResumeDeadlineKey] ?: 0L

    suspend fun setPendingResume(active: Boolean, deadlineMs: Long) {
        context.dataStore.edit {
            it[pendingResumeKey] = active
            it[pendingResumeDeadlineKey] = if (active) deadlineMs else 0L
        }
    }

    // ---- geofence-armed state (an arrival-resume proximity alert is outstanding) ----

    suspend fun geofenceArmed(): Boolean =
        context.dataStore.data.first()[geofenceArmedKey] ?: false

    suspend fun setGeofenceArmed(armed: Boolean) {
        context.dataStore.edit { it[geofenceArmedKey] = armed }
    }
}
