# Testing

## Automated tests

```sh
./gradlew :aap:test :app:testDebugUnitTest :headunit:test
```

Result for this build (1.2.0, 2026-11-27):

```
app       947 tests, 0 failures, 0 errors, 0 skipped
aap       219 tests, 0 failures, 0 errors, 0 skipped
headunit   25 tests, 0 failures, 0 errors, 0 skipped
```

`app`'s suite is AARE's whole unit-test suite plus DashGrid's workspace, dock and Android-widget
tests. What the new tests cover:

- **Dock tones** (`ui/theme/DockIconToneTest`): every matrix is grayscale (the three colour rows
  are identical), alpha passes through untouched, the luminance weights are sRGB's, White is
  lighter than Grey and Black is darker than Dark grey, every tone has a distinct matrix, and
  the stored ids are stable (`white`, `grey`, `dark_grey`, `black`).
- **Fixed buttons** (`ui/car/DockButtonsTest`): AARE's switching button stays with both settings
  off; a fixed button suppresses it only on the screen where it would repeat its action; with
  both fixed nothing is left for the switching button.
- **Android widgets in the model** (`workspace/AndroidWidgetWorkspaceTest`):
  JSON round-trip of `kind = android_widget` with its id and provider, dropping an item that lost
  either, one bound id owned by one item across the whole config, the free grid giving a widget
  the cells its provider's minimum asks for, and a preset keeping its own slots.
- **VPM cockpit preset** (`workspace/WorkspaceModelTest`): the fixed `workspace-car` id and the
  single 10 × 6 built-in `vpm` widget (no app shortcut, no pane); the preset is added once and a
  second call is a no-op; an old dashboard migration keeps every item and the new workspace does not
  duplicate through a JSON round-trip; deleting the Car workspace does not bring it back (the config
  version is the marker).
- **VPM snapshot contract** (`vpm/VpmSnapshotTest`): a full contract-v1 bundle, another version
  rejected, every connection kind's truthfulness (demo/connected draw, connecting/disconnected/
  error do not), broken JSON becoming empty, one bad entry not losing the others, missing values
  staying missing, one entry per `(id, category)` with a signal allowed under two tabs, the label
  fallback and the 128-entry cap.
- **VPM readings and categories** (`vpm/VpmMetricsTest`): exactly three gauges and five tiles, demo
  and live drawn, a disconnected car and a missing snapshot not drawn, a silent car as “—” and not
  as zero, a stale value as “—” with no needle, boost in bar on VPM's −1…2 scale, VPM's warning and
  critical thresholds, the VAG oil alternative, a gear without a unit, the category order preserved,
  a category-less signal falling back to its known metric and the European number style.
- **VPM state cards** (`ui/car/vpm/VpmUiStateTest`): a missing app is a card and never an exception;
  an installed app without the bridge asks for an update; a provider with no snapshot still draws
  the cockpit with dashes; a snapshot is drawn; a refused read is an explanation.
- **Picker sizing** (`ui/AndroidWidgetPickerTest`): the minimum-size-to-cells rule and its clamps.
- **Settings backup** (`SettingsBackupTest`): the dock-icon and fixed-button keys export and
  import, with their types.
- The existing workspace tests (cyclic paging, presets, migration, JSON tolerance) keep passing.

## What was checked beyond unit tests

- `:app:assembleDebug` and `:app:assembleStandalone` compile and pass `lintVital`.
- **The VPM cockpit and the Car workspace were run on an emulator**: `sdk_gphone64_arm64`, Android
  15 (API 35), 1920 × 720 and 1280 × 480, with VPM 0.4.1 installed and no telemetry bridge. A fresh
  `pm clear` produced **Home + Car**; the Car workspace is 100 % and drew the calm “VPM is installed,
  but this version doesn't share telemetry” card with **Update VPM** and **Open on the phone**, with
  no crash. The cockpit itself was run through the debug-only `VpmPreviewActivity` with VPM's mock
  values: both screens at both resolutions, a horizontal swipe from the main screen to categories, a
  tab tap and a gauge tap opening its category. The log showed a single bridge “no provider” line
  (not one per poll) and no OBD/ELM327 attempt.
