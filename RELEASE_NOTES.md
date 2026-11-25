**Swipe. Arrange. Drive.**

DashGrid 1.0.6 is a small update of the independent fork of
[AARE](https://github.com/prototux/AARE), published as the **standalone build only**.

## New

- **Show the assistant button** — a switch in *Settings → Voice assistant*. Turned off, the car
  screen's bar hides its microphone in all three layouts (taskbar, dock and classic). The
  steering wheel's voice key, voice commands and the assistant bar's own stop button keep
  working, so the assistant can always be stopped.
- The choice is exported and imported with the other DashGrid settings.

## Changed

- **Home from the app grid** now opens the **main (default) workspace**. The grid belongs to no
  workspace, so an app started there returns to the main one rather than to whichever workspace
  happened to be showing before. An app opened from a workspace still returns to that workspace.
  Back and an app that closes itself are unchanged.

## Also in 1.0.6

- Workspaces, live app panes, per-widget app pinning, dot-only pager with optional name.
- 60/40 split and tap-to-open live apps (1.0.5).
- Music widget with session-only players, artwork, progress, seek, play/pause and skip.
- Full Russian UI; short Russian summary in the README.

## Install

This release attaches **only** `DashGrid-standalone-v1.0.6.apk`. It defines
`android.car.permission.TEMPLATE_RENDERER`, so AARE must be uninstalled first.

```sh
adb uninstall net.prototux.aare
adb install -r DashGrid-standalone-v1.0.6.apk
```

- Package: `io.dashgrid.app`, version 1.0.6 (10006)
- SHA-256: `61df33afdb4ce8e2730798b613dfdde86da32f82736ef4f55b2385a09c46974d`
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
