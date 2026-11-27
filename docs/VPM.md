# VAG Performance Monitor cockpit

DashGrid can preinstall a **Car** workspace that shows **VAG Performance Monitor** (VPM,
`io.github.nchalexz.aaperfmon`) as a native cockpit: DashGrid draws the three gauges, the five tiles
and the category grid itself, and reads the values VPM already holds. VPM remains the only owner of
the ELM327 adapter and the VAG UDS session; DashGrid opens no OBD connection and subscribes to no
PID.

This is not a copy of VPM's window and not a second app session. It is DashGrid's own Kotlin/Compose
module with VPM 0.6.2's composition (`Gauge.kt`, `DashboardScreens.kt`, `MetricRegistry.kt`).

## What the workspace contains

- A workspace with the fixed id `workspace-car`, named **Car** (`Автомобиль` in Russian), added once
  to a configuration written before version 2. Its version marker is why deleting the workspace does
  not bring it back. Existing names, items, the default and the last-shown workspace are untouched.
- One built-in widget, id `vpm`, across the whole 10 × 6 grid. The editor offers it as the cockpit
  choice and can add it to another workspace. A saved workspace that still holds AARe's old `car`
  (OBD cockpit) widget gets `vpm` in the same cells when the config is loaded, so the car screen and
  the phone preview show the same cockpit.
- No app shortcut, no Shizuku, no root: the cockpit reads a normal read-only ContentProvider, so the
  one pane that used to need a privileged launcher now needs none.

## The screens

<p align="center">
  <img src="screenshots/vpm-cockpit-main-1920x720.png" alt="The Car workspace cockpit at 1920 by 720: three gauges and five tiles with DEMO data" width="100%">
</p>
<p align="center"><sub>The main screen at 1920 × 720, drawn by DashGrid on an emulator from VPM's own
mock values. It is a render of the running app, not a head-unit capture, and not proof of
compatibility.</sub></p>

<p align="center">
  <img src="screenshots/vpm-cockpit-categories-1920x720.png" alt="The category screen: the same gauges smaller, six category tabs and a two-row grid" width="100%">
</p>
<p align="center"><sub>The category screen at 1920 × 720. Tapping a gauge opens the tab of its
category; the tabs and the grid are VPM's Engine / Temperatures / Fuel / Gearbox / Turbo /
Diagnostics.</sub></p>

<p align="center">
  <img src="screenshots/vpm-cockpit-main-1280x480.png" alt="The same cockpit at 1280 by 480: gauges, five tiles, DEMO pill" width="100%">
</p>
<p align="center"><sub>The same composition at 1280 × 480: the gauges shrink, the five tiles stay on
one row.</sub></p>

<p align="center">
  <img src="screenshots/vpm-car-workspace-no-bridge-1920x720.png" alt="The Car workspace with the calm card saying VPM is installed but does not share telemetry" width="100%">
</p>
<p align="center"><sub>What a phone with VPM 0.4.1 shows: the workspace is there, the card explains
that this VPM has no bridge, and offers **Update VPM** and **Open on the phone**. The rail, dock and
workspace bar are DashGrid's; the card fills the page.</sub></p>

The one deviation from VPM's screenshot that is deliberate: VPM's own service row (catalog,
settings, close) is not duplicated, so the gauges get the height that row would take. The **DEMO**
pill stays, so demo values are never mistaken for a car.

## Geometry and tokens

The gauge follows VPM 0.4.1's geometry (its `Gauge.kt`):

| Element | Position / size |
|---|---|
| Centre and value | X = 0, Y = 0 |
| Outer radius | R |
| Dark disc | 0.44R |
| Maximum value width | 0.78R |
| Unit centre | X = 0, Y = 0.54R |
| Pictogram centre | X = 0, Y = 0.73R |
| Pictogram size | 0.18R |
| Gauge name | X = 0, Y = 1.15R |

The value is centred on the glyphs' real bounds, not on the baseline, and shrinks until the number,
its comma and its minus fit the disc. The ring pair, the bright progress arc between them, the red
zone, the 60 ticks and the needle are drawn from the same constants as VPM: 135° start, 270° sweep.
The main screen is three gauges (`coolant 40…120 °C`, `absolute boost 0…2.5 bar`, `oil 40…150 °C`)
and five tiles (DSG temperature, intake air, exhaust gas, voltage, rail pressure) in **one row**
from 480 dp wide; the tile height is 26 % of the screen height (62–110 dp) so the gauges keep the
rest. Below 480 dp the five tiles wrap, as there is no honest way to fit them. The category screen
is the same gauges smaller, the six tabs and a grid of two full rows, 2–5 columns by width.

