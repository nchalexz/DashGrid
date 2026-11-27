# Changelog — DashGrid

DashGrid starts as a fork of **AARE 1.6.0** (upstream commit `1aa62163`). This file lists what the
fork changed; everything not mentioned is AARE's and is intentionally unchanged. See
[UPSTREAM.md](UPSTREAM.md) for the base and update process.

## 1.3.2 — VPM 0.6.2 alignment

### Cockpit

- The Car cockpit now draws **VPM 0.6.2's main screen**: gauges coolant / absolute boost / oil and
  tiles DSG oil / intake air / EGT / voltage / rail pressure. Speed, revs, gear, fuel use and fuel
  level leave the main screen (they stay in the category grid, as in VPM).
- Units follow VPM: °C, bar, V. Absolute boost and rail pressure arrive from the bridge already in
  bar, so DashGrid keeps its own factor at 1 instead of dividing by 100 a second time.
- A stale value stays a **number** — dimmed, with a stale dot, its needle faded — instead of turning
  into “—”. A dash now means only `never_read` or no value. The same rule keeps the last number on
  screen when VPM's link drops but the bridge still carries it as stale.
- The category grid takes the occurrence of a signal that belongs to its tab: `engine.map_kpa` is
  kPa under Engine and bar under Turbo, and the boost gauge reads the Turbo one.
- The visible cockpit asks VPM for a snapshot every **100 ms** instead of 500 ms. VPM refreshes MAP
  at 5 Hz, so the sample arrives within one 5 Hz frame instead of adding up to half a second of lag;
  the needle animation is 90 ms, and the page still polls nothing while it is off screen.
- The five main tiles stay in **one row** from 480 dp wide: the old 700 dp threshold wrapped them
  into two rows at 500–700 dp and shrank the gauges. Tile height is now 26 % of the screen height
  (62–110 dp), so the gauges keep the rest.
- The oil gauge takes the VAG temperature when the primary `engine.oil_c` is `never_read`: a live
  alternative is chosen over a dead primary, so a diesel that does not answer PID 0x5C still shows
  oil temperature.
- The DEMO/LIVE pill and the page dots moved to a thin header, so a long adapter status cannot
  cover the coolant dial. The gauge pictograms are VAG/Porsche-style thermometer, oil can and turbo.
- In the workspace editor the red **Remove** action for a live pane got its own row: with **Change
  app** and **Move to** in the same row it was clipped off a narrow phone.
- The VPM cockpit is added only as the whole 10 × 6 workspace; a saved smaller item now shows a
  clear hint instead of a clipped bottom-left tile. Its two screens switch by the header dots or a
  gauge tap, so a horizontal swipe always reaches DashGrid's workspace pager.

### Contract

- `VpmSignal` carries `status` (`valid` / `stale` / `never_read`, VPM 0.6.0+). A VPM older than
  0.6.0 has no status: it is derived from `stale` and the presence of a value. An unknown status
  falls back the same way and never crashes the parser.
- The bridge status wins over the `stale` flag; `stale` is used only as the fallback, as the
  contract specifies.

### Tests

- `VpmMetricsTest`, `VpmSnapshotTest` and `VpmUiStateTest` cover the new composition, stale numbers,
  never-read dashes, an old VPM without `status`, an unknown status, a dropped link with a stale
  number, and the kPa/bar duplicate of `engine.map_kpa`.
- 1,206 JVM tests (962 app + 219 `aap` + 25 `headunit`), all green; `lintVital` runs on the
  release build.

### Documentation

- [docs/VPM.md](docs/VPM.md) describes VPM 0.6.2's composition, the `status` field, the
  stale-as-number rule and the unit handoff; README and [RELEASE_NOTES.md](RELEASE_NOTES.md) are
  updated; the F-Droid changelog for `versionCode 10302` is added.

### Status

- 1.3.2 / 10302 was released on 27 November 2026.

## 1.3.1 — one cockpit entry (fix)

### Settings

- The workspace widget picker no longer offers AARe's **Car** (OBD cockpit) widget. The cockpit
  choice there is DashGrid's **VPM cockpit**, from VPM's read-only telemetry.
