<!-- Modified for WorkPause: independent project documentation. -->
# WorkPause

<img src="docs/workpause-icon.svg" width="128" height="128" alt="WorkPause icon">

**Work on your schedule.**

WorkPause pauses and resumes an existing Android work profile on selected weekdays,
with optional location-based resuming. It uses Android alarms rather than a continuous
scheduler service.

## Features

- Scheduled resume and pause times, with weekday selection.
- Manual resume and pause controls.
- Silent control after a one-time ADB permission grant.
- Optional resume-at-a-place location check.
- Deferred resume when Android requires the device or work profile to be unlocked.
- Material 3 interface and adaptive, round, and themed launcher icons.

## App identity

- Application ID: `io.github.marinfrankovic.workpause`
- Initial version: `1.0.0` (version code `1`)
- Repository: [marinfrankovic/WorkPause](https://github.com/marinfrankovic/WorkPause)

WorkPause installs separately from the original application. Settings and permission
grants do not migrate automatically.

## Requirements and setup

Android 10 or newer and an existing Android work profile are required. Samsung Secure
Folder is not a substitute for a work profile.

1. Install a WorkPause APK on the personal side of the device.
2. For silent control, grant the permission from an authorized ADB host:

   ```text
   adb shell pm grant io.github.marinfrankovic.workpause android.permission.MODIFY_QUIET_MODE
   ```

3. Open **Setup & permissions** and review exact alarms and battery restrictions.
4. Select weekdays and times, then save the schedule.
5. Enable background location only if using **Resume at a place**.

Accessibility is an optional, visible fallback, not a requirement for the silent backend.
The app cannot bypass a device or work-profile PIN. Employer policy can restrict its use.

## Build and test

Use JDK 17 or 21 and the Android SDK. Set `JAVA_HOME` and `ANDROID_HOME`.
On Windows:

```powershell
.\gradlew.bat :app:assembleDebug :app:lintDebug
adb -s emulator-5556 install -r .\app\build\outputs\apk\debug\app-debug.apk
```

The debug APK is for development only. Debug and release builds share the application
ID but use different signing keys; Android will not install one over the other.
Uninstalling clears that installation's settings and permission grants.

## Release signing

Release builds require WorkPause's private key and never fall back to debug signing.
Windows helpers store the PKCS12 keystore outside the repository, under
`%LOCALAPPDATA%\WorkPause\signing`, with access restricted to the current Windows account.
The randomly generated password is encrypted with Windows DPAPI.

Initialize once, then build:

```powershell
.\tools\Initialize-ReleaseSigning.ps1
.\tools\Build-Release.ps1
```

The signed APK is written to `app\build\outputs\apk\release\app-release.apk`.
For other environments, provide `WORKPAUSE_KEYSTORE_PATH`, `WORKPAUSE_STORE_PASSWORD`,
`WORKPAUSE_KEY_ALIAS`, and `WORKPAUSE_KEY_PASSWORD` securely before running
`:app:assembleRelease`. Never put passwords in command-line arguments, tracked files,
or logs. CI builds debug artifacts only and does not receive the release key.

**Backup is essential:** losing the private key or its password can prevent future
updates. DPAPI credentials are tied to this Windows user and machine; copying the
encrypted password file alone is not a portable recovery backup. Arrange a protected
off-device backup of the keystore and a separately secured password before distribution.

Increase `versionCode` for every published update. Version names and release tags are
managed independently from upstream.

## Status and limitations

- Emulator installation and appearance do not establish Samsung or enterprise compatibility.
- Location gating is a convenience, not a security enforcement boundary.
- The inherited external `TEST_FIRE` hook remains a known security issue and must be
  addressed before public distribution or privileged use on a real work phone.
- No public release is published as part of the initial setup.

## Artwork

The original WorkPause icon combines a mint briefcase and negative-space pause symbol
on midnight blue. The editable source is [docs/workpause-icon.svg](docs/workpause-icon.svg).

## License and attribution

Licensed under [Apache License 2.0](LICENSE). Based on
[Work Profile Scheduler](https://github.com/ravindrasan1997/work-profile-scheduler),
Copyright 2026 Ravindra. Required attribution is retained in [NOTICE](NOTICE).
WorkPause is an independently maintained fork and does not imply upstream endorsement.
