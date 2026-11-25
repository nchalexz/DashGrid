**Swipe. Arrange. Drive.**

DashGrid 1.0.4 is a small update of the independent fork of
[AARE](https://github.com/prototux/AARE), published as the **standalone build only**.

## New

- **Close apps when hidden** — a per-workspace option in *Settings → Workspaces*. When the page is
  left, DashGrid releases that workspace's car app sessions and surfaces and pauses its music.
  Off by default: routes, playback and app state stay alive across page switches.
- The audio and navigation apps of a workspace that another workspace also uses are not closed.
  Android may keep the phone app's process alive; DashGrid stops the car-side session and audio it
  owns.

## Also in 1.0.4

- Live app panes, per-widget app pinning, dot-only pager with optional name.
- Music widget with session-only players, artwork, progress, seek, play/pause and skip.
- Full Russian UI; short Russian summary in the README.

## Install

This release attaches **only** `DashGrid-standalone-v1.0.4.apk`. It defines
`android.car.permission.TEMPLATE_RENDERER`, so AARE must be uninstalled first.

```sh
adb uninstall net.prototux.aare
adb install -r DashGrid-standalone-v1.0.4.apk
```

- Package: `io.dashgrid.app`, version 1.0.4 (10004)
- SHA-256: `8cf8887082c804106eeeaef3e88e5def383df12bf7c4ebd95e0b72563889f417`
- Signed with DashGrid's own key (`CN=DashGrid`); updates must use the same key.

## Documentation

[README](https://github.com/nchalexz/DashGrid#readme) ·
[architecture](https://github.com/nchalexz/DashGrid/blob/main/ARCHITECTURE.md) ·
[design](https://github.com/nchalexz/DashGrid/blob/main/DESIGN.md) ·
[test report](https://github.com/nchalexz/DashGrid/blob/main/TESTING.md) ·
[upstream](https://github.com/nchalexz/DashGrid/blob/main/UPSTREAM.md)

## Notes

- One live map surface per connection; inactive workspaces show a placeholder.
- Car App Library apps that allow-list AARE's signing digest must add DashGrid's.
- AARE is GPL-3.0; DashGrid keeps that licence. This is a fork, not an official AARE release.
