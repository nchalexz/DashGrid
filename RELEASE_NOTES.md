<p align="center">
  <img src="https://raw.githubusercontent.com/nchalexz/DashGrid/main/docs/screenshots/social-preview.png" width="100%" alt="DashGrid 1.3.1 — one cockpit entry in the settings">
</p>

<h1 align="center">DashGrid 1.3.1</h1>

<p align="center">
  <strong>STABLE FIX — one cockpit in the settings, and saved workspaces move to it.</strong><br>
  27 November 2026
</p>

> [!WARNING]
> **No real head unit, phone or car was used.** 1.3.1 is a patch on 1.3.0, checked by JVM tests and
> `lintVital`; the cockpit itself is the 1.3.0 build. The migration was not exercised on a real
> installed phone or head unit with an old saved config. Emulator captures are not compatibility
> proof. See [Limits](#limits).

1.3.1 fixes the cockpit entry and the upgrade path, not the cockpit. The widget picker now offers a
single cockpit — **VPM cockpit** — and a workspace saved by an older build that still holds AARe's
**Car** (OBD cockpit) widget gets the VPM cockpit in the same cells on load. The old cockpit is gone
from the settings and the car screen shows the same cockpit the phone preview shows.

In this release: [What changed](#what-changed) · [Migration](#migration) · [Install](#install) ·
[Verification](#verification) · [Limits](#limits)

## What changed

- **One cockpit entry.** The workspace widget picker no longer lists AARe's **Car** widget; the
  cockpit choice is **VPM cockpit**, fed by VPM's read-only telemetry as in 1.3.0.
- **Saved configs migrate.** Every workspace that still holds the old `car` widget gets `vpm` when
  the config is loaded, keeping the item's id and cells. If the workspace already has the VPM
  cockpit, the old item is dropped instead of drawing a second cockpit.
- **No dead code.** The old widget's drawing branch and its unused “adapter not connected” string are
  removed. DashGrid's own OBD app, its settings and the read-only OBD stack are untouched, and no
  permission, provider or exported component is added or removed.

## Migration

| Saved workspace | On the next load |
|---|---|
| `car` widget only | becomes `vpm`, same item id, same cells, layout unchanged |
| `car` and `vpm` | the `car` item is dropped, `vpm` stays where it was |
| no `car` widget | unchanged (the migration is a no-op) |

The migration is idempotent: after the first load there is nothing left to replace, so deleting the
Car workspace or reordering items later is not affected.

## Install

Download
[DashGrid-standalone-v1.3.1.apk](https://github.com/nchalexz/DashGrid/releases/download/dashgrid-v1.3.1/DashGrid-standalone-v1.3.1.apk),
then install it with `adb install -r` or by opening the file. It installs over 1.3.0 in place: same
`CN=DashGrid` key, `versionCode 10301`.

```text
19,707,283 bytes
SHA-256  24f6679ee5247fb9ca246627a2c99f7d3a224afaba39cf4dc9fcae5ccf7e5570  DashGrid-standalone-v1.3.1.apk
certificate SHA-256: 4c464d2c35c723c2bc4ca30dafa84c3dbdf16473d61ec9961e37dc6e973420a5
```

> [!IMPORTANT]
> This release is **standalone only**. The APK defines `android.car.permission.TEMPLATE_RENDERER`
> itself, so **AARE must be uninstalled first** — unless DashGrid is already installed, in which case
> install over it. Requirements: Android 6.0 or newer, USB or wireless Android Auto, no Google Mobile
> Services, and VPM 0.5.0+ for the cockpit to show values.

## Verification

- 1,193 JVM tests, 0 failures (949 `app`, 219 `aap`, 25 `headunit`), including two new
  `WorkspaceModelTest` cases: `car` becomes `vpm` with the same id and cells, the old item is dropped
  when `vpm` is present, and a second migration is a no-op.
- `lintVital` on the standalone release.
- The APK was checked with `aapt2 dump badging` (`versionCode 10301`, `versionName 1.3.1`) and
  `apksigner verify --print-certs` (the same `CN=DashGrid` certificate as 1.3.0).
- Not verified: the migration on a real phone or head unit with an old saved config, a real
  ELM327, a car, a real head unit and EMUI 14+. The cockpit values remain VPM's demo values.

## Limits

- **No hardware verification.** No real head unit, car or ELM327 was available; the patch is tested
  on the JVM and built for the emulator, not driven in a car.
- **VPM 0.5.0+ is required for values.** On 0.4.1 the cockpit explains the missing bridge instead of
  drawing numbers.
- **AARE must go first** on a phone where it is still installed; the standalone APK cannot coexist
  with it.
- **No in-app update check.** New releases are installed by hand.

## Documentation

- [README](https://github.com/nchalexz/DashGrid/blob/main/README.md) — install, workspaces and the
  Cockpit section.
- [VPM cockpit](https://github.com/nchalexz/DashGrid/blob/main/docs/VPM.md) — screens, contract,
  states, checks and files.
- [CHANGELOG](https://github.com/nchalexz/DashGrid/blob/main/CHANGELOG.md) ·
  [TESTING](https://github.com/nchalexz/DashGrid/blob/main/TESTING.md).
