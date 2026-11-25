# Changelog — DashGrid

DashGrid starts as a fork of **AARE 1.6.0** (upstream commit `1aa62163`). This file lists what the
fork changed; everything not mentioned is AARE's and is intentionally unchanged. See
[UPSTREAM.md](UPSTREAM.md) for the base and update process.

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
