# Changelog — DashGrid

DashGrid starts as a fork of **AARE 1.6.0** (upstream commit `1aa62163`). This file lists what the
fork changed; everything not mentioned is AARE's and is intentionally unchanged. See
[UPSTREAM.md](UPSTREAM.md) for the base and update process.

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
- No physical-car validation in this release; see TESTING.md.
