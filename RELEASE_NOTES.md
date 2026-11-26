**Swipe. Arrange. Drive.**

DashGrid 1.1.0 puts standard **Android App Widgets** on the workspaces, makes the dock's icons
**monochrome** with four tones, and pins **Home** and **App Dash** where the driver expects them.
It is published as the **standalone build only** (AARE must be removed first).

## Android App Widgets

- A standard App Widget can live in a workspace's cells: the provider's own live RemoteViews,
  updating and scrolling, not a screenshot. Add one in **Settings → Workspaces → edit a
  workspace → Android widget**.
- The list is every App Widget installed on the phone — app icon, widget name, minimum size and
  preview. Nothing is filtered by a fixed catalogue.
- The first widget from a provider asks for Android's own permission on the phone, and the
  provider's configuration screen opens there too. **No Shizuku and no root.**
- The view is inflated with the car screen's density; the cells' size in dp is reported back to
  the provider, so a resizable widget reflows instead of being stretched. No tint, no scaling: the
  widget keeps its own design.
- Ids survive restarts and reconnects; the host listens only during a car session; removing the
  item or its workspace releases the id; a removed provider shows a placeholder; several widgets
  and several instances of one provider work; a duplicated workspace doesn't share a bound widget.
- [docs/ANDROID_WIDGETS.md](https://github.com/nchalexz/DashGrid/blob/main/docs/ANDROID_WIDGETS.md)
  has the architecture, instructions and limits.

## Dock icons

- **Monochrome dock icons** (off by default): grayscale of the original icon by its own
  brightness, with shape, logo, detail, transparency and the active-app highlight kept. The app
  grid and the workspaces keep their colours.
- **Four tones**, chosen separately for light and dark colours: White, Grey, Dark grey and Black.
  Defaults: Dark grey in the light theme, White in the dark one. The day/night switch applies the
  matching tone without a restart, and processed icons are cached.
- The settings show a preview of the phone's real icons in every tone.

## Fixed navigation buttons

- **Always show the Home button** and **Always show the App Dash button**, independent and off by
  default. They sit at a fixed place at the driver's end of the rail, dock or classic bar, on
  every workspace, the app grid and inside apps — recent apps can't push them out.
- Actions are unchanged (Home returns to the workspace you left, App Dash opens the app grid), and
  AARE's switching button is hidden only to avoid drawing the same action twice.

## Also in 1.1.0 — the Cockpit

- Car profile *Golf 7 2.0 TDI (CUNA) · DQ381* and a new **Values** page with coolant, boost,
  oil, EGT, rail pressure and the other readings, in the style of the car's instruments.
- VAG identifier probe, UDS values, the compact Cockpit card in a live app pane, and the
  Titillium Web typeface.

## Screens

| Dock tones (dark) | Dock tones (light) |
|:--:|:--:|
| ![Four dock tones on the dark theme](https://raw.githubusercontent.com/nchalexz/DashGrid/main/docs/screenshots/dock-tones-dark.png) | ![Four dock tones on the light theme](https://raw.githubusercontent.com/nchalexz/DashGrid/main/docs/screenshots/dock-tones-light.png) |
| **Monochrome dock** | **Home and App Dash fixed** |
| ![Monochrome dock icons](https://raw.githubusercontent.com/nchalexz/DashGrid/main/docs/screenshots/dock-monochrome-dark.png) | ![Fixed Home and App Dash buttons](https://raw.githubusercontent.com/nchalexz/DashGrid/main/docs/screenshots/pinned-buttons.png) |

These are Paparazzi renders on the JVM with DashGrid's own icons; a real car screen with third-party
icons can differ.

## Install

This release attaches **only** `DashGrid-standalone-v1.1.0.apk`. It defines
`android.car.permission.TEMPLATE_RENDERER`, so AARE must be uninstalled first.

```sh
adb uninstall net.prototux.aare
adb install -r DashGrid-standalone-v1.1.0.apk
```

- Package: `io.dashgrid.app`, version 1.1.0 (10100)
- SHA-256: `f8ac4d91fe220fb0c2f01ac9cb810f5264086d2995e49c396636025c2310778a`
- Signed with DashGrid's own key (`CN=DashGrid`); updates must use the same key.

## Verification

- 1 162 unit tests pass (`app` 918, `aap` 219, `headunit` 25); the release passes `lintVital`.
- **No car, phone or emulator was available for this build.** The widget host, the tones on a real
  head unit, third-party icons and EMUI 14+ are implemented and compile, but are **not** verified
  on a device. The exact test checklist and the open points are in
  [TESTING.md](https://github.com/nchalexz/DashGrid/blob/main/TESTING.md). Please do not treat
  this release as confirmed compatible with EMUI or any particular head unit.

## Documentation

[README](https://github.com/nchalexz/DashGrid#readme) ·
[architecture](https://github.com/nchalexz/DashGrid/blob/main/ARCHITECTURE.md) ·
[Android widgets](https://github.com/nchalexz/DashGrid/blob/main/docs/ANDROID_WIDGETS.md) ·
[test report](https://github.com/nchalexz/DashGrid/blob/main/TESTING.md) ·
[theme-file proposal](https://github.com/nchalexz/DashGrid/blob/main/docs/EXTERNAL_THEMES.md)

## Notes

- One live map surface per connection; inactive workspaces show a placeholder.
- One live view per bound App Widget id; the phone preview shows a placeholder while a real car
  screen is connected.
- A widget button whose action is an activity `PendingIntent` may open on the phone or be refused
  by Android's background-activity rules; broadcast/service actions are unaffected.
- AARE is GPL-3.0; DashGrid keeps that licence. This is a fork, not an official AARE release.
