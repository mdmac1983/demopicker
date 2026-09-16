# demopicker

Standalone Android app version of the "Demo Settings" decoy screen from
Launcher:OrionMD (`apk.orionmd.launcher.com`). It's a static, spoof clone of
the Android Settings UI — every row is inert except two that show a live
device value — meant to look exactly like the original mockup in both light
and dark mode.

## Identity

- Package / applicationId: `apk.oriommd.demopicker` (intentionally spelled
  differently from the launcher's `orionmd` package so the two apps can
  never collide on package name, shared resource IDs, or signing identity).
- App label shown under the launcher icon: **Settings**.
- Launcher icon: the exact uploaded asset (43x43 PNG), copied byte-for-byte
  into `res/mipmap-nodpi/` (both `ic_launcher.png` and `ic_launcher_round.png`)
  so Android never resamples/upscales it for a density bucket.

## Behavior

- `MainActivity` renders the settings list: Battery, Display, Sound,
  Storage, Accessibility, System, About tablet — matching the tile colors,
  icons, spacing and typography sampled from the provided day/night
  screenshots.
- Tapping **any** row opens `BlankActivity`, a plain white window. Nothing
  else is wired up — this is a decoy, not a functional settings app.
- **Battery** row subtitle is live: read via `BatteryManager` on launch and
  kept current with a registered `ACTION_BATTERY_CHANGED` receiver while
  the activity is visible.
- **Storage** row subtitle is live: computed from `StatFs` on the data
  partition (`used GB of total GB`, decimal GB).
- All other rows keep the static subtitle text from the original mockup.
- Theme follows the system day/night setting (`Theme.AppCompat.DayNight`);
  colors for both modes were sampled directly from the provided screenshots.

## Renaming the app

Android won't let an installed app rewrite its own manifest, so there's no
way to type genuinely arbitrary text and have it show under the home-screen
icon. Instead, **long-press the back arrow** at the top of the screen to
open an "App name" picker with 8 pre-declared names: Settings, System,
Tools, Files, Calendar, Calculator, Notes, Clock.

Under the hood, each name is its own `<activity-alias>` in
`AndroidManifest.xml`, all pointing at the same `MainActivity` and sharing
the same icon. Picking a name disables the currently-enabled alias and
enables the chosen one via
`PackageManager.setComponentEnabledSetting(...)`, and remembers the choice
in `SharedPreferences` so the picker shows the right selection next time.
Most launchers pick up the new label immediately; some need a trip back to
the home screen to refresh.

## Project layout

Plain Gradle/AGP project (no decompiled smali, unlike the launcher repo):

```
app/src/main/java/apk/oriommd/demopicker/MainActivity.java
app/src/main/java/apk/oriommd/demopicker/BlankActivity.java
app/src/main/res/layout/            (activity + one layout per row)
app/src/main/res/drawable/          (row icons, tile/card/search backgrounds)
app/src/main/res/values[-night]/    (colors, strings, themes)
app/src/main/res/mipmap-nodpi/      (exact app icon asset)
```

## Building

```
./gradlew assembleDebug
```

Output APK: `app/build/outputs/apk/debug/app-debug.apk`.

CI (`.github/workflows/build.yml`) runs the same `assembleDebug` on every
push/PR to `main` and uploads the resulting APK as a build artifact.
