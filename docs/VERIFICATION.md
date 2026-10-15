<!-- Modified for WorkPause: replace inherited claims with this fork's actual verification status. -->
# WorkPause verification

## Physical devices: not tested

**WorkPause has not been verified on a real phone.** In particular, there is no
verified result for the Samsung Galaxy S24 Ultra running One UI 9.

There is also no WorkPause-specific verification of Intune, Knox, employer compliance,
separate work-profile credentials, or Samsung background restrictions.
Testing performed on the original application must not be presented as testing of WorkPause.

## WorkPause 1.0.0 preview

| Check | Status |
|---|---|
| Debug APK compilation | Passed |
| Location/schedule regression tests | 7 passed |
| Android debug lint | Passed, with warnings |
| Protected release signing | Passed: account-restricted PKCS12 key, DPAPI-protected password |
| Release APK signature and packaged identity | Passed: RSA-4096 certificate, own application ID, version 1.0.0 |
| Pixel Android 15 emulator installation and launch | Pending |
| Work-profile pause/resume on a physical device | Not tested |
| Locked-device resume on a physical device | Not tested |
| Location-triggered resume on a physical device | Not tested |
| Samsung S24 Ultra / One UI 9 compatibility | Not tested |

These build checks describe the local release candidate. A published release must point
to the corresponding source commit and pass the remaining emulator checks.

The regression tests cover fix age boundaries, invalid accuracy, invalid saved
coordinates/radius, daytime and overnight schedule boundaries, week rollover, and
disabled schedules. They do not simulate Android's location providers or enterprise management.

The first release is a preview. Emulator and automated checks do not establish real-device
compatibility, reliable delivery of every scheduled alarm, or enterprise compliance.
