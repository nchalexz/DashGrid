<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="104" alt="DashGrid icon">
</p>

<h1 align="center">DashGrid</h1>

<p align="center"><strong>Swipe. Arrange. Drive.</strong></p>

<p align="center">
  An independent fork of AARE that turns the car screen into switchable workspaces —<br>
  a configurable grid of live apps and widgets, edited from the phone.
</p>

<p align="center">
  <img alt="version 1.0.3" src="https://img.shields.io/badge/version-1.0.3-36D6C3?style=flat-square&labelColor=101418">
  <img alt="Android 6+" src="https://img.shields.io/badge/Android-6%2B-36D6C3?style=flat-square&labelColor=101418">
  <img alt="GPL-3.0" src="https://img.shields.io/badge/licence-GPL--3.0-94A3B8?style=flat-square&labelColor=101418">
  <img alt="fork of AARE" src="https://img.shields.io/badge/fork-AARE-94A3B8?style=flat-square&labelColor=101418">
</p>

<p align="center">
  <a href="https://github.com/nchalexz/DashGrid/releases/download/dashgrid-v1.0.3/DashGrid-standalone-v1.0.3.apk">
    <img alt="Download DashGrid v1.0.3" src="https://img.shields.io/badge/Download-DashGrid--v1.0.3-36D6C3?style=for-the-badge&labelColor=101418">
  </a>
</p>

<p align="center">
  <img src="docs/screenshots/dashgrid-workspaces.png" width="100%" alt="DashGrid on a 16:9 car screen: map and music workspace, pager dots">
</p>

> **Fork of AARE.** The Android Auto projection protocol, transport, audio, microphone, app
> hosting, Car App Library integration and the built-in screens are AARE's (GPL-3.0, © the AARE
> contributors). DashGrid adds workspaces, the grid editor, live app panes and its own identity.
> See [UPSTREAM.md](UPSTREAM.md).

| Workspaces | Grid | Widgets | Layouts |
|:--|:--|:--|:--|
| Any number, cyclic paging | 10 × 6, drag, resize, merge | 7 built-in types + live app panes | Taskbar, dock, classic |

## Workspaces, not a dashboard

Each workspace is its own home screen: a name, an order, a layout and a set of app shortcuts,
live app panes and widgets. Switch with a horizontal swipe, the dots, or the Home button — past
the last workspace comes the first one. The workspace you leave an app from is the one you return
to.

The editor lives on the phone, where a keyboard and precision touch make sense. The car screen
stays a viewer: no setup UI, no accidental drags while driving.

## What you get

| Area | What it does |
|:--|:--|
| **Workspaces** | Independent pages with their own layouts and items; create, rename, duplicate, reorder, delete, choose a default. |
| **Grid** | 100 %, 50/50, 70/30, 30/70, three widgets, four widgets, or a custom 10 × 6 grid. Long-press to drag, change width/height to merge cells. |
| **Live app panes** | Render an app's own screen into its cells — Car App templates, a map, a media screen, a launched phone app — with the same rounded corners as widgets. |
| **Widgets** | Map, music, calendar, latest notification, weather, smart-home actions and live OBD. Map and music widgets can be pinned to a specific app per workspace. |
| **Music** | The Music widget follows session-only players (Yandex Music, podcasts, radio) through `MediaSessionManager`, with artwork, progress, seek, play/pause and skip. No root, no GMS. |
| **Pager** | Compact bar with dots and an optional workspace name. No arrows: swipe the bar, empty space, or tap a dot. |
| **Identity** | Own application ID and signing key, icon, DashGrid theme, Russian and English UI. |

## Screens

| 70 / 30 with pager | Four widgets |
|:--:|:--:|
| <img src="docs/screenshots/dashgrid-workspaces.png" width="100%" alt="70/30 map and music workspace"> | <img src="docs/screenshots/dashgrid-four.png" width="100%" alt="Four widgets in a 2x2 grid"> |
| **Three columns** | **Day colours** |
| <img src="docs/screenshots/dashgrid-three.png" width="100%" alt="Three widgets in columns"> | <img src="docs/screenshots/dashgrid-day.png" width="100%" alt="Workspace in day colours"> |

## Install