- A saved workspace that still holds the old `car` widget is migrated to `vpm` when the config is
  loaded, keeping the item's cells (and dropping the old one when the workspace already has VPM).
  The car screen and the phone preview then show the same cockpit instead of the old AARe one.
- The old widget's dead code and its unused "adapter not connected" string are removed; the built-in
  OBD app, its settings and the read-only OBD stack are unchanged.

### Tests

- 949 app tests, 219 `aap` tests and 25 `headunit` tests, all green; `lintVital` on the release
  build. Two new `WorkspaceModelTest` cases cover the `car` → `vpm` migration and its idempotence.

### Documentation

- [RELEASE_NOTES.md](RELEASE_NOTES.md) is rewritten for 1.3.1; README, TESTING and
  [docs/VPM.md](docs/VPM.md) describe the single cockpit entry and the migration; the F-Droid
  changelog for `versionCode 10301` is added.

## 1.3.0 — live VPM telemetry (stable)

### Cockpit

- **The Car workspace now shows real VPM data.** The native cockpit reads the read-only snapshot of
  VAG Performance Monitor **0.5.0+** over contract v1
  (`content://io.github.nchalexz.aaperfmon.telemetry`), so the three gauges, five tiles and the
  category grid draw VPM's own formatted values, display units, thresholds and category order. The
  **DEMO** pill stays while VPM is in demo; **LIVE** is shown only on a connected link.
- VPM missing, VPM without the bridge, no data yet, demo, live, a stale value and a refused read stay
  distinct, unit-tested states. Neither is a drawn zero or a black square; the missing cases
  offer **Install** or **Update VPM** / **Open on the phone**.
- At **1280 × 480** the category grid used to clip the second row: on short screens the grid now gets
  the larger half and the tiles shrink their label, value and unit to fit. 1920 × 720 is unchanged.
- VPM remains the only owner of the ELM327 adapter and the VAG UDS session. DashGrid opens no OBD
  connection, polls no PID, starts no service, and needs no root or Shizuku; the existing OBD `car`
  widget and the OBD app are untouched.

### Compatibility

- Verified with **VPM 0.5.0** on an Android 15 emulator at 1920 × 720 and 1280 × 480: `content call`
  returns `version=1`, 128 signals and a changing `updatedAt`; `dumpsys package io.dashgrid.app`
  reports `READ_TELEMETRY: granted=true`; no second ELM327/OBD attempt and no `SecurityException` in
  `adb logcat`.
- VPM 0.4.1 shows the **Update VPM** card; an uninstalled VPM shows **Install**. Removing and
  reinstalling VPM requires updating DashGrid once so Android re-grants the normal permission.
- RU and EN, day and night tokens, and both cockpit screens were exercised. There was no ELM327
  adapter, car, phone in a car or real head unit: the values are VPM's demo values, so live sensor
  readings and the adapter path remain unverified.

### Documentation

- [RELEASE_NOTES.md](RELEASE_NOTES.md) is rewritten for the stable 1.3.0 release;
  [docs/VPM.md](docs/VPM.md), README, TESTING and the limitation lists describe the bridge as
  shipped and keep the unverified points explicit.
- [docs/VPM_BRIDGE_PROMPT.md](docs/VPM_BRIDGE_PROMPT.md) remains the VPM-side contract v1.

## 1.2.0 — the VPM cockpit (test release)

### Workspaces

- **A preinstalled "Car" workspace** with the fixed id `workspace-car` and one full-screen built-in
  widget, `vpm`. The preset is added once to a configuration written before version 2; a deleted Car
  workspace does not come back, and existing names, items, the default and the last-shown workspace
  are not changed.
- **A native VPM cockpit.** DashGrid draws VAG Performance Monitor's 0.4.1 composition itself: three
  gauges (coolant, boost, oil) and five tiles (speed, revs, gear, fuel use, fuel), plus the
  extended screen with the Engine / Temperatures / Fuel / Gearbox / Turbo / Diagnostics tabs and its
  adaptive grid. The gauge geometry, the ring pair, the progress arc, the red zone, the 60 ticks, the
  night and day tokens and Titillium Web follow VPM's own sources; the DEMO pill stays.
