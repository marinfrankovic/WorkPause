<!-- Modified for WorkPause: independent project documentation. -->
# WorkPause

[![Android 10+](https://img.shields.io/badge/Android-10%2B-3DDC84?logo=android&logoColor=white)](#requirements-and-setup)
[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white)](app/src/main/java/com/worksched)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?logo=jetpackcompose&logoColor=white)](app/src/main/java/com/worksched/ui)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Build](https://github.com/marinfrankovic/WorkPause/actions/workflows/build.yml/badge.svg)](https://github.com/marinfrankovic/WorkPause/actions/workflows/build.yml)

<img src="docs/workpause-icon.svg" width="128" height="128" alt="WorkPause icon">

**Work on your schedule.**

WorkPause pauses and resumes an existing Android work profile on selected weekdays,
with optional location-based resuming. It uses Android alarms rather than a continuous
scheduler service.

## Features

- Independent Turn ON (resume) and Turn OFF (pause) times for every weekday.
- Copy one day's hours and enabled status to any other days.
- Overnight hours, a master automation switch, and an explicit Save button.
- Simple One UI-inspired layout, with setup and optional location controls in Settings.
- Manual resume/pause control.
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

3. Open **Settings** using the gear and review setup, exact alarms, and battery restrictions.
4. Select a weekday, enable **Use day**, and choose its **Turn ON** and **Turn OFF** times.
   Repeat for other days or use **Copy to other days**, then tap **Save**.
5. Optional location controls stay under Settings; grant background location only if using them.

An OFF time earlier than ON ends the work interval the next day. Disabling a weekday
prevents new hours starting that day, but the previous day's overnight hours may continue.
Overlapping work intervals remain on until the combined interval ends. Turning off the
master switch and saving cancels automation without changing the current profile state.
Saving an enabled schedule also applies the current scheduled state.

The design is inspired by Samsung's simple settings layouts; it does not use proprietary
Samsung components and is not a claim of One UI 9 compatibility.

Accessibility is an optional, visible fallback, not a requirement for the silent backend.
If Android shows a confirmation during this fallback, confirm it and close Quick Settings
yourself; WorkPause does not automatically accept dialogs.
The app cannot bypass a device or work-profile PIN. Employer policy can restrict its use.

## Build and test

Use JDK 17 or 21 and the Android SDK. Set `JAVA_HOME` and `ANDROID_HOME`.
On Windows:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
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
- The inherited external testing hook has been removed. Internal retry broadcasts are
  non-exported, accessibility is restricted to System UI, and generic confirmation
  buttons are no longer clicked automatically.
- Location-based automatic resumes require a fresh fix with valid accuracy and
  revalidate location after unlocking. Manual resume remains an explicit override.
- This is an initial preview, not a claim of verified compatibility with Samsung or Intune.
- **Not tested on a real phone**, including the Samsung Galaxy S24 Ultra / One UI 9.
  See the [verification record](docs/VERIFICATION.md) for the checks actually performed.

## Artwork

The original WorkPause icon combines a mint briefcase and negative-space pause symbol
on midnight blue. The editable source is [docs/workpause-icon.svg](docs/workpause-icon.svg).

<img src="docs/screenshots/workpause-main.png" width="300" alt="WorkPause running in the Android 15 emulator">

## License and attribution

Licensed under [Apache License 2.0](LICENSE). Based on
[Work Profile Scheduler](https://github.com/ravindrasan1997/work-profile-scheduler),
Copyright 2026 Ravindra. Required attribution is retained in [NOTICE](NOTICE).
WorkPause is an independently maintained fork and does not imply upstream endorsement.
