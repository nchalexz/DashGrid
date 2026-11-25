**Swipe. Arrange. Drive.**

DashGrid 1.0.2 is a UI and compatibility update of the independent fork of
[AARE](https://github.com/prototux/AARE). It keeps live app panes, adds a seek bar to the normal
Music widget, removes the separate media-bridge widget, and simplifies the pager.

## Highlights

- **Live app panes** render an app's own screen into its cells (Car App templates, map, media,
  launched phone apps). Panes now use the same rounded corners as the built-in widgets.
- **Music widget** still follows session-only apps such as Yandex Music through `SessionTracker`,
  and now has a **seek bar** when the media session supports it. There is **no separate
  “Universal Media Bridge” widget any more**: it duplicated the Music widget. Playback does not
  depend on the widget and is not interrupted by switching workspaces.
- **Pager**: the arrows are gone. Switch with a swipe or by tapping the dots; the workspace name
  on the bar is optional in *Settings → Workspaces → Switching*.
- **Pinned widgets**: a map or music widget can be pinned to a specific app per workspace instead
  of following the last opened one.
- **Russian**: all previously missing strings are translated; the new UI is localized.

## Yandex Music

The official APK inspected for this release declares its `MusicBrowserService` with
`android:enabled="false"`, so a third-party host cannot use its full library (My Wave, playlists,
search) without root/Shizuku or modifying the app. The **Music widget** is the supported fallback:
it shows the active Yandex Music media session (artwork, title/artist, progress/seek, play/pause,
next/previous) without root or GMS; notification access is required. Full evidence and the test
plan are in [docs/MEDIA_COMPATIBILITY.md](https://github.com/nchalexz/DashGrid/blob/main/docs/MEDIA_COMPATIBILITY.md).

## Install

```sh
adb install -r DashGrid-v1.0.2.apk                 # with AARE installed
adb install -r DashGrid-standalone-v1.0.2.apk      # without AARE
```

- Package: `io.dashgrid.app`, version 1.0.2 (10002)
- Side-by-side SHA-256: `8c43f142f47dd8a8d7ffa74bf39dd53c8576c6e0254cd8ec58db230d071fc806`
- Standalone SHA-256: `c02aca5bd7495b133e8607c6778f1e028bc4d7b8c055e48bed111ba2cf1a2c67`
- Signed with DashGrid's own key (not AARE's); future updates must use the same key.

Requires Android 6+ and a car with Android Auto (USB or wireless), or *Settings → Workspaces →
Preview car screen* without a car. No root and no Google Play Services are required.

## Documentation

[README](https://github.com/nchalexz/DashGrid#readme) ·
[media compatibility](https://github.com/nchalexz/DashGrid/blob/main/docs/MEDIA_COMPATIBILITY.md) ·
[architecture](https://github.com/nchalexz/DashGrid/blob/main/ARCHITECTURE.md) ·
[test report](https://github.com/nchalexz/DashGrid/blob/main/TESTING.md) ·
[upstream](https://github.com/nchalexz/DashGrid/blob/main/UPSTREAM.md)

## Known limitations

- **No physical validation** on Huawei/EMUI or in a car; only builds, 869 unit tests and
  JVM-rendered UI mockups were run. The Yandex matrix is a test plan, not a device pass.
- Yandex Music's full library is unavailable while its MediaBrowser service is disabled; the
  Music widget controls playback through the media session but cannot browse My Wave/playlists.
- One live map surface per connection; inactive workspaces show a placeholder. The classic layout
  keeps AARE's app-grid home.
- Car App Library apps that allow-list AARE's signing digest must add DashGrid's.
- This is a fork, not an official AARE release. AARE is GPL-3.0; DashGrid keeps that licence.