- The cockpit reads **VPM's read-only telemetry snapshot** (`content://…aaperfmon.telemetry`,
  contract v1). VPM keeps the only ELM327 connection: DashGrid opens no OBD connection, polls no
  PID, needs no root and no Shizuku, and only asks for a snapshot while the Car page is on screen.
- VPM missing, VPM without the bridge, no data yet, demo, live, a stale value and a refused read are
  six distinct, unit-tested states. None of them is a drawn zero or a black square; the two missing
  cases offer **Install** or **Update VPM** / **Open on the phone**.
- The existing built-in OBD `car` widget and the OBD app are untouched: the VPM cockpit is a new
  widget id, not a replacement of DashGrid's own OBD stack.
- `<uses-permission …READ_TELEMETRY>` and a `<queries>` entry name VPM in the manifest.
- A debug-only `VpmPreviewActivity` renders the cockpit with VPM's mock values for emulator checks;
  it is not in a release build.

### Documentation

- [docs/VPM.md](docs/VPM.md) describes the preset, the two screens, the contract, the states, the
  emulator checks and the honest limits. README, its limitation list and its documentation table
  point there.
- [docs/VPM_BRIDGE_PROMPT.md](docs/VPM_BRIDGE_PROMPT.md) is the VPM-side task for the read-only
  provider; VPM 0.4.1 has none, so live values wait for that release.

### Tests

- 947 app tests, 219 `aap` tests and 25 `headunit` tests, all green; `lintVital` on the release
  build; Paparazzi renders of the cockpit at 1920 × 720 and 1280 × 480.

## 1.1.1 — the minimalist icon

### Design

- **New launcher icon.** One workspace in a 70/30 layout: a tall turquoise pane and two muted
  panes. Three shapes, no frame, no paging dots and no side pages; it stays readable at 48 px.
- Updated the adaptive foreground and monochrome layers, every legacy square and round PNG
  (48–192 px), the 512 px store icon, the social preview and [DESIGN.md](DESIGN.md).

### Documentation

- The README is rebuilt on the shared project standard. Its tagline now names the four core
  features: any number of screens, the 10 × 6 grid, live apps in their panes and Android app
  widgets.
- No functional changes: workspaces, live panes, Android app widgets, the dock and the Cockpit are
  the same as 1.1.0.

## 1.1.0 — Android App Widgets, a monochrome dock and the Cockpit

### Android App Widgets on workspaces

- **A standard Android App Widget can live in a workspace's cells**: the provider's own live
  RemoteViews, updating and taking taps, not a screenshot. Any installed provider is listed in the
  editor's new **Android widget** picker — app icon, widget name, minimum size and preview — with
  no hard-coded allow-list.
- The first widget of a provider asks for Android's own permission on the phone
  (`ACTION_APPWIDGET_BIND`); the provider's configuration screen (`ACTION_APPWIDGET_CONFIGURE`)
  opens there too. No Shizuku and no root are needed for hosting.
- The host view is created with the car screen's `Presentation` context, so the virtual display's
  density applies; the cells' size in dp is reported to the provider with
  `AppWidgetManager.updateAppWidgetOptions`, so a resizable widget reflows instead of being
  stretched. No tint, no scale, no dock filter: the provider's design is kept.
- Lifecycle: stable ids restored from the workspace JSON, `startListening`/`stopListening` around
  a car session, release on item/workspace removal, cancellation of add and configuration, a
  placeholder for a removed provider, one live view per id, and a release of ids no workspace
  refers to at the next session start. Several widgets and several instances of one provider are
  supported; a duplicated workspace does not copy Android widgets (one bound id, one owner).
- A workspace with a live widget keeps its normal pager gestures even with "swipe anywhere" on,
  so the widget's own horizontal gestures still work.
- **Move to another workspace** in the item inspector moves any item (an Android widget included)
  without allocating a new widget: the same bound instance follows it.
