**Swipe. Arrange. Drive.**

DashGrid 1.0.5 is a small update of the independent fork of
[AARE](https://github.com/prototux/AARE), published as the **standalone build only**.

## New

- **60 / 40 layout** — a fourth workspace split beside 100 %, 50/50, 70/30 and 30/70. Side by side
  it is exactly 60/40 (6+4 cells); top and bottom rounds to 4+2 rows on the 6-row grid, as 70/30
  already does.
- **Tap a live app to open it** — a per-workspace option in *Settings → Workspaces*. A tap on the
  free space of a live pane opens that app full screen, as tapping a widget card does. The pane's
  own buttons, lists and map keep working. Off by default, so existing workspaces behave as before.
- Both settings are stored in the workspace JSON and covered by unit tests.

## Also in 1.0.5

- Workspaces, live app panes, per-widget app pinning, dot-only pager with optional name.
- Music widget with session-only players, artwork, progress, seek, play/pause and skip.
- Full Russian UI; short Russian summary in the README.

## Install

This release attaches **only** `DashGrid-standalone-v1.0.5.apk`. It defines
`android.car.permission.TEMPLATE_RENDERER`, so AARE must be uninstalled first.

```sh
adb uninstall net.prototux.aare
adb install -r DashGrid-standalone-v1.0.5.apk
```

- Package: `io.dashgrid.app`, version 1.0.5 (10005)
- SHA-256: `4ea4e7ea2c6654984090f24f12b308b46a92b9c11189918a3a109f6969997220`
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