This release publishes **one APK: the standalone build**. It defines the legacy
`android.car.permission.TEMPLATE_RENDERER`, so it cannot be installed while AARE is installed.
Uninstall AARE first; this removes AARE's settings.

```sh
adb uninstall net.prototux.aare
adb install -r DashGrid-standalone-v1.0.3.apk
```

- Download: [DashGrid-standalone-v1.0.3.apk](https://github.com/nchalexz/DashGrid/releases/download/dashgrid-v1.0.3/DashGrid-standalone-v1.0.3.apk)
- Package: `io.dashgrid.app`, version 1.0.3 (10003)
- SHA-256: `ceda3f9aff5b886e81dbb1d9bbdbdad211f6edccb9937e008e50bc25cfc9e95a`
- Signed with DashGrid's own key (`CN=DashGrid`); updates must use the same key.

The side-by-side variant, for a phone where AARE stays installed, is a local build only:
`./gradlew dashgridApk`. It is not attached to this release.

## Build

<details>
<summary>Requirements and commands</summary>

JDK 17–21, Android SDK platform 37, build-tools 36.0.0, and an SDK path in `local.properties`:

```properties
sdk.dir=/path/to/android-sdk
```

```sh
# Debug APK
./gradlew assembleDebug

# Published standalone release APK
./gradlew dashgridStandaloneApk
# -> app/build/outputs/dashgrid/DashGrid-standalone-v1.0.3.apk

# Side-by-side variant for installing next to AARE (local use)
./gradlew dashgridApk
# -> app/build/outputs/dashgrid/DashGrid-v1.0.3.apk
```

Signing reads `keystore.properties`, which is not committed. See
[keystore.properties.example](keystore.properties.example) and [keystore/README.md](keystore/README.md).
Back up the keystore and its passwords: without them a future build cannot update an installed
DashGrid.

</details>

## Compatibility

| Case | Result |
|:--|:--|
| Android 6+ phone, no root, no Google Play Services | Works with AARE's non-root audio and connection paths. |
| AARE installed alongside | Use the local side-by-side build; the published standalone APK conflicts on `TEMPLATE_RENDERER`. |
| Yandex Music | Full library is closed: its `MusicBrowserService` is disabled in the inspected APK. The Music widget controls the active session instead. [Details](docs/MEDIA_COMPATIBILITY.md). |
| Other music apps | AARE's MediaBrowser UI for apps that expose an enabled library; the Music widget for session-only apps. |
| Head units | AARE's USB/wireless Android Auto transport and certificate flow are unchanged. |

## Documentation

| Document | Contents |
|:--|:--|
| [ARCHITECTURE.md](ARCHITECTURE.md) | How workspaces, panes, paging and the media fallback fit into AARE |
| [DESIGN.md](DESIGN.md) | Palette, icon, typography and car-UI rules |
| [docs/MEDIA_COMPATIBILITY.md](docs/MEDIA_COMPATIBILITY.md) | Yandex Music diagnosis, session fallback and test plan |
| [TESTING.md](TESTING.md) | Automated tests and build checks |
| [CHANGELOG.md](CHANGELOG.md) | Changes relative to AARE 1.6.0 |
| [UPSTREAM.md](UPSTREAM.md) | Base commit, merge process, identifiers that must not change |

## По-русски

**DashGrid** — независимый форк AARE: переключаемые рабочие столы, живые панели приложений и
виджеты, редактор на телефоне. Свой application ID и ключ подписи.

- Рабочие столы листаются свайпом или точками; название на панели можно отключить.
- Раскладки 100 %, 50/50, 70/30, 30/70, три/четыре виджета и своя сетка 10 × 6.
- Музыка продолжает играть при переключении; Яндекс Музыка управляется через медиасессию.
- Установка: [DashGrid-standalone-v1.0.3.apk](https://github.com/nchalexz/DashGrid/releases/download/dashgrid-v1.0.3/DashGrid-standalone-v1.0.3.apk).
  AARE должен быть удалён: standalone-сборка не ставится рядом с ним.

## Licence

GPL-3.0-or-later. AARE © the AARE contributors; DashGrid additions © the DashGrid contributors.
Android Auto is a Google trademark; DashGrid is not affiliated with Google.
