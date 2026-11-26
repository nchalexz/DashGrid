<p align="center">
  <img src="https://raw.githubusercontent.com/nchalexz/DashGrid/main/docs/screenshots/social-preview.png" width="100%" alt="DashGrid 1.1.1 — Swipe. Arrange. Drive.">
</p>

<h1 align="center">DashGrid 1.1.1</h1>

<p align="center">
  <strong>Swipe. Arrange. Drive.</strong><br>
  26 November 2026
</p>

DashGrid 1.1.1 is a design patch: a new **minimalist launcher icon** and a README that names the
four things the app gives you. There are **no functional changes** — workspaces, live panes,
Android app widgets, the dock and the Cockpit are the same as 1.1.0.

In this release: [Icon](#icon) · [README](#readme) · [Install](#install) ·
[Verification](#verification)

> [!IMPORTANT]
> This release is **standalone only**. The APK defines `android.car.permission.TEMPLATE_RENDERER`
> itself, so **AARE must be uninstalled first**. Removing AARE also removes its settings.

<p align="center">
  <a href="https://github.com/nchalexz/DashGrid/releases/download/dashgrid-v1.1.1/DashGrid-standalone-v1.1.1.apk"><img alt="Download DashGrid 1.1.1" src="https://img.shields.io/badge/Download_APK-v1.1.1-36D6C3?style=for-the-badge&labelColor=101418"></a>
</p>

## Icon

<p align="center">
  <img src="https://raw.githubusercontent.com/nchalexz/DashGrid/main/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="128" alt="DashGrid 1.1.1 launcher icon">
</p>

The new mark is one workspace in a 70/30 layout: a tall turquoise pane on the left and two muted
panes stacked on the right. **Three shapes, no frame, no paging dots and no side pages.** It keeps
the graphite-and-turquoise identity and stays readable at 48 px.

- The adaptive foreground and monochrome layers are updated for Android's themed icons.
- Every legacy square and round PNG (48–192 px) is regenerated.
- The 512 px store icon, the social preview and [DESIGN.md](https://github.com/nchalexz/DashGrid/blob/main/DESIGN.md)
  describe the same mark.
- The APK's `mipmap-xxxhdpi/ic_launcher.png` is byte-identical to the source file.

## README

The README now follows the same standard as the other projects: a full-width banner, six
flat-square badges, and the same section order from **What this is** to **License**. Its tagline
states the four core features:

> As many screens as you want, a 10 × 6 grid, live apps in their panes and any Android app widget.

That is the whole feature summary: unlimited workspaces, a 10 × 6 grid, live apps inside their
panes, and standard Android app widgets hosted in the cells. No functional change was needed for
any of them.

## Install

Download
[DashGrid-standalone-v1.1.1.apk](https://github.com/nchalexz/DashGrid/releases/download/dashgrid-v1.1.1/DashGrid-standalone-v1.1.1.apk),
then:

```sh
adb uninstall net.prototux.aare   # only if AARE is installed
adb install -r DashGrid-standalone-v1.1.1.apk
```

| | |
|:--|:--|
| **Package** | `io.dashgrid.app` |
| **Version** | 1.1.1 (version code 10101) |
| **Minimum Android** | 6.0 (API 23) |
| **Size** | 19 MB |
| **SHA-256** | `d9e5d2a09e0c2ab1b2c718af0b3588cad72fa1960ac1e8285b34304304eacd9d` |
| **Signing key** | `CN=DashGrid` |
| **Certificate SHA-256** | `4c464d2c35c723c2bc4ca30dafa84c3dbdf16473d61ec9961e37dc6e973420a5` |

```sh
shasum -a 256 DashGrid-standalone-v1.1.1.apk
# d9e5d2a09e0c2ab1b2c718af0b3588cad72fa1960ac1e8285b34304304eacd9d
```

**Upgrading from 1.1.0:** install over the old version; the package name, application ID, signing
key and version code order are unchanged, so settings and workspaces stay in place.

**Coming from AARE:** uninstall AARE first. Its settings are removed with it and do not carry over.

## Verification

- **1,162 unit tests pass**: `app` 918, `aap` 219, `headunit` 25.
- The release build compiles and passes `lintVital`.
- The APK is signed with `CN=DashGrid`; the certificate fingerprint is above.
- The icon inside the APK is byte-identical to
  `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png`.
- **No car, phone or emulator was available for this build.** The 1.1.0 hardware caveat still
  applies: the widget host, dock tones and third-party icons are not confirmed on a real screen.
  See [TESTING.md](https://github.com/nchalexz/DashGrid/blob/main/TESTING.md).

## Known limitations

1.1.1 changes no runtime code, so the 1.1.0 limitations are unchanged:

- One live map surface per connection; inactive workspaces show a placeholder.
- One live view per bound Android widget id; the phone preview shows a placeholder while a real car
  session is connected.
- A widget button whose action is an activity `PendingIntent` may open on the phone or be refused by
  Android's background-activity rules.
- Third-party dock icon tones follow each icon's own artwork, so they can read differently from app
  to app.
- The published build does not check for updates itself; new releases are installed by hand.

The complete list is in the
[README](https://github.com/nchalexz/DashGrid#limitations).

## Documentation

- [README](https://github.com/nchalexz/DashGrid#readme)
- [Architecture](https://github.com/nchalexz/DashGrid/blob/main/ARCHITECTURE.md)
- [Android app widgets](https://github.com/nchalexz/DashGrid/blob/main/docs/ANDROID_WIDGETS.md)
- [Test report](https://github.com/nchalexz/DashGrid/blob/main/TESTING.md)
- [Full changelog](https://github.com/nchalexz/DashGrid/blob/main/CHANGELOG.md)
- [All changes since 1.1.0](https://github.com/nchalexz/DashGrid/compare/dashgrid-v1.1.0...dashgrid-v1.1.1)

## Thank you

DashGrid is a fork of [AARE](https://github.com/prototux/AARE). The connection, transport, audio
and app hosting are AARE's work; the workspaces, editor, dock, Cockpit and this icon are DashGrid's.

AARE is GPL-3.0-or-later; DashGrid keeps that licence. This is a fork, not an official AARE
release.
