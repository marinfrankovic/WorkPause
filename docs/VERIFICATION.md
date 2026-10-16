<!-- Modified for WorkPause: replace inherited claims with this fork's actual verification status. -->
# WorkPause verification

## Physical devices: not tested

**WorkPause has not been verified on a real phone.** In particular, there is no
verified result for the Samsung Galaxy S24 Ultra running One UI 9.

There is also no WorkPause-specific verification of Intune, Knox, employer compliance,
separate work-profile credentials, or Samsung background restrictions.
Testing performed on the original application must not be presented as testing of WorkPause.

## WorkPause 1.0.2 preview

| Check | Status |
|---|---|
| Debug APK compilation | Passed |
| Weekly schedule and location regression tests | 31 passed, no failures/errors/skips |
| Android debug lint | Passed, with warnings |
| Protected release signing | Passed: account-restricted PKCS12 key, DPAPI-protected password |
| Release APK signature and packaged identity | Passed: RSA-4096 certificate, own application ID, version 1.0.2 |
| Packaged legal notices | LICENSE and NOTICE match the root originals exactly |
| Removed testing entry point | TEST_FIRE absent from compiled code; invoking old action creates no test PendingIntent |
| Internal retry receiver | Non-exported in the packaged manifest |
| Pixel Android 15 emulator installation and foreground launch | Passed with the signed release APK |
| Compact scheduler UI | Passed: single-row header; no repeated weekday/weekly heading |
| Single WorkPause ON/OFF switch | Passed: Auto, Resume now, and Pause now absent |
| Immediate ON/OFF persistence | Passed: both states survive process restart without Save |
| Unsaved daily edits | Passed: switch preserves the visible draft without saving it |
| Save schedule while OFF | Passed: daily edits persist after restart without enabling WorkPause |
| OFF alarm cancellation | Passed: no WorkPause alarms in the active portion of the Android alarm dump |
| Daily modes and copying | Unit regression tests passed; existing controls retained |
| Settings ADB instructions | Previously smoke-tested in 1.0.0; controls unchanged |
| Location controls | Hidden for configurations with location gating disabled; existing enabled settings remain manageable |
| Queued accessibility fallback cancellation | Callbacks cancelled on OFF; delayed wake activity revalidates saved state; actual profile switching not tested |
| Work-profile pause/resume on a physical device | Not tested |
| Locked-device resume on a physical device | Not tested |
| Location-triggered resume on a physical device | Not tested |
| Samsung S24 Ultra / One UI 9 compatibility | Not tested |

The single-switch UI checks were performed on the signed 1.0.2 APK. Original daily
rules and switch state were restored after testing. The README screenshot was
refreshed from this build.

The emulator had no work profile: installation, rendering, alarm cancellation, and
scheduler editing were tested, not actual profile switching. The earlier Copy command
check verified its UI response, not an independent read of Android's clipboard contents.

The 24 weekly-schedule tests cover independent daily hours, daily modes, copying,
legacy migration, validation, overnight boundaries and OFF-day priority, overlaps,
week rollover, disabled automation, and daylight-saving changes. New regressions
ensure OFF takes priority over an all-day ON rule and day edits/copying cannot
enable WorkPause. The 7 location
tests cover fix age, invalid accuracy, saved coordinates/radius, and active-window
boundaries. They do not simulate Android's location providers or enterprise management.

Release identity: `io.github.marinfrankovic.workpause`, version name `1.0.2`, version
code `3`, non-debuggable. The release certificate SHA-256 fingerprint is:

```text
b57687219a28dd705da54e5d804e1b556d5bb4ff3f43fe35b389be016bddbefe
```

Inherited Git history and old upstream tags can mention versions such as v1.4.0 or
v1.7.0. Those are not WorkPause APK versions. WorkPause uses its own application ID,
signing certificate, and release numbering.

The release title retains "Preview" and the release is marked Latest on GitHub.
Emulator and automated checks do not establish real-device
compatibility, reliable delivery of every scheduled alarm, or enterprise compliance.