- The `vpm` widget is a new widget id: the built-in OBD `car` widget and the OBD app are untouched.
  The manifest adds one normal `uses-permission` (VPM's read-only telemetry) and a `<queries>` entry
  and **no** exported component.
- The new screens were rendered with Paparazzi (JVM, no emulator): the four tones on the light
  and dark themes, a monochrome dock, the two fixed buttons, and the workspace editor with an
  Android-widget item. The images are in `docs/screenshots/`.
- The app's minSdk is 23 and every Android API used by the widget host is guarded or available
  from 23: `AppWidgetHost.createView`, `ACTION_APPWIDGET_BIND/CONFIGURE`,
  `getInstalledProviders`, `updateAppWidgetOptions`, `AppWidgetProviderInfo.configure/label/
  resizeMode`, `loadPreviewImage`; `Bitmap.generationId` (26) has a fallback.

## Not verified — needs a phone and a car

The cockpit was run on the emulator described above, but **nothing below has been run on a real
head unit, in a car, or on EMUI**, and no real VPM could feed it (VPM 0.4.1 has no bridge). It must
not be reported as confirmed compatible until it has.

- App Widget hosting on the car screen: the AppWidgetHostView drawing on AARE's virtual display
  (Presentation), its density and cell sizes, RemoteViews updates, taps and `PendingIntent`s, the
  provider permission dialog, a provider's configuration screen, a provider removed while its
  item exists, and a widget on a reconnected session.
- Widget gestures with `swipe anywhere` on, the knob/D-pad inside RemoteViews, and the phone
  preview while a real car session is also running (the preview shows a placeholder in that case).
- The dock tones and fixed buttons on a real head unit: white on a dark theme, black on a light
  one, the active-app ring, the day/night switch while connected, and a live settings change
  (the settings listener and `LocalCarNight` path are unit-/render-checked, not device-checked).
- The preview with real third-party icons (2GIS, Yandex Navigator, Yandex Music, phone): the
  preview loads installed app icons at runtime; only DashGrid's own artwork was rendered here.
- EMUI 14+ and any particular head unit.

## Manual checklist for a phone and a car

```sh
adb uninstall net.prototux.aare   # required for the standalone APK
adb install -r app/build/outputs/dashgrid/DashGrid-standalone-v1.2.0.apk

# Preview without a car
# DashGrid → Settings → Workspaces → Preview car screen
```

1. **VPM Car workspace.** Install VAG Performance Monitor with the read-only bridge, then restart
   DashGrid. On a fresh install, swipe to **Car**: the cockpit should fill the page, both screens
   should swipe, a gauge tap should open its category, the tabs should switch the grid, and the
   **DEMO** pill should be visible while VPM is in demo. Leave and return to Car: the last screen
   should come back. With VPM uninstalled, the page should show **Install**; with a VPM without the
   bridge, **Update VPM** and **Open on the phone**. Check `adb logcat` for a second ELM327
   connection: VPM is the only owner, and DashGrid must not open the OBD app at the same time.
2. **Dock icons.** Settings → Car screen → Dock icons: switch *Monochrome* on. In the preview,
   check the rail/dock icons: shape and logo readable, no one-colour discs, active app still
   highlighted. Switch the light and dark tones through White/Grey/Dark grey/Black and back to
   off; the originals return. The app grid keeps its colours throughout.
3. **Day/night while connected.** With *Colours: Car's day/night*, cover the car's light sensor or
   toggle the car's night mode: the tone follows without a reconnect. Change the tone while the
   car is connected: it applies immediately.
4. **Fixed buttons.** Turn on *Always show Home*, then *Always show App Dash*. On a workspace, in
   the app grid and inside an app, both stay at the same place; recent apps don't push them; the
   Home and Apps actions are unchanged and no action appears twice. Repeat in all three layouts
   (taskbar, dock, classic) and in portrait/landscape, with two pinned and with one.
5. **Add a widget.** Settings → Workspaces → edit → **Android widget** → choose, for example, a
   clock. Allow the permission on the phone, configure it if asked. Check: app icon + widget name
   + minimum size + preview in the list; the widget appears in the cells on the car; cancel the
   permission once (nothing is added) and the configuration once (nothing is added).
6. **Widget behaviour.** Wait for an update (a clock minute, a weather refresh); press a widget
   button (a media control, a calendar day); scroll a list widget; resize its cells in the editor
   and check a resizable provider reflows without stretching; put two instances of one provider
   on two workspaces; pin the map next to it and check the map stays live.
7. **Lifecycle.** Switch workspaces away and back, open another workspace's app, disconnect and
   reconnect the car: the widget returns with its state. Remove the provider app (or disable it):
   the cell says *Widget unavailable*; remove the item from the editor. Remove a workspace with a
   widget: the next add gets a new id. Move a widget to another workspace with *Move to another
   workspace*: the same widget follows, not a copy.
8. **Regression.** Workspaces, layouts, live panes, map/music, pager ring, media playback,
   settings backup, and the existing AARE connection/apps still behave as in 1.0.7.

## Build verification

```sh
./gradlew :app:assembleStandalone    # signed with keystore.properties
./gradlew dashgridStandaloneApk      # dist-style name
```

Published artifact (filled at release time):

```
app/build/outputs/dashgrid/DashGrid-standalone-v1.2.0.apk
19,707,760 bytes
SHA-256 ee4e228afa6a94d2d4fbee1122237c8f359e221e0bdaed8208dfd4d34c939546
SHA-256 f8ac4d91fe220fb0c2f01ac9cb810f5264086d2995e49c396636025c2310778a
```

Verified from the APK itself:

```sh
apksigner verify --print-certs app/build/outputs/dashgrid/DashGrid-standalone-v1.2.0.apk
aapt2 dump badging app/build/outputs/dashgrid/DashGrid-standalone-v1.2.0.apk
```

- `package: io.dashgrid.app`, `versionCode 10200`, `versionName 1.2.0`;
- `application-label: DashGrid`;
- signature: DashGrid's own key (`CN=DashGrid`);
- the standalone APK declares `android.car.permission.TEMPLATE_RENDERER`;
- provider authorities and the `dashgrid` scheme are unchanged from 1.0.7;
- the new code adds **no** permission and no exported component for Android widgets: a host needs
  none, and binding uses Android's own user-confirmation dialog.

For an update, rebuild with a higher `versionCode` and the same keystore, then `adb install -r`.

## Compatibility notes

- **Yandex Music**: the inspected APK declares `MusicBrowserService` with
  `android:enabled="false"`, so its full library is not available to a third-party host. The Music
  widget shows and controls the active media session (artwork, metadata, progress/seek,
  play/pause, previous/next). Details in [docs/MEDIA_COMPATIBILITY.md](docs/MEDIA_COMPATIBILITY.md).
- **Two navigation apps** can be connected (split screen/different workspaces); only one is the
  active navigation app for the dashboard map, cluster and "navigate to". Android may pause the
  background app's surface.
- **One live map surface per `CarAppConnection`**; a workspace that is not current shows a
  placeholder for its map.
- **One live AppWidgetHostView per bound id**: only the visible page hosts it; the phone preview
  shows a placeholder while a real car screen is connected.
- **VPM cockpit needs VPM's bridge.** VPM 0.4.1 exports no telemetry snapshot; the cockpit shows
  the “update VPM” card until a VPM release adds the read-only provider
  ([docs/VPM_BRIDGE_PROMPT.md](docs/VPM_BRIDGE_PROMPT.md)). DashGrid needs no update then.
- **Standalone vs side-by-side**: the standalone APK cannot be installed while AARE is installed
  because both define `TEMPLATE_RENDERER` with different certificates.
- **External themes**: investigated in [docs/EXTERNAL_THEMES.md](docs/EXTERNAL_THEMES.md); not
  implemented, as the task requires agreement before adding a new theme format.