Text uses Titillium Web, which DashGrid already ships. Colours are VPM's night and day tokens
(`#10151B…#E8ECEF` and the rest); the car screen's Material palette is not used inside the cockpit.
Night and day follow the current car theme.

## The data contract

VPM answers a read-only `ContentProvider` call; DashGrid never starts VPM's service and never polls
the adapter from the provider.

| Item | Value |
|---|---|
| Authority | `io.github.nchalexz.aaperfmon.telemetry` |
| Permission | `io.github.nchalexz.aaperfmon.permission.READ_TELEMETRY` (`normal`) |
| Method | `call("snapshot", null, null)` |
| Other methods | not supported; `query`/`insert`/`update`/`delete` are not allowed |
| Contract version | `1` |

The bundle carries `version` (Int), `connection` (`demo`, `connecting`, `connected`, `disconnected`,
`error`), `status` (VPM's own line), `source` (transport, optional), `updatedAt` (when VPM last
changed values, not when DashGrid read it) and `signals` (JSON array, 128 entries at most).

Every signal is what VPM's own cockpit shows:

```json
{
  "id": "engine.coolant_c",
  "label": "ОЖ",
  "value": "86",
  "unit": "°C",
  "raw": 86,
  "stale": false,
  "warning": false,
  "category": "temperatures",
  "status": "valid"
}
```

`value` is formatted text (comma decimals), `raw` is the same number in display units (bar for
absolute boost and rail pressure, not kPa), `stale` and `warning` are VPM's own flags, and `status`
(`valid`, `stale`, `never_read`, added in VPM 0.6.0) is the bridge's own statement about freshness.
The array is grouped by category in VPM's display order; a signal VPM shows under two tabs (MAP
under Engine in kPa and under Turbo in bar) appears once per tab with the matching `category`.
DashGrid keeps the order and picks the occurrence of the metric's own category, so the boost gauge
never mistakes 98 kPa for 98 bar.

VPM applies its display factors before the bridge: `raw` for `engine.map_kpa` and `fuel.rail_kpa`
arrives in bar, already divided by 100. DashGrid therefore keeps its own factor at 1 for the
absolute-boost and rail metrics; a second 0.01 would under-read them by a hundred.

Only the visible page polls: one Binder call every 200 ms while the Car workspace is on screen, and
nothing when it is not. A bridge problem is logged once, not five times a second.

## States the cockpit can be in

| Situation | What the user sees |
|---|---|
| VPM not installed | **VAG Performance Monitor is not installed** and **Install** (the releases page) |
| VPM installed, no bridge (VPM 0.4.1) | **VPM is installed, but this version doesn't share telemetry with DashGrid** and **Update VPM** / **Open on the phone** |
| Bridge present, VPM has not run yet | the cockpit with dashes and a **NO LINK** pill, never a drawn zero |
| VPM in demo | VPM's demo values with the **DEMO** pill |
| Live link | values, the **LIVE** pill and VPM's status line |
| Stale value | the last known number, dimmed, with a stale dot; the needle fades but stays on the scale |
| Never read | a dash: there was no successful read in the session, which is not the same as zero |
| Bridge refused | a card with **Open on the phone** / **Update VPM**, no crash |
| The page is off screen | a calm "VPM cockpit — on another page" tile, no polling |

The states are a pure mapping (`vpmUiState`) and are unit-tested, including the missing-app case.

## Requirements

- VPM **0.6.2** installed on the same phone for the composition described here; the contract is
  v1 and works from VPM 0.5.0. This is the only DashGrid feature that needs VPM; workspaces, live
  panes and the dock work without it. VPM 0.4.1 does not export the provider, and the cockpit then
  shows the **Update VPM** card. An older 0.5.x bridge has no `status` field: DashGrid derives it
  from `stale` and the presence of a value, so such a VPM never crashes the cockpit. The contract is
  in [VPM_BRIDGE_PROMPT.md](VPM_BRIDGE_PROMPT.md).
