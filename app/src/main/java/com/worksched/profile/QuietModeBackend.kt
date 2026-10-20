package com.worksched.profile

// Modified for WorkPause: setup identity and SDK-defined quiet-mode flag.
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserManager
import android.util.Log

/**
 * Silent backend. Requires the MODIFY_QUIET_MODE permission, granted once via:
 *
 *   adb shell pm grant io.github.marinfrankovic.workpause android.permission.MODIFY_QUIET_MODE
 *
 * The permission's protectionLevel is `signature|privileged|development|role`;
 * the `development` flag is what makes `pm grant` work for a normal app (same
 * mechanism as WRITE_SECURE_SETTINGS). Once held, [UserManager.requestQuietModeEnabled]
 * pauses/resumes the work profile in ~50 ms with no UI, no screen wake, and
 * without touching profile ownership (Intune enrolment stays intact).
 *
 * On Android 11+, scheduled resumes use the credential-not-required flag whether
 * the phone is locked or unlocked. If credentials are needed, the caller retries
 * silently and offers a notification. Only an explicit action in the visible
 * unlock picker uses the prompting overload.
 */
object QuietModeBackend {
    private const val TAG = "QuietModeBackend"
    const val PERMISSION = "android.permission.MODIFY_QUIET_MODE"

    fun isAvailable(context: Context): Boolean =
        context.checkSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED

    fun isDeviceLocked(context: Context): Boolean {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return km?.isDeviceLocked == true
    }

    /**
     * enable=true  → resume the profile (quietMode=false)
     * enable=false → pause  the profile (quietMode=true)
     *
     * Returns true if the profile reached the desired state. For resume this is
     * the OS's real result: false means "deferred — credential needed (locked)".
     * Pause always returns true (it never needs a credential).
     */
    fun toggle(context: Context, enable: Boolean): Boolean {
        val um = context.getSystemService(Context.USER_SERVICE) as UserManager
        val workUser = WorkProfileDetector(context).workProfileHandle()
        if (workUser == null) {
            Log.w(TAG, "no work profile present")
            return false
        }
        return try {
            if (enable) {
                val locked = isDeviceLocked(context)
                // The API also returns false for an already-running profile, not just a credential challenge.
                val ok = !um.isQuietModeEnabled(workUser) || if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    um.requestQuietModeEnabled(false, workUser, UserManager.QUIET_MODE_DISABLE_ONLY_IF_CREDENTIAL_NOT_REQUIRED)
                } else {
                    um.requestQuietModeEnabled(false, workUser)
                }
                Log.i(TAG, "resume: profile ON request satisfied=$ok (deviceLocked=$locked)")
                ok
            } else {
                // Pause = enable quiet mode. Always succeeds, no credential.
                um.requestQuietModeEnabled(true, workUser)
                Log.i(TAG, "pause: requestQuietModeEnabled(quiet=true) issued")
                true
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "MODIFY_QUIET_MODE not held: $e")
            false
        } catch (t: Throwable) {
            Log.w(TAG, "toggle failed: $t")
            false
        }
    }

    fun resumeFromUserAction(context: Context): Boolean {
        val workUser = WorkProfileDetector(context).workProfileHandle() ?: run {
            Log.w(TAG, "Cannot request work credentials: no work profile")
            return false
        }
        return try {
            context.getSystemService(UserManager::class.java).requestQuietModeEnabled(false, workUser)
        } catch (e: SecurityException) {
            Log.w(TAG, "Android denied the user-initiated work-profile resume", e)
            false
        }
    }
}
