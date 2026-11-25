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

## Install — pick one APK

AARE installed? Use the **side-by-side** APK. Without AARE? Use the **standalone** APK. They share
the package, version and key, so they replace each other; the standalone one must not be installed
beside AARE (it defines `android.car.permission.TEMPLATE_RENDERER`, which AARE owns with another
certificate, and Android refuses the duplicate definition).

```sh
adb install -r DashGrid-v1.0.0.apk                 # with AARE
adb install -r DashGrid-standalone-v1.0.0.apk      # without AARE
```

- Package: `io.dashgrid.app`, version 1.0.0 (10000)
- Side-by-side SHA-256: `9fc864609f020da39105d45b37bc9f89ac3d1e147635256efeac32b8bdbe5c1a`
- Standalone SHA-256: `7521eea19fb7a2f3527b27be9ceddc6131b4ec9a162741e153edc540e8a6c57e`
- Signed with DashGrid's own key (not AARE's); future updates must use the same key.

> **Fixed in this build:** the first upload declared the legacy car-host permission itself and EMUI
> refused to install it beside AARE. The side-by-side APK no longer declares it; the standalone APK
> keeps it for phones without AARE.

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
