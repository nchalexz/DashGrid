<p align="center">
  <img src="https://raw.githubusercontent.com/nchalexz/DashGrid/main/docs/screenshots/social-preview.png" width="100%" alt="DashGrid 1.3.0 — the VPM cockpit with live telemetry">
</p>

<h1 align="center">DashGrid 1.3.0</h1>

<p align="center">
  <strong>STABLE — the VPM cockpit now reads VPM's read-only telemetry.</strong><br>
  27 November 2026
</p>

> [!WARNING]
> **No real head unit, phone or car was used.** 1.3.0 is verified on an Android 15 emulator only.
> The VPM bridge was checked with a real VPM 0.5.0 over the normal permission and ContentProvider, but
> there was no ELM327 adapter and no vehicle: the cockpit shows VPM's demo values, not live sensor
> readings. Emulator captures are not compatibility proof. See [Limits](#limits).

DashGrid 1.3.0 is the first stable release of the native **VAG Performance Monitor** cockpit. The
preinstalled **Car** workspace reads the snapshot VPM already holds, so the three gauges, five tiles
and the category grid show the numbers VPM's own cockpit shows — with **DEMO** or **LIVE** stated
honestly. VPM remains the only owner of the ELM327 adapter and the VAG UDS session; DashGrid opens no
OBD connection and needs no root or Shizuku.

In this release: [Cockpit](#cockpit) · [Live data](#live-data-from-vpm) · [Screen sizes](#screen-sizes) ·
[Install](#install) · [Verification](#verification) · [Limits](#limits)

## Cockpit

<p align="center">
  <img src="https://raw.githubusercontent.com/nchalexz/DashGrid/main/docs/screenshots/vpm-cockpit-main-1920x720.png" width="100%" alt="Three gauges and five tiles with VPM's DEMO values at 1920 by 720">
</p>
<p align="center"><sub>1920 × 720: coolant, boost and oil over speed, revs, gear, fuel use and fuel,
fed by VPM 0.5.0's read-only snapshot. An emulator capture of the running app — not a head-unit
screenshot.</sub></p>

<p align="center">
  <img src="https://raw.githubusercontent.com/nchalexz/DashGrid/main/docs/screenshots/vpm-cockpit-categories-1920x720.png" width="100%" alt="The category screen with six tabs and a two-row grid">
</p>
<p align="center"><sub>The extended screen: the same gauges smaller, the six category tabs and the
adaptive grid. A gauge tap opens the tab of its category.</sub></p>

- three gauges with VPM's geometry — 0.44R disc, 0.78R value, 0.54R unit, 0.73R pictogram, 1.15R
  name, 60 ticks, 135°/270° scale, the bright arc and the red zone;
- five tiles and the Engine / Temperatures / Fuel / Gearbox / Turbo / Diagnostics grid, in VPM's own
  order;
- VPM's night and day tokens, Titillium Web and its comma number style;
- the **DEMO** pill stays while VPM is in demo, so sample values are never mistaken for a car;
- the one deliberate deviation: VPM's own service row is not duplicated, so the gauges get its
  height.

## Live data from VPM

DashGrid makes one read-only call and draws the result:

| Item | Value |
|---|---|
| Authority | `io.github.nchalexz.aaperfmon.telemetry` |
| Permission | `io.github.nchalexz.aaperfmon.permission.READ_TELEMETRY` (`normal`) |
| Call | `call("snapshot", null, null)` |
| Contract version | `1` |
| Provider in | VPM **0.5.0** and newer |

The snapshot carries the link state, VPM's status line, `updatedAt` and up to 128 signals: id, label,
formatted value, unit, numeric display value, stale flag, warning flag and category. Values arrive in
display units (boost in bar), so DashGrid never re-derives VPM's factors. Only the visible page polls
(500 ms), and a bridge problem is logged once, not five times a second.

- **VPM 0.5.0 or newer** — the cockpit draws VPM's values with a **DEMO** or **LIVE** pill.
- **VPM 0.4.1** — a calm “this version doesn't share telemetry” card with **Update VPM** and **Open on
  the phone**.
- **VPM not installed** — the **Install** card.
- **No data / stale** — “—”, never a zero; the needle goes out.
- **Permission** — Android grants the normal permission when DashGrid is installed after VPM 0.5.0.
  If DashGrid was installed earlier, update or reinstall it once.

VPM stays the only owner of the adapter. DashGrid polls no PID, starts no service and opens no second
ELM327 connection. The built-in OBD `car` widget and the OBD app are untouched.

## Screen sizes

<p align="center">
  <img src="https://raw.githubusercontent.com/nchalexz/DashGrid/main/docs/screenshots/vpm-cockpit-main-1280x480.png" width="100%" alt="The same cockpit at 1280 by 480">
</p>
<p align="center"><sub>1280 × 480: the gauges shrink, the five tiles stay on one row, and the grid
keeps both rows of labels, values and units whole.</sub></p>

At 1280 × 480 the category grid used to clip the second row. 1.3.0 gives the grid the larger half on
short screens and lets the tiles shrink their text; both rows are now fully readable.

## Install

Download
[DashGrid-standalone-v1.3.0.apk](https://github.com/nchalexz/DashGrid/releases/download/dashgrid-v1.3.0/DashGrid-standalone-v1.3.0.apk),
then install it with `adb install -r` or by opening the file.

```text
19,707,769 bytes
SHA-256  7a05a80d8a5d0dd2e6ea0b4511d77a6aff5d1b017fb4cd09501481e2248cb456  DashGrid-standalone-v1.3.0.apk
certificate SHA-256: 4c464d2c35c723c2bc4ca30dafa84c3dbdf16473d61ec9961e37dc6e973420a5
```

> [!IMPORTANT]
> This release is **standalone only**. The APK defines `android.car.permission.TEMPLATE_RENDERER`
> itself, so **AARE must be uninstalled first**. That also removes AARE's settings. DashGrid updates
> use the same `CN=DashGrid` key and install over the old build.

Requirements: Android 6.0 or newer, USB or wireless Android Auto, no Google Mobile Services, and VPM
0.5.0+ for the cockpit to show values. **No new runtime permission is requested.** The cockpit
declares VPM's normal read-only permission; it is inert until VPM is installed.

## Verification

- 1,191 JVM tests (947 app, 219 `aap`, 25 `headunit`), 0 failures; `lintVital` on the standalone
  release.
- Fresh install → Home + Car, the Car workspace fills the page.
- With VPM 0.5.0 on Android 15: DEMO pill, values from the provider, `updatedAt` changing between
  calls, both cockpit screens, swipe, tabs and a gauge tap opening its category, leave/return to Car.
- `content call` returned `version=1`, 128 signals and a fresh `updatedAt`; `dumpsys package` showed
  `READ_TELEMETRY: granted=true`.
- With VPM 0.4.1: the **Update VPM** card, no crash. With VPM uninstalled: the **Install** card.
- 1920 × 720 and 1280 × 480, RU and EN, day and night tokens; no clipped values or tabs.
- No OBD/ELM327 attempt and no `SecurityException` in `adb logcat` while the cockpit was open; no
  root and no Shizuku used.
- Not verified: a real ELM327, a car, a real head unit, EMUI 14+, wireless Android Auto.

## Limits

- **No hardware verification.** No real head unit, car or ELM327 was available; demo values came from
  VPM 0.5.0. Live sensor readings and the adapter path are unverified.
- **VPM 0.5.0+ is required for values.** On 0.4.1 the cockpit explains the missing bridge instead of
  drawing numbers.
- **Head-unit quirks remain AARE's.** `vehicles.json` still handles cars that need a different
  protocol.
- **One live map surface and one Android widget view per bound id**, as before.
- **No in-app update check.** New releases are installed by hand.

## Documentation

- [VPM cockpit](https://github.com/nchalexz/DashGrid/blob/main/docs/VPM.md) — screens, contract,
  states, checks and files.
- [VPM bridge contract](https://github.com/nchalexz/DashGrid/blob/main/docs/VPM_BRIDGE_PROMPT.md) —
  the provider VPM implements.
- [README](https://github.com/nchalexz/DashGrid/blob/main/README.md) ·
  [CHANGELOG](https://github.com/nchalexz/DashGrid/blob/main/CHANGELOG.md) ·
  [TESTING](https://github.com/nchalexz/DashGrid/blob/main/TESTING.md).