- [docs/ANDROID_WIDGETS.md](docs/ANDROID_WIDGETS.md) describes the architecture, the instructions
  and the limits; the widget-host code has not been run on a car head unit or EMUI in this release
  (see [TESTING.md](TESTING.md)).

### Dock icons

- **Monochrome dock icons**, off by default: the dock's app icons are drawn as grayscale by their
  own brightness (a ColorMatrix over the original bitmap), keeping shape, logo, detail, alpha,
  size, spacing and the existing active-app highlight. The app grid and the workspaces keep the
  original artwork.
- **Four tones, one for the light theme and one for the dark theme**: White, Grey, Dark grey and
  Black — a brightness range, not a fill. Defaults: Dark grey in light colours, White in dark
  ones. A day/night switch applies the matching tone immediately, and turning the setting off
  restores the original icons (or a single-colour theme's tint, as before).
- Processed icons are cached (`LruCache`, 64 entries, keyed by bitmap and tone). The settings have
  a preview of the phone's real icons in every tone, for both themes.

### Fixed dock buttons

- **Always show the Home button** and **Always show the App Dash button**, independent and off by
  default. Switched on, each sits at a fixed place at the driver's end of the rail, dock or
  classic bar — outside the scrolling/recent app list — on every workspace, the app grid and
  inside every app.
- The actions are AARE's: Home returns to the workspace you left, App Dash opens the app grid.
  AARE's switching button is hidden only when it would repeat a fixed action, so no action is
  drawn twice; switched off, the old behaviour is unchanged.
- Both settings are exported and imported with the other settings.

### Cockpit and car profile

- **Car profile in the Cockpit's settings** (Settings → Автомобиль): *Generic* or
  **Golf 7 2.0 TDI (CUNA) · DQ381**, which fills in diesel, 1968 cm³, a 50 L tank, 1500 kg with the
  driver and a 5000 rpm rev counter with its red line at 4600. They stay ordinary settings after
  that, so a number that's wrong for the car can be corrected without leaving the profile.
- **Values page** in the Cockpit, right after the dashboard: three dials in the style of the car's
  own instruments (coolant, boost or load, oil or intake air) and every other reading as a tile —
  speed, revs, gear, throttle, pedal, intake and atmospheric pressure, rail pressure, EGT 1 and 2,
  air flow, fuel rate, fuel level, battery and consumption.
- **Exhaust gas temperature and rail pressure** from the standard PIDs (`0x78`, `0x79`, `0x23`),
  read by the same poller; a car that doesn't report them gets no tile.
- **VAG probe and identifier scan** in the Cockpit's settings (read-only, UDS service `22`): asks
  the identifiers public VAG tools use for coolant and the DSG's oil, and walks the engine's
  `0x1100–0x11FF` and the gearbox's `0x2100–0x21FF` ranges to find the identifiers a particular
  software version uses for exhaust gas temperatures and injector corrections. The answers go to
  the connection log, so they can be read from the phone afterwards.
- **UDS values on the Values page**: a signal table, read every few seconds and sanity-checked by
  range, adds named values the standard PIDs don't carry (gearbox oil, EGT, injector corrections)
  once their identifiers are known — nothing is shown for an identifier the car doesn't answer.
- **Compact Cockpit card in a live app pane**: under 800 dp wide or 420 dp tall (a workspace pane,
  not a head unit's own screen) the Cockpit shows three dials and five main tiles instead of its
  tab bar; the tabbed page opens when the app is opened full screen.
- **Two dial looks**: the VW power display (bezel, wide value arc, red zone, needle) and a thinner
  Porsche-cluster take (hairline rim, fine ticks, long thin needle, light numerals, upper-case
  labels); the Cockpit draws itself in the Porsche one, the other is a one-line change.
- **The values page's tiles are grouped**: engine, temperatures, fuel and gearbox, one group on
  screen at a time, instead of every tile at once.
- **The pretend adapter answers UDS too**: coolant, gearbox oil and one injector correction come
  back over service `22`, so the parse, the scaling and the tiles can be exercised without a car
  (the identifiers are the public ones, the numbers are made up).
- **The Cockpit's own typeface**: Titillium Web (OFL, in `res/font`, licence in
  `assets/licenses/ofl-titillium-web.txt`) for the dial numbers, units and tile labels — a
  technical sans in the spirit of the cluster faces, since Audi Type, Porsche Next and VW Head
  are not licensable here.
- **Black backdrop and centred tiles**: on a dark theme the Cockpit sits on black, and every tile
  (card and full page alike) centres its label, number and unit both ways.
- **The Cockpit borrows the MIB head unit's space**: a dark-navy gradient behind the page, a
  bright double rim on the dials, flat hairline tiles, and the pages in a thin bar at the bottom
  with hairline separators and an accent underline, instead of chips along the top.
- **Each dial carries its pictogram**: a thermometer for coolant, an oil can for oil, air for
  intake, compression arrows for boost, a flame for EGT, a pump for fuel, a battery for the
  electrical system — in the dial's open bottom, under the unit, where nothing else sits. They are
  drawn in the app (ISO 2575-style strokes), not taken from a vendor icon set.
- **The Cockpit's tiles are flat and quiet**: no filled cards — a hairline outline and a breath of
  tint — and the numbers are pure white on a dark theme (black on a light one), the brightest thing
  in the tile.
- **The card's five tiles are chosen in the settings** (Settings → Приборы → *Card tiles*): tap
  values in the order they should appear; a value the car stops reporting is replaced by one it
  does. Speed, revs, gear, consumption, fuel, boost or load, coolant, oil, intake, EGT 1/2, rail
  pressure and battery are on the list.
- **The look is a hybrid**: the Porsche dial's hairline rim and fine ticks with the VW power
  display's thick needle; the previous two looks stay one enum value away.
- **Track, Lap and Dyno are gone from the Cockpit** (AARE's racing pages), and the first tab is now
  the same card the app pane shows, so the glanceable screen and the full window can't drift apart.
- **Tapping a dial changes what it shows** (coolant, oil, intake, boost or load, EGT 1/2, fuel,
  battery), walking past the values the other two dials already show; the choice is kept in the
  settings. A value the car stops reporting is replaced by one it does.

## 1.0.7 — hide the Android Auto version of an enabled phone app

### Added

- **Hide the Android Auto version of an enabled phone app**, in *Settings → Apps* under
  **Apps on the car screen (advanced)**, right above the list of phone apps. When a phone app
  enabled there also appears in the car's app grid as an Android Auto app, the grid lists only the
  phone version. Off by default, so existing installations don't change.
- Only the app grid changes: shortcuts already placed on a workspace keep working, the rail's
  recents can still open the Android Auto app, and the phone's app list still shows both.
- The option is exported and imported with the other DashGrid settings, and is covered by a unit
  test.

## 1.0.6 — Home from the app grid and an optional assistant button

### Added

- Settings → Voice assistant → **Show the assistant button**. Turned off, the car screen's bar
  hides its microphone button in all three layouts (taskbar, dock and classic). The steering
  wheel's voice key, voice commands and the assistant's own stop control keep working, so the
  assistant can always be stopped.
- The setting is exported and imported with the other DashGrid settings.

### Changed

- **Home** from an app opened in the app grid now opens the **main (default) workspace**, whichever
  workspace was showing when the app was opened. The app grid belongs to no workspace, so "home"
  there means the main one. An app opened from a workspace card or live pane still returns to
  that workspace (1.0.3's behaviour); Back and an app that closes itself are unchanged.

## 1.0.5 — 60/40 layout and tap-to-open live apps

### Added

- **60 / 40** workspace layout, next to 50/50, 70/30 and 30/70. Side by side it is exactly 6+4
  cells; top and bottom rounds to 4+2 rows on the 6-row grid, as 70/30 already does.
- Per-workspace **Tap a live app to open it** option. A tap on the free space of a live pane opens
  that app full screen, as tapping a widget card does; the pane's own buttons, lists and map keep
  working. Off by default, so existing workspaces behave as before.
- The option is stored in the workspace JSON and has a unit test for the round trip.

## 1.0.4 — close apps when a workspace is hidden

### Added

- Per-workspace **Close apps when hidden** option. Switching away releases the car app sessions
  and surfaces of that workspace and pauses its music; apps also used by another workspace stay
  connected. Off by default, so routes and playback keep surviving page switches.
- The setting is stored in the workspace JSON and has a unit test for the round trip.

## 1.0.3 — Home returns to the workspace; standalone-only release

### Fixed

- **Home from an app** now returns to the last workspace in the dock and classic layouts too
  (taskbar already did). In classic, a workspace is the home screen; its Home button opens the
  app grid from a workspace and returns from an app, instead of dropping into the all-apps grid.

### Distribution

- This release publishes only `DashGrid-standalone-v1.0.3.apk`. It defines
  `android.car.permission.TEMPLATE_RENDERER`, so AARE must be uninstalled first. The side-by-side
  variant remains buildable with `./gradlew dashgridApk` but is not attached to the release.

## 1.0.2 — pager simplification, pane corners and Music seek

### Changed

- Removed the separate `media_bridge` widget: it duplicated the normal **Music** widget, which
  already follows session-only apps through `SessionTracker`. Playback continuity never depended on
  the widget; the media session and `MediaHub` are independent of the workspace.
- The Music widget now shows a **seek bar** when the active media session supports seeking.
- Pager arrows removed: switch with a swipe or by tapping the dots. The workspace name on the bar
  is optional (*Settings → Workspaces → Switching → Workspace name*).
- Live app panes are clipped to the same card corner radius as the built-in widgets.

### Removed

- `media_bridge` widget, its editor entry, source picker/launch UI and the bridge-specific
  `MediaHub` API. The underlying AARE session tracking remains and feeds the Music widget.

### Kept

- Live app panes, per-widget app pinning, Russian translations, two-APK packaging and the AARE
  coexistence fix.

## 1.0.1 — live app panes, media bridge and full Russian coverage

### Added

- **Live app panes** (`pane`): the app's own UI is rendered into its workspace cells (Car App
  templates, map, media, launched phone apps). The editor has separate **Live app** and
  **Shortcut** actions; shortcuts keep the old icon tile.
- **Universal Media Bridge** (`media_bridge` widget): system media sessions via
  `MediaSessionManager`/`MediaController`; artwork, metadata, progress, seeking, play/pause,
  skip, custom actions, a player picker (active sessions + installed players) and launching the
  chosen app. Works with Yandex Music's session when its MediaBrowser library is unavailable.
- `MediaHub` additions: `sessions`, `musicApps`, `selectSessionPackage`, `seekTo`, `runAction`,
  `launchApp`; `PlaybackInfo` carries capabilities/actions.
- Per-widget app pinning is explicit in the editor (map/music); workspace switching no longer
  changes the global navigation app.
- Full Russian translations for 105 previously missing strings (audio, root, voice replies, OBD,
  themes) and all new UI; corrected semantic errors found in the existing Russian strings.

### Fixed

- The side-by-side APK no longer declares `android.car.permission.TEMPLATE_RENDERER`, so it
  installs beside AARE on EMUI (`INSTALL_FAILED_DUPLICATE_PERMISSION`).
- The workspace editor no longer queries the package manager in a way that crashes headless
  previews/tests; a config override lets the editor be rendered without SharedPreferences.

### Research

- Yandex Music APK 2020.11.1 declares an exported but **disabled** `MusicBrowserService`
  (`android:enabled="false"`); the non-exported `MediaSessionService` is still visible through
  `MediaSessionManager`. Full library access under no-root/no-app-change constraints is therefore
  impossible; the bridge is the supported fallback. See `docs/MEDIA_COMPATIBILITY.md`.

### Packaging

- Version 1.0.1 (10001), same two APK variants and signing key as 1.0.0.

## 1.0.0 — initial DashGrid release

### Added

- **Workspaces**: any number of independent car-screen home screens, each with an id, name,
  order, layout, items and per-item settings.
- **Cyclic switching** in both directions: horizontal swipe on the pager bar (and on empty
  workspace space), screen arrows, tappable page dots, and a compact name+count indicator. The
  ring wraps `1 → 2 → 3 → 1` and `1 → 3 → 2 → 1`.
- **Layouts**: 100 %, 50/50, 70/30, 30/70, three widgets, four widgets, and a custom 10 × 6 grid.
  Horizontal or vertical split for the two-item layouts. Existing AARE styles/layouts are kept.
- **Custom grid editing**: long-press drag, width/height resizing (merging cells), overlap
  rejection, per-workspace item positions.
- **Workspace editor on the phone** (*Settings → Workspaces*): create, delete, rename, duplicate,
  reorder, choose layout/axis, add/remove widgets and app shortcuts, resize/move, choose the
  default workspace, pin a map or music widget to an app, and preview the result (grid preview and
  the existing car-screen preview).
- **Widget support**: map, music, calendar, latest notification, weather, smart-home Home card and
  OBD card — the existing `DashCard` widgets, placed per workspace.
- **App shortcuts**: built-in DashGrid screens, installed AndroidX Car App Library apps and media
  apps (found through their MediaBrowserService).
- **Storage**: versioned JSON in the existing settings file, `last`/`default` workspace,
  switching settings, automatic migration of AARE's old single dashboard (up to seven cards) into
  the first workspace, a preserved copy of an unreadable config, and backup-file support.
- **Lifecycle work**: only the visible workspace is composed; only its map widget owns the app's
  surface; switching never disconnects an app, so routes, playback and sessions survive.
- **Brand**: `DashGrid` name and slogan, new adaptive/monochrome/legacy icon, `DashGridTheme`
  (graphite + turquoise tokens), Russian and English strings for all new UI.
- **Tests**: workspace model, cyclic indexing, layouts/cells, JSON, sanitization, migration and
  editing operations (see TESTING.md).

### Changed (fork identity)

| What | AARE | DashGrid |
|---|---|---|
| Application ID | `net.prototux.aare` | `io.dashgrid.app` |
| App name | AARE | DashGrid |
| Release signing | AARE's key | DashGrid's own key (`keystore/`, not committed) |
| APK | `aare-<version>.apk` | `DashGrid-v<version>.apk` |
| Version | 1.6.0 / 10600 | 1.0.0 / 10000 |
| Xposed module ID | `net.prototux.aare.xposed` | `io.dashgrid.app.xposed` |
| Car-control provider | `net.prototux.aare.car` | `io.dashgrid.app.car` |
| Assistant link scheme | `aare://` | `dashgrid://` |
| Update check | AARE GitHub releases, on | Off by default (no DashGrid release repo configured) |
| Car theme default (new install) | AARE | DashGrid |

Internal Java/Kotlin packages deliberately stay `net.prototux.aare…`: renaming them would make
upstream merges and bug reports much harder. The application ID is the Android identity, and it is
different, so AARE and DashGrid install side by side with independent data.

### Packaging

- Two signed APKs: `DashGrid-v1.0.0.apk` for installing **beside AARE** (it only uses, never
  declares, `android.car.permission.TEMPLATE_RENDERER`) and
  `DashGrid-standalone-v1.0.0.apk` for phones **without AARE** (it declares the legacy permission
  so older Car App Library apps trust the host). This fixes
  `INSTALL_FAILED_DUPLICATE_PERMISSION` when AARE is installed.

### Preserved from AARE

USB/wireless Android Auto projection, AAP transport and TLS, the certificate download, audio and
microphone paths, notification access, media/messaging/calls, assistant and car tools, split
screen, phone-screen mirror, OBD/Cockpit, car data, browser, Home actions, all themes and all
three layouts, the desktop head unit, the Xposed module (with a new ID), the licence and the
upstream documentation.

### Known limitations

- Workspaces are the home of the taskbar and dock layouts; the classic layout keeps its app-grid
  home.
- One live map surface per connection; inactive workspaces show a placeholder for their map. Two
  navigation apps can be connected (split screen / different workspaces) but only one is active.
- Assistants or apps hard-coded to AARE's provider authority or `aare://` links need updating.
- Car App Library apps that allow-list AARE's signing digest must add DashGrid's.
