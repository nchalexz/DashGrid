# DashGrid

**Swipe. Arrange. Drive.**

DashGrid is an independent, installable fork of [AARE](https://github.com/prototux/AARE) — the
open-source replacement for the phone side of Android Auto — that turns the car's home screen into
a set of switchable workspaces with a configurable grid of apps and widgets.

> **This repository is a fork of AARE.** The projection protocol, transport, audio, microphone,
> app hosting, the AndroidX Car App Library host, CarPlay-like and classic layouts, media and
> messaging screens, OBD support and almost all user-facing features come from AARE (GPL-3.0,
> © the AARE contributors). DashGrid adds the workspace system, its editor and its branding. See
> [UPSTREAM.md](UPSTREAM.md) for the exact base, the licence and how updates are merged.

## Кратко по-русски

**DashGrid** — независимый форк AARE: несколько переключаемых рабочих столов на экране автомобиля,
настраиваемые сетки приложений и виджетов, редактор на телефоне. Свой application ID, иконка, тема и
ключ подписи. Сборка standalone не требует AARE, но и не ставится рядом с ним.

- Рабочие столы переключаются по кругу свайпом или точками.
- Раскладки: 100 %, 50/50, 70/30, 30/70, три виджета, четыре виджета и своя сетка 10 × 6 с
  перетаскиванием, изменением размера и объединением ячеек.
- Виджеты: карта, музыка, календарь, уведомления, погода, умный дом, OBD. Карту и музыку можно
  привязать к конкретному приложению.
- Переключение не перезапускает приложения: маршрут, музыка и сессии сохраняются.
- Установка: [DashGrid-standalone-v1.0.3.apk](https://github.com/nchalexz/DashGrid/releases/download/dashgrid-v1.0.3/DashGrid-standalone-v1.0.3.apk)
  (Android 6+, без обязательного root и GMS; рядом с AARE не ставится).

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="128" alt="DashGrid icon">
</p>

<p align="center">
  <img src="docs/screenshots/dashgrid-workspaces.png" width="49%" alt="Workspace City: map 70% + music 30%, pager bar">
  <img src="docs/screenshots/dashgrid-four.png" width="49%" alt="Workspace Everything: four widgets in a 2x2 grid">
</p>
<p align="center">
  <img src="docs/screenshots/dashgrid-three.png" width="49%" alt="Workspace Route: map, music and weather side by side">
  <img src="docs/screenshots/dashgrid-day.png" width="49%" alt="The same workspace in day colours">
</p>

## What DashGrid adds

- **Workspaces** — any number of independent home screens. Each has its own name, order,
  layout, app shortcuts and widgets.
- **Cyclic switching** — swipe the pager bar (or anywhere, if you allow it), or tap a dot. Past the
  last workspace comes the first one; back from the first goes to the last. The workspace name on
  the bar is optional.
- **Grid layouts** — 100 %, 50/50, 70/30, 30/70, three widgets and four widgets, plus a custom
  10 × 6 grid where items can be dragged, resized and merged into larger cells.
- **Widgets** — map, music, next calendar event, latest notification, weather, smart-home actions
  and live OBD data, placed per workspace. A map or music widget can follow a specific app.
- **Live app panes** — render an app's own screen into its cells: Car App templates, a map, a
  media screen, a launched phone app, with the same rounded corners as the widgets. Shortcuts stay
  available as a separate, lighter tile.
- **Session-only players in the Music widget** — Yandex Music and other apps whose MediaBrowser is
  closed to third parties still appear through the system media session: artwork, track, progress
  and seek, play/pause, previous/next. No separate bridge widget, no root/GMS.
- **App shortcuts** — Car App Library apps, media apps, and DashGrid's built-in screens (phone,
  notifications, browser, car data, Home, OBD, phone screen).
- **Phone editor** — *Settings → Workspaces*: create, rename, duplicate, reorder and delete
  workspaces; pick a layout; add, move, resize and remove items; choose the default workspace;
  preview the result, or open the car screen on the phone.
- **Branding** — its own application ID (`io.dashgrid.app`), icon, theme and signing key; the
  published standalone build does not require AARE.

Everything AARE does still works: USB and wireless projection, Google Maps/Waze guidance via
notifications, music, messages, calls, the assistant, day/night themes, the dashboard widgets,
split screen, the phone-screen mirror, OBD and the desktop head unit.

## Requirements

- A phone with Android 6 or newer (Android 8+ for all features), the same as AARE.
- A car with Android Auto over USB or wireless, or the desktop head unit / car-screen preview
  to try it without a car.
- **No root and no Google Play Services are required.** Root or Shizuku only unlock the optional
  AARE features that always needed them (car audio channels, ordinary phone apps on the car
  screen, the phone's location).

## Install

This release publishes **only the standalone APK**. It defines the legacy
`android.car.permission.TEMPLATE_RENDERER`, so it **cannot be installed while AARE is installed** —
uninstall AARE first (that removes AARE's settings). It is the build for a phone without AARE.

Download [DashGrid-standalone-v1.0.3.apk](https://github.com/nchalexz/DashGrid/releases/download/dashgrid-v1.0.3/DashGrid-standalone-v1.0.3.apk)
from the [release](https://github.com/nchalexz/DashGrid/releases/tag/dashgrid-v1.0.3), then:

```sh
adb install -r DashGrid-standalone-v1.0.3.apk
```

- SHA-256: `ceda3f9aff5b886e81dbb1d9bbdbdad211f6edccb9937e008e50bc25cfc9e95a`
- Package: `io.dashgrid.app`, version 1.0.3 (10003)
- Signed with DashGrid's own key (`CN=DashGrid`; `apksigner verify --print-certs` prints the
  SHA-256). AARE and DashGrid are separate packages, but the standalone build owns the permission
  AARE also defines, so Android refuses the duplicate definition (`INSTALL_FAILED_DUPLICATE_PERMISSION`).

The side-by-side APK still exists as a local build variant (`./gradlew dashgridApk`) for phones
where AARE stays installed; it is **not attached to this release**.

Because it is a fork, apps built with the AndroidX Car App Library may trust AARE's signing
certificate but not DashGrid's. Those apps need DashGrid added to their host allow-list (the same
limitation every fork has). Apps that trust hosts holding
`android.car.permission.TEMPLATE_RENDERER` work unchanged; the permission is declared by DashGrid
as AARE does.

## Build

Requirements: JDK 17–21, Android SDK platform 37 and build-tools 36.0.0, and an SDK path in
`local.properties`:

```properties
sdk.dir=/path/to/android-sdk
```

```sh
# Debug APK
./gradlew assembleDebug

# The published standalone release APK
./gradlew dashgridStandaloneApk
# -> app/build/outputs/dashgrid/DashGrid-standalone-v1.0.3.apk

# The side-by-side variant (kept for local use, not published in this release)
./gradlew dashgridApk
# -> app/build/outputs/dashgrid/DashGrid-v1.0.3.apk
```

Signing reads `keystore.properties` (ignored by git; see
[keystore.properties.example](keystore.properties.example) and [keystore/README.md](keystore/README.md)):

```properties
storeFile=keystore/dashgrid-release.jks
storePassword=…
keyAlias=dashgrid
keyPassword=…
```

Without it, `assembleRelease` produces an unsigned APK. **Back up the keystore**: an update must be
signed with the same key or Android will refuse to install it over an existing DashGrid.

The APK name and `applicationId` are set in [app/build.gradle.kts](app/build.gradle.kts)
(`versionCode`/`versionName` are raised there too; `-PversionName=…` overrides them).

## Tests

```sh
./gradlew testDebugUnitTest
```

Workspace-model tests cover serialization, migration from the old single dashboard, cyclic
indexing, layouts, the cell grid and editing operations; [TESTING.md](TESTING.md) lists the
automated results and build checks.

## Using the car screen

- The rail, themes, day/night colours and all AARE screens are unchanged.
- With more than one workspace, a compact pager bar sits along the bottom: optional name and
  dots. A horizontal drag on the bar switches workspaces; so does a drag on empty space between
  items.
  Turn on *Swipe anywhere* to make the whole page the pager's gesture area — at the cost of the
  map's own horizontal gestures.
- The app grid (rail's menu button) still lists every app; workspaces are shortcuts, not a
  replacement for the launcher.
- Open an item to launch its app full screen. Going home returns to the workspace you left.
- The published **standalone** APK defines `android.car.permission.TEMPLATE_RENDERER`, so older
  Car App Library apps trust DashGrid as a host. Because AARE defines the same permission with a
  different certificate, the standalone build cannot be installed while AARE is installed.

## Known limitations

- **Workspaces are the home screen of all three layouts.** In the classic layout, the Home button
  opens the app grid from a workspace and returns to the last workspace from an app; in taskbar
  and dock the same rule applies.
- **Only the current workspace is live.** A map widget gets the app's surface only while its page
  is on screen; the page beside it shows a placeholder. This is deliberate: one
  `CarAppConnection` has one surface, and drawing two maps would make the navigation app fight
  itself. Route, playback and app state survive the switch.
- **Two navigation apps** can be connected (one as the active navigation app, the other opened
  beside it in split screen). Only the active one owns the dashboard map, the cluster and
  "navigate to"; Android may pause a background app's surface. See [ARCHITECTURE.md](ARCHITECTURE.md).
- The **CarControlProvider** authority is `io.dashgrid.app.car` and the assistant link scheme is
  `dashgrid://`. Assistants hard-coded for AARE's authority or `aare://` links won't call DashGrid
  until they add it.
- **Portrait head units** get the same logical 10 × 6 grid turned 90°; the editor previews only the
  landscape orientation.

## Project layout

| Path | What it is |
|---|---|
| `app/src/main/java/net/prototux/aare/` | AARE's code, kept in its original packages (upstreamable) |
| `app/src/main/java/net/prototux/aare/workspace/` | Workspace model, JSON store, migration, app catalog |
| `app/src/main/java/net/prototux/aare/ui/car/WorkspaceHome.kt` | Car-screen pager, grid, widgets, pager bar |
| `app/src/main/java/net/prototux/aare/ui/WorkspaceSettings.kt` | Phone editor |
| `app/src/test/java/net/prototux/aare/workspace/` | Model, migration and behaviour tests |
| `docs/` | AARE's upstream documentation (protocol, certificates, decisions) |

Package names inside the app intentionally stay `net.prototux.aare`: only the installable identity
(application ID, label, icon, signing key and the public identifiers listed in UPSTREAM.md) changed.
That keeps merges from AARE practical.

## Documentation

- [ARCHITECTURE.md](ARCHITECTURE.md) — how workspaces fit into AARE, lifecycle and surfaces
- [DESIGN.md](DESIGN.md) — colours, icon, type and car-UI rules
- [docs/MEDIA_COMPATIBILITY.md](docs/MEDIA_COMPATIBILITY.md) — Yandex Music, media sessions and the Music-widget fallback
- [CHANGELOG.md](CHANGELOG.md) — what changed relative to AARE 1.6.0
- [TESTING.md](TESTING.md) — tests run and the manual checklist
- [UPSTREAM.md](UPSTREAM.md) — upstream base, how to merge updates, what must not change
- [docs/assistant-links.md](docs/assistant-links.md) — `dashgrid://` assistant links
- [AARE's README](https://github.com/prototux/AARE#readme) — everything inherited

## Licence and attribution

DashGrid is distributed under the **GNU General Public License v3.0 or later**, like AARE. See
[LICENSE](LICENSE). AARE is © the AARE contributors; DashGrid's additions are © the DashGrid
contributors. The name "Android Auto" is Google's; DashGrid is not affiliated with Google.
