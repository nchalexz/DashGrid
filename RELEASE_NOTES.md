**Swipe. Arrange. Drive.**

DashGrid 1.0.7 is a small update of the independent fork of
[AARE](https://github.com/prototux/AARE), published as the **standalone build only**.

## New

- **Hide the Android Auto version of an enabled phone app** — in *Settings → Apps*, under
  **Apps on the car screen (advanced)**, right above the phone apps. When an app enabled there
  also appears in the car's app grid as an Android Auto app, the grid lists only the phone
  version. Off by default, so existing installations don't change.
- Only the app grid changes: shortcuts already placed on a workspace keep working, the rail's
  recents can still open the Android Auto app, and the phone's own app list still shows both.
- The option is exported and imported with the other settings, and has a unit test.

## Also in 1.0.7

- **Home from the app grid** opens the main (default) workspace; apps started from a workspace
  return to it (1.0.6).
- The bar's **assistant button** can be hidden in *Settings → Voice assistant*; the steering
  wheel's voice key and the assistant's own stop button keep working (1.0.6).
- Workspaces, live app panes, per-widget app pinning, dot-only pager, 60/40 split and
  tap-to-open live apps, the session-only Music widget, and the full Russian UI.

## Install

This release attaches **only** `DashGrid-standalone-v1.0.7.apk`. It defines
`android.car.permission.TEMPLATE_RENDERER`, so AARE must be uninstalled first.

```sh
adb uninstall net.prototux.aare
adb install -r DashGrid-standalone-v1.0.7.apk
```

- Package: `io.dashgrid.app`, version 1.0.7 (10007)
- SHA-256: `cd92208248335aeff8ab3da243b2cdc982c8450e79837fc6512e3d0640226731`
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
