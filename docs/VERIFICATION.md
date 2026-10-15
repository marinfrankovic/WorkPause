<!-- Modified for WorkPause: replace inherited claims with this fork's actual verification status. -->
# WorkPause verification

## Physical devices: not tested

**WorkPause has not been verified on a real phone.** In particular, there is no
verified result for the Samsung Galaxy S24 Ultra running One UI 9.

There is also no WorkPause-specific verification of Intune, Knox, employer compliance,
separate work-profile credentials, or Samsung background restrictions.
Testing performed on the original application must not be presented as testing of WorkPause.

## WorkPause 1.0.1 preview

| Check | Status |
|---|---|
| Debug APK compilation | Passed |
| Weekly schedule and location regression tests | 29 passed, no failures/errors/skips |
| Android debug lint | Passed, with warnings |
| Protected release signing | Passed: account-restricted PKCS12 key, DPAPI-protected password |
| Release APK signature and packaged identity | Passed: RSA-4096 certificate, own application ID, version 1.0.1 |
| Packaged legal notices | LICENSE and NOTICE match the root originals exactly |
| Removed testing entry point | TEST_FIRE absent from compiled code; invoking old action creates no test PendingIntent |
| Internal retry receiver | Non-exported in the packaged manifest |
| Pixel Android 15 emulator installation and foreground launch | Passed with the signed release APK |
| Compact scheduler UI | Passed: single-row header; no repeated weekday/weekly heading |
| Daily ON/OFF modes | Passed: matching descriptions; time fields hidden in both modes |
| Copy and Save | Passed: copied ON mode survives process restart; original day modes restored afterward |
| Settings ADB instructions | Passed: correct application-specific command shown; Copy changes to Command copied |
| Location controls | Hidden for configurations with location gating disabled; existing enabled settings remain manageable |
| Work-profile pause/resume on a physical device | Not tested |
| Locked-device resume on a physical device | Not tested |
| Location-triggered resume on a physical device | Not tested |
| Samsung S24 Ultra / One UI 9 compatibility | Not tested |

The scheduler UI checks were performed for WorkPause 1.0.0; 1.0.1 retains those
controls and corrects the README, About description, and accessibility service label.
Build, unit tests, lint, signing, packaged version, and installation are rechecked
for 1.0.1. The emulator had no work
profile: installation, rendering, and scheduler editing were tested, not actual profile
switching. The Copy command check verified its UI response, not an independent read of
Android's clipboard contents.

The 22 weekly-schedule tests cover independent daily hours, daily modes, copying,
legacy migration, validation, overnight boundaries and OFF-day priority, overlaps,
week rollover, disabled automation, and daylight-saving changes. The 7 location
tests cover fix age, invalid accuracy, saved coordinates/radius, and active-window
boundaries. They do not simulate Android's location providers or enterprise management.

Release identity: `io.github.marinfrankovic.workpause`, version name `1.0.1`, version
code `2`, non-debuggable. The release certificate SHA-256 fingerprint is:

```text
b57687219a28dd705da54e5d804e1b556d5bb4ff3f43fe35b389be016bddbefe
```

Inherited Git history and old upstream tags can mention versions such as v1.4.0 or
v1.7.0. Those are not WorkPause APK versions. WorkPause uses its own application ID,
signing certificate, and release numbering.

The first release is a preview. Emulator and automated checks do not establish real-device
compatibility, reliable delivery of every scheduled alarm, or enterprise compliance.
