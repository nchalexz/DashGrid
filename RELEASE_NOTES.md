**Swipe. Arrange. Drive.**

DashGrid 1.0.3 is released as the **standalone build only**. It is an independent fork of
[AARE](https://github.com/prototux/AARE) with switchable workspaces, live app panes and a simplified
pager.

## Highlights

- **Home button fixed**: from an app it returns to the workspace you left, not to the all-apps
  grid. The classic layout now uses workspaces as its home too; Home opens the app grid from a
  workspace and returns from an app.
- **Standalone only**: this release attaches only `DashGrid-standalone-v1.0.3.apk`. It defines
  `android.car.permission.TEMPLATE_RENDERER`, so **AARE must be uninstalled first**; the two cannot
  be installed together. The side-by-side variant still exists as a local build
  (`./gradlew dashgridApk`), but is not published here.
- **Pager**: no arrows; switch with a swipe or by tapping the dots. The workspace name is optional.
- **Live app panes** render an app into its cells with the same rounded corners as the widgets.
- **Music widget** follows session-only players such as Yandex Music (metadata, artwork,
  progress/seek, play/pause, previous/next). There is no separate media-bridge widget; playback is
  independent of it and never stops on a workspace switch.
- **Russian**: the short summary is on the README; all new screens are localized.

## Install

```sh
adb uninstall net.prototux.aare   # required if AARE is installed: the standalone APK owns the same permission
adb install -r DashGrid-standalone-v1.0.3.apk
```

- Package: `io.dashgrid.app`, version 1.0.3 (10003)
- SHA-256: `ceda3f9aff5b886e81dbb1d9bbdbdad211f6edccb9937e008e50bc25cfc9e95a`
- Signed with DashGrid's own key (not AARE's); future updates must use the same key.

Requires Android 6+ and a car with Android Auto (USB or wireless), or *Settings → Workspaces →
Preview car screen* without a car. No root and no Google Play Services are required.

## Yandex Music

The inspected official APK declares its `MusicBrowserService` with `android:enabled="false"`, so
the full library (My Wave, playlists, search) is unavailable to a third-party host without
root/Shizuku or modifying the app. The **Music widget** is the supported fallback: it shows the
active Yandex Music media session (artwork, title/artist, progress/seek, play/pause, next/previous)
without root or GMS; notification access is required. Evidence and the test plan:
[docs/MEDIA_COMPATIBILITY.md](https://github.com/nchalexz/DashGrid/blob/main/docs/MEDIA_COMPATIBILITY.md).

## Documentation

[README](https://github.com/nchalexz/DashGrid#readme) ·
[media compatibility](https://github.com/nchalexz/DashGrid/blob/main/docs/MEDIA_COMPATIBILITY.md) ·
[architecture](https://github.com/nchalexz/DashGrid/blob/main/ARCHITECTURE.md) ·
[test report](https://github.com/nchalexz/DashGrid/blob/main/TESTING.md) ·
[upstream](https://github.com/nchalexz/DashGrid/blob/main/UPSTREAM.md)

## Known limitations

- Yandex Music's full library is unavailable while its MediaBrowser service is disabled; the
  Music widget controls playback through the media session but cannot browse My Wave/playlists.
- One live map surface per connection; inactive workspaces show a placeholder.
- Car App Library apps that allow-list AARE's signing digest must add DashGrid's.
- This is a fork, not an official AARE release. AARE is GPL-3.0; DashGrid keeps that licence.
