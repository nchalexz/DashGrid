<p align="center">
  <img src="https://raw.githubusercontent.com/nchalexz/DashGrid/main/docs/screenshots/social-preview.png" width="100%" alt="DashGrid 1.3.2 — a workspace home screen for Android Auto">
</p>

<h1 align="center">DashGrid 1.3.2</h1>

<p align="center">
  <strong>STABLE — VPM 0.6.2, five tiles in one row, and stale values that stay numbers.</strong><br>
  27 November 2026
</p>

> [!WARNING]
> **Checked on a real phone and a car, not on a head unit.** 1.3.2 was run on a Huawei ALT-L29 with
> VPM 0.6.2 and a Bluetooth ELM327 v1.5, in demo and live: gauges, all six tabs, five tiles in one
> row, the DEMO/LIVE header and the live sensor values were exercised. A real head unit, EMUI's car
> screen and a real Android Auto session were **not** available. The pictures below are phone
> previews, not head-unit captures. See [Limits](#limits).

1.3.2 aligns the native Car cockpit with **VAG Performance Monitor 0.6.2** and fixes the things a
real car exposed: a dead oil PID hiding a live VAG temperature, five tiles wrapping into two rows,
and a long LIVE status covering the coolant dial.

In this release: [What changed](#what-changed) · [Install](#install) · [Verification](#verification)
· [Limits](#limits)

## What changed

- **VPM 0.6.2 composition.** Gauges: coolant, **absolute boost** (0…2.5 bar) and oil. Tiles: DSG oil,
  intake air, exhaust gas, voltage and rail pressure. Speed, revs, gear, fuel use and fuel level
  leave the main screen and stay in the category grid, exactly as in VPM.
- **Stale is a number.** The cockpit reads VPM's `status` field (`valid` / `stale` / `never_read`). A
  stale value keeps its number, dimmed with a stale dot and a faded needle; only `never_read` or a
  missing value is a dash. An older VPM without `status` is derived from `stale`, and an unknown
  status never crashes the parser.
- **Oil on a diesel.** When `engine.oil_c` never answers, the gauge now prefers a live
  `vag.7e0.11be.oil_temp` / `vag.714.202f.oil_temp` over the dead primary. This is what brought the
  oil gauge back on the test car.
- **Five tiles in one row.** From 480 dp wide the five tiles no longer wrap at 500–700 dp. Their
  height is 26 % of the screen (62–110 dp), so the gauges keep the rest; on the test phone the
  change turned two cramped rows into one row and visibly larger dials.
- **A calmer header.** The DEMO/LIVE pill and the page dots moved into a thin header, so a long
  `Bluetooth OBDII · ELM327 v1.5 · 14.3V` status cannot cover a gauge. The gauge pictograms are
  drawn VAG/Porsche-style thermometer, oil can and turbo instead of generic Material icons.
- **Faster follow.** The visible cockpit polls VPM every 100 ms and the needle settles in 90 ms.
  VPM 0.6.2 still refreshes MAP at 5 Hz, so the adapter remains the limit.
- **Delete stays reachable.** In the workspace editor the red **Remove** action for a live pane was
  clipped when it shared a row with **Change app** and **Move to**; it now has its own row.

<p align="center">
  <img src="https://raw.githubusercontent.com/nchalexz/DashGrid/main/docs/screenshots/vpm-cockpit-main-1920x720.png" width="100%" alt="The Car cockpit at 1920 by 720: coolant, absolute boost and oil gauges over five tiles in one row">
</p>
<p align="center"><sub>The main screen at 1920 × 720: three gauges and five tiles in one row, VPM's demo values. A phone preview, not a head-unit capture.</sub></p>

<p align="center">
  <img src="https://raw.githubusercontent.com/nchalexz/DashGrid/main/docs/screenshots/vpm-cockpit-categories-1920x720.png" width="100%" alt="The category screen: six tabs and two rows of VPM values">
</p>
<p align="center"><sub>The category screen at 1920 × 720: the same gauges smaller, six tabs and a two-row grid.</sub></p>

## Install

One APK: the **standalone** build. It defines `android.car.permission.TEMPLATE_RENDERER` itself, so
AARE must be uninstalled first. Requirements: Android 6.0+, USB or wireless Android Auto, no root,
no Google Mobile Services. The cockpit needs **VPM 0.6.2** on the same phone.

| File | Size | SHA-256 |
|---|---:|---|
| `DashGrid-standalone-v1.3.2.apk` | 19 MB | `29168d3344e84c5d22f60440f6aef96980edbf84e42e03aec5464cf115c6f1cd` |

```sh
adb uninstall net.prototux.aare   # only if AARE is installed
adb install -r DashGrid-standalone-v1.3.2.apk
```

Signed with `CN=DashGrid`, certificate SHA-256
`4c464d2c35c723c2bc4ca30dafa84c3dbdf16473d61ec9961e37dc6e973420a5`. Updates use the same key and
install over the old build.

## Verification

| Check | How | Result |
|---|---|---|
| Build | `./gradlew :aap:test :app:testDebugUnitTest :headunit:test :app:dashgridStandaloneApk` | **yes** |
| Tests | 962 app + 219 `aap` + 25 `headunit` | **1,206, all green** |
| Lint | `lintVital` on the release build | **yes** |
| Real phone, demo | Huawei ALT-L29, VPM 0.6.2 | **yes** |
| Real phone, live | Bluetooth OBDII / ELM327 v1.5, car | **yes** |
| Live main screen | coolant 84 °C, boost 0.91 bar, oil 90 °C, DSG 80 °C, intake 11 °C, EGT 185 °C, 14.4 V, rail 319 bar | **yes** |
| All six tabs | Engine, Temperatures, Fuel, Gearbox, Turbo, Diagnostics | **yes** |
| Five tiles in one row | 2504 × 1080 phone preview and 1280 × 480 render | **yes** |
| Real head unit / EMUI car screen | — | **no** |
| 1920 × 720 and 1280 × 480 | emulator renders | **yes** |
| VPM 0.4.1 / no VPM cards | earlier emulator run; unit-tested | **yes** |

Emulator or phone-preview renders are not compatibility proof. The exact checklist is in
[docs/VPM.md](docs/VPM.md) and [TESTING.md](TESTING.md).

## Limits

- **No head unit, no real Android Auto session.** 1.3.2 was run on a phone and in a car, but not on
  a head unit or EMUI's car screen. Widget hosting, dock tones and third-party icons remain
  unconfirmed there.
- **VPM owns the adapter.** DashGrid never opens OBD. The cockpit is a reader; it needs VPM 0.6.2
  running and connected. With VPM 0.4.1 or no VPM the Car workspace shows **Update VPM** or
  **Install**.
- **Boost is adapter-bound.** VPM 0.6.2 polls MAP at 5 Hz; the 100 ms bridge poll cannot make the
  car faster than the ELM327 answers.
- **AARE must go first.** The standalone APK cannot coexist with AARE; the side-by-side build is
  local only.
- **No in-app update check.** New releases are installed by hand.

## Documentation

- [README](https://github.com/nchalexz/DashGrid#readme) — what DashGrid is, install and screens.
- [docs/VPM.md](https://github.com/nchalexz/DashGrid/blob/main/docs/VPM.md) — the cockpit, contract,
  states and the live check.
- [CHANGELOG.md](https://github.com/nchalexz/DashGrid/blob/main/CHANGELOG.md) — every change.
- [TESTING.md](https://github.com/nchalexz/DashGrid/blob/main/TESTING.md) — tests and the device
  checklist.
