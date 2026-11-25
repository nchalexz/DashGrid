**Swipe. Arrange. Drive.**

DashGrid 1.0.0 is the first release of the independent fork of [AARE](https://github.com/prototux/AARE).
It turns the car's home screen into switchable workspaces with a configurable grid of apps and
widgets, while keeping AARE's Android Auto projection, audio, navigation, music, messages, calls
and assistant.

## Highlights

- Any number of workspaces, switched cyclically by swipe, arrows or page dots.
- Layouts: 100 %, 50/50, 70/30, 30/70, three widgets, four widgets, custom 10 × 6 grid.
- Drag, resize and merge cells; pin a map or music widget to an app.
- Phone editor: create, rename, duplicate, reorder and delete workspaces, add apps/widgets,
  choose the default, preview on the phone.
- Only the visible workspace is composed; app sessions, routes and playback survive switching.
- Own application ID (`io.dashgrid.app`), icon, theme and signing key: installs beside AARE.

## Install

Download `DashGrid-v1.0.0.apk` below and open it, or:

```sh
adb install -r DashGrid-v1.0.0.apk
```

- Package: `io.dashgrid.app`
- Version: 1.0.0 (10000)
- SHA-256 of the APK: `e9f801b38310b186fd9dcdd8d66c56258b754b4db1d3d713c27f920e0c6c657e`
- Signed with DashGrid's own key (not AARE's); future updates must use the same key.

Requires Android 6+ and a car with Android Auto (USB or wireless), or use *Settings →
Workspaces → Preview car screen* without a car. No root and no Google Play Services are required.

## Documentation

[README](https://github.com/nchalexz/DashGrid#readme) ·
[architecture](https://github.com/nchalexz/DashGrid/blob/main/ARCHITECTURE.md) ·
[test report](https://github.com/nchalexz/DashGrid/blob/main/TESTING.md) ·
[upstream](https://github.com/nchalexz/DashGrid/blob/main/UPSTREAM.md)

## Known limitations

- Not validated on a physical car or on Huawei/EMUI; only build, 867 unit tests and JVM-rendered
  UI mockups were run.
- Workspaces are the home of the taskbar and dock layouts; the classic layout keeps AARE's app-grid
  home.
- One live map surface per connection; inactive workspaces show a map placeholder.
- Car App Library apps that allow-list AARE's signing digest must add DashGrid's.
- This is a fork, not an official AARE release. AARE is GPL-3.0; DashGrid keeps that licence.
