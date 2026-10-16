<!-- Modified for WorkPause: replace inherited claims with this fork's actual verification status. -->
# WorkPause verification

## CI environment

The Android build workflow uses JDK 17 and explicitly installs `platform-tools` and
`platforms;android-35`. This overrides the pinned SDK setup action's default package
list, which includes the obsolete `tools` package and fails during SDK setup.
The workflow builds the debug APK, runs unit tests and lint, and uploads a debug
artifact; it does not publish or replace the signed release APK.

## Physical phone: user-confirmed operation

On **2026-10-16**, the owner reported personally testing WorkPause 1.0.2 on a
**Samsung Galaxy S24 Ultra running One UI 9**, and confirmed that it
**works as expected**.

The owner also reported needing to enter the **work-profile PIN when the profile
turns back on**. This is an expected Android or employer-policy credential
requirement. Silent control requests a profile state change; it does not unlock
the profile or bypass its PIN.

This is a user-confirmed functional result, not an independently observed test or
a general Samsung compatibility certification. Individual test scenarios were not
recorded, so the report does not separately establish scheduled alarm delivery,
other locked-device or credential scenarios, accessibility fallback behavior, or
enterprise-policy compatibility.

### Installation and readiness checks

On **2026-10-16**, WorkPause 1.0.2 (code 3) was installed on the personal side of a
**Samsung Galaxy S24 Ultra (SM-S928B)**. The phone reported Android SDK **37**.
The One UI version was supplied by the owner, not independently checked.

| Phone check | Result |
|---|---|
| Published APK checksum | Matched the verified 1.0.2 release asset |
| Installation and foreground launch | Passed |
| Existing managed work profile | Detected by Android and by WorkPause |
| Silent-control permission | `MODIFY_QUIET_MODE` granted to the personal-side app |
| App's Silent control status | Active |
| Exact alarms | Allowed, confirmed in WorkPause Settings |
| Battery optimization exemption | Not enabled; unrestricted battery use recommended |
| Main screen status during readiness check | Waiting for unlock |
| Non-disruptive check | Work-profile flags unchanged before and after inspecting Settings |
| Actual pause/resume and scheduled switching | Not attempted during the assistant's readiness checks |
| Locked-device or separate work-profile credential handling | Not tested during the assistant's readiness checks |

No schedule was saved and the WorkPause switch was not changed during the readiness
check. Permission and readiness checks do **not** prove that silent switching works.
The assistant did not initiate switching, because it would interrupt work apps.
The owner's subsequent functional test report is recorded above.

There is no independent WorkPause-specific verification of Intune, Knox, employer
compliance, other work-profile credential configurations, or Samsung background restrictions.
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
| Galaxy S24 Ultra installation and setup readiness | Passed with limitations recorded above |
| Owner's physical-device functional test | User-confirmed: works as expected on Galaxy S24 Ultra / One UI 9 |
| Work-profile PIN on resume | Owner reports PIN entry is required when the profile turns back on; expected credential protection, not bypassed by WorkPause |
| Compact scheduler UI | Passed: single-row header; no repeated weekday/weekly heading |
| Single WorkPause ON/OFF switch | Passed: Auto, Resume now, and Pause now absent |
| Immediate ON/OFF persistence | Passed: both states survive process restart without Save |
| Unsaved daily edits | Passed: switch preserves the visible draft without saving it |
| Save schedule while OFF | Passed: daily edits persist after restart without enabling WorkPause |
| OFF alarm cancellation | Passed: no WorkPause alarms in the active portion of the Android alarm dump |
| Daily modes and copying | Unit regression tests passed; existing controls retained |
| Settings ADB instructions | Previously smoke-tested in 1.0.0; controls unchanged |
| Location controls | Hidden for configurations with location gating disabled; existing enabled settings remain manageable |
| Queued accessibility fallback cancellation | Callbacks cancelled on OFF; delayed wake activity revalidates saved state; fallback switching not independently tested |
| Work-profile pause/resume on a physical device | Not separately documented; covered only by the owner's general functional report above |
| Locked-device resume on a physical device | Not tested |
| Location-triggered resume on a physical device | Not tested |
| Samsung S24 Ultra / One UI 9 functional compatibility | Owner reports expected operation; not independently certified or established for other devices |

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