- If DashGrid was installed before VPM defined this permission, update DashGrid once after installing
  VPM's bridge (reinstall the same build): Android grants a normal permission when the app that
  requests it is installed or updated, not retroactively when another app defines it.
- Android 6 or newer, like the rest of DashGrid. No root and no Shizuku for this widget.

## What was checked

On a **Huawei ALT-L29** (`HTV0223418000954`, Android 12) with the CI-signed **VPM 0.6.2**
(`versionCode 11`) and a locally built **DashGrid 1.3.2** (`versionCode 10302`), VPM in demo mode:

- Both packages report the expected version, and `dumpsys package io.dashgrid.app` reports
  `READ_TELEMETRY: granted=true`.
- The main screen draws VPM 0.6.2's set: gauges **ОЖ 86 °C**, **НАДДУВ АБС. 1,62 бар**,
  **МАСЛО 95 °C**; tiles **DSG 82 °C**, **впуск 31 °C**, **выхлоп 320 °C**, **напряжение 14,4 В**,
  **рампа 447 бар**. VPM's own cockpit at the same moment carried the same labels and units; the
  numbers differed by a few units only because both apps sample the moving demo model at different
  instants.
- All six tabs open with two whole rows and no clipped values. **Turbo** shows the absolute boost in
  bar while **Engine** keeps the same signal id in kPa, and the boost gauge still reads bar.
- The demo pill stays on both apps: demo values are never presented as a car.

Not checked in this session: a live ELM327/car (the saved OBDII adapter was listed, but no car session was
available), the stale and
dropped-link states on a device, the **Update VPM** / **Install** cards, a real head unit, EMUI's
car screen and the 1920 × 720 / 1280 × 480 sizes. The stale rules are covered by JVM tests; the
earlier 1.3.0/1.3.1 emulator run covered the cards, gestures and the two sizes with VPM 0.5.0, but
its stale-as-dash behaviour is exactly what 1.3.2 replaces. Emulator or phone-preview captures are
not proof of head-unit compatibility.

The unit tests cover the parser (bad JSON, missing fields, version mismatch, one entry per category,
`status` present/absent/unknown), the readings (demo, live, disconnected, waiting, stale,
never-read, a dropped link with a stale number, warning/critical, the VAG oil alternative), the
preset (added once, deleted stays deleted, JSON round-trip), the legacy `car` → `vpm` migration and
the state cards. No OBD/ELM327 connection is opened by DashGrid: the cockpit has no OBD code, and
the log shows no adapter attempt.

## Trying it without a car

The debug build has a preview activity with VPM's mock values (not part of a release APK):

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n io.dashgrid.app/net.prototux.aare.vpm.VpmPreviewActivity --ei page 1
# page 0 = main, page 1 = categories; --ez night false for the day palette
```

The real car screen on the phone is the ordinary **Settings → Workspaces → Preview car screen**.

## The cross-repository release session

The session that verifies both apps, runs both test suites, checks compatibility and publishes VPM
and DashGrid releases follows [VPM_COMPAT_PROMPT.md](VPM_COMPAT_PROMPT.md). The VPM 0.5.0 and
DashGrid 1.3.x releases were produced by it; this page remains the DashGrid-side description.

## Files

- [`VpmSnapshot.kt`](../app/src/main/java/net/prototux/aare/vpm/VpmSnapshot.kt) — the contract, the
  parser and the connection kinds.
- [`VpmBridge.kt`](../app/src/main/java/net/prototux/aare/vpm/VpmBridge.kt) — the provider call, the
  package lookups and the install/releases actions.
- [`VpmMetrics.kt`](../app/src/main/java/net/prototux/aare/vpm/VpmMetrics.kt) — the main-screen
  metrics, the readings and the category mapping.
- [`VpmGauge.kt`](../app/src/main/java/net/prototux/aare/ui/car/vpm/VpmGauge.kt) — the gauge and its
  geometry.
- [`VpmScreens.kt`](../app/src/main/java/net/prototux/aare/ui/car/vpm/VpmScreens.kt) — the two
  screens, the tiles, the tabs and the pill.
- [`VpmCockpit.kt`](../app/src/main/java/net/prototux/aare/ui/car/vpm/VpmCockpit.kt) — polling, the
  cards and the pager.
- [`WorkspaceModel.kt`](../app/src/main/java/net/prototux/aare/workspace/WorkspaceModel.kt) — the
  preset, the ids and the config version.
