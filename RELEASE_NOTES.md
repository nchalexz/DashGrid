**Swipe. Arrange. Drive.**

DashGrid 1.0.1 is an update of the independent fork of
[AARE](https://github.com/prototux/AARE). It adds live app panes, the Universal Media Bridge for
apps whose MediaBrowser is closed (notably Yandex Music on some builds), full Russian coverage for
the new screens and the two-APK packaging from 1.0.0.

## Highlights

- **Live app panes**: an app can be rendered into its own cells — Car App templates, a map, a
  media screen, a launched phone app — instead of a shortcut icon. “Live app” and “Shortcut” are
  separate choices in the workspace editor.
- **Pinned widgets**: a map or music widget can be pinned to a specific app per workspace, instead
  of following whichever app was opened last.
- **Universal Media Bridge**: a new `media_bridge` widget that uses the system media session
  (`MediaSessionManager` / `MediaController`), with artwork, title/artist, progress + seeking,
  play/pause, next/previous, custom session actions, a player picker (active sessions and installed
  players) and a button to start the chosen app. It keeps working while switching workspaces and
  needs neither root nor GMS; notification access is required.
- **Yandex Music**: the official APK inspected for this release declares its
  `MusicBrowserService` with `android:enabled="false"`, so a third-party host cannot use its full
  library (My Wave, playlists, search) without root/Shizuku or modifying the app. The bridge is the
  supported fallback; the exact diagnosis, APK evidence and a test plan are in
  [docs/MEDIA_COMPATIBILITY.md](https://github.com/nchalexz/DashGrid/blob/main/docs/MEDIA_COMPATIBILITY.md).
- **Russian**: all previously missing strings in the audio, root, voice-reply, OBD and theme
  screens are translated, and the new workspace/media bridge strings are in both languages. A
  Russian phone editor and car-screen mockup were rendered to check Cyrillic and long labels.
- **Two APKs**: use the side-by-side file with AARE installed, the standalone file on a phone
  without AARE. They share package, version and key and replace each other.

## Install

```sh
adb install -r DashGrid-v1.0.1.apk                 # with AARE installed
adb install -r DashGrid-standalone-v1.0.1.apk      # without AARE
```

- Package: `io.dashgrid.app`, version 1.0.1 (10001)
- Side-by-side SHA-256: `7f87b613cb1082a43a976652c949c82959fc8fdbe4b5e33155ed0fd2a67d48a8`
- Standalone SHA-256: `9d327be358c5354486de242150decf0e1e524eb6863078b70faa676a4352dd7a`
- Signed with DashGrid's own key (not AARE's); future updates must use the same key.

Requires Android 6+ and a car with Android Auto (USB or wireless), or use *Settings →
Workspaces → Preview car screen* without a car. No root and no Google Play Services are required.

## Documentation

[README](https://github.com/nchalexz/DashGrid#readme) ·
[media compatibility](https://github.com/nchalexz/DashGrid/blob/main/docs/MEDIA_COMPATIBILITY.md) ·
[architecture](https://github.com/nchalexz/DashGrid/blob/main/ARCHITECTURE.md) ·
[test report](https://github.com/nchalexz/DashGrid/blob/main/TESTING.md) ·
[upstream](https://github.com/nchalexz/DashGrid/blob/main/UPSTREAM.md)

## Known limitations

- **No physical validation** on Huawei/EMUI or in a car; only builds, 869 unit tests and
  JVM-rendered UI mockups were run. The media compatibility matrix is a test plan, not a device pass.
- Yandex Music's full library is unavailable while its MediaBrowser service is disabled; the
  bridge controls playback through the media session but cannot browse My Wave/playlists/search.
- One live map surface per connection; inactive workspaces show a placeholder. The classic layout
  keeps AARE's app-grid home.
- Car App Library apps that allow-list AARE's signing digest must add DashGrid's.
- This is a fork, not an official AARE release. AARE is GPL-3.0; DashGrid keeps that licence.
