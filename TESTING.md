# Testing

Honest scope: this repository was built and tested **without a car and without an Android device**.
Everything below marked “automated” or “build” was actually run in this workspace; everything that
needs a head unit, a phone, EMUI, a physical touch screen or a real drive is marked **not run**.

## Environment used

| | |
|---|---|
| Host | macOS (Apple silicon) |
| JDK | Eclipse Temurin 21.0.7 |
| Gradle | 9.9.1 (wrapper) |
| Android Gradle plugin | 9.5.2 |
| compileSdk / targetSdk | 37 / 37 |
| Build tools | 36.0.0 |
| SDK platform | `android-37.0` |
| minSdk | 23 |
| Product | `io.dashgrid.app`, DashGrid 1.0.2 (10002) |

## Automated tests

```sh
./gradlew testDebugUnitTest
```

Result on 2026-11-25:

```
869 tests, 0 failures, 0 errors, 0 skipped
BUILD SUCCESSFUL
```

That is AARE's whole unit-test suite plus **25 new workspace tests**
(`app/src/test/java/net/prototux/aare/workspace/WorkspaceModelTest.kt`), covering:

- cyclic indexing and stepping in both directions, and the shortest ring distance;
- virtual pager pages mapping back to workspaces (the seamless `last → first` wrap);
- every layout preset: inside the 10 × 6 grid, no overlaps, exactly fills it, for both axes;
- the 70/30 split really being 7/3 columns, and the portrait turn preserving area;
- adding items to preset slots, free-cell placement, refusing a layout with fewer slots;
- reflow after removal, overlap rejection, custom-grid switching;
- removing the active/default workspace and refusing to remove the only one;
- reordering, JSON round-trip, tolerant decoding of broken/duplicated/overlapping data;
- migration of the old dashboard: 1–4 cards into a preset, **5–7 cards into a custom grid with no
  card lost**;
- default configuration and grid helpers.

Escaped XML strings and resource references are compiled by the Android build, so missing
translations/ids fail the build (they did during development and were fixed).

## UI rendering check (JVM, not a device)

```sh
./gradlew :app:recordPaparazziDebug -Pmockups --no-configuration-cache --tests '*CarScreenMockups*'
```

Paparazzi rendered the car screen from the real Compose tree at 1280 × 720, including the Russian
workspace render (optional name, dots without arrows) and the Russian phone editor. The images are
in [docs/screenshots/](docs/screenshots/).
This proves the workspace composables lay out and draw on the JVM; it does **not** exercise touch,
surfaces, a head unit or EMUI.

## Build verification

```sh
./gradlew assembleDebug      # successful
./gradlew dashgridApk        # successful, signed release
```

Release artifacts:

```
app/build/outputs/dashgrid/DashGrid-v1.0.2.apk                 19,429,297 bytes
SHA-256 8c43f142f47dd8a8d7ffa74bf39dd53c8576c6e0254cd8ec58db230d071fc806

app/build/outputs/dashgrid/DashGrid-standalone-v1.0.2.apk      19,429,308 bytes
SHA-256 c02aca5bd7495b133e8607c6778f1e028bc4d7b8c055e48bed111ba2cf1a2c67
```

The side-by-side APK must **not** contain a `<permission>` element for
`android.car.permission.TEMPLATE_RENDERER` (only a `<uses-permission>`); the standalone APK must
contain it. Otherwise installing beside AARE fails with `INSTALL_FAILED_DUPLICATE_PERMISSION` —
the failure that was observed on EMUI and fixed here. Both APKs have the same package, version and
certificate.

Verified from the APK itself:

```sh
apksigner verify --print-certs app/build/outputs/dashgrid/DashGrid-v1.0.2.apk
aapt2 dump badging app/build/outputs/dashgrid/DashGrid-v1.0.2.apk
aapt2 dump xmltree --file AndroidManifest.xml app/build/outputs/dashgrid/DashGrid-v1.0.2.apk
```

- `package: io.dashgrid.app`, `versionCode 10002`, `versionName 1.0.2`;
- `application-label: DashGrid` (all locales);
- signature: v1 (JAR) **true**, v2 **true**, one signer,
  `CN=DashGrid`, SHA-256 `4c464d2c35c723c2bc4ca30dafa84c3dbdf16473d61ec9961e37dc6e973420a5`;
  this is not AARE's release certificate, so the two apps are independent;
- manifest check: the side-by-side APK has no `<permission>` for `TEMPLATE_RENDERER`; the
  standalone APK has it (protection level normal);
- provider authorities in the APK: `io.dashgrid.app.car`, `io.dashgrid.app.shizuku`,
  `androidx.car.app.connection`, `io.dashgrid.app.androidx-startup` — no
  `net.prototux.aare.car`, so installing AARE beside it doesn't clash;
- assistant-link scheme: `dashgrid` (not `aare`);
- USB accessory filter still declares the Android Auto models
  (`Android Auto`, `Android Open Automotive Protocol`) — unchanged, as the protocol requires;
- adaptive icon + monochrome icon + legacy PNGs are packaged (`application-icon` resolves to the
  adaptive icon on API 26+, to `mipmap-*/ic_launcher.png` below).

Not run: `./gradlew lint` (the project disables several upstream lint categories for a sideloaded
app; `lintVitalRelease` runs as part of the release build and passed). No signing with AARE's key
is possible or intended.

## Manual checklist (not run here)

| # | Scenario | Status | What to check |
|---|---|---|---|
| 1 | Install AARE and DashGrid on the same phone | **Not run** | Both install; two launcher icons; separate settings; no `INSTALL_FAILED_CONFLICTING_PROVIDER` |
| 2 | Launch each independently | **Not run** | Each starts with its own data; DashGrid's name/icon |
| 3 | 1, 3 and 5 workspaces | **Not run** (model tested) | Creation, dots, `1 → 2 → 3 → 1`, `1 → 5 → 4 → 1` |
| 4 | Cyclic switching both ways | **Not run** (math tested) | Bar swipe, dots, no jump at the wrap |
| 5 | Layouts 100/50-50/70-30/30-70/3/4/custom | **Not run** (model tested) | Widgets fill, no gaps/overlaps, axis switch |
| 6 | Reorder workspaces | **Not run** | Active workspace keeps its contents; order persists |
| 7 | Delete the active workspace | **Not run** (model tested) | Neighbour becomes active; last one can't be deleted |
| 8 | Settings survive a restart | **Not run** | Names, layouts, items, default, last workspace |
| 9 | Update APK without uninstalling | **Not run** | `adb install -r` with a higher `versionCode` signed by the same key; settings kept |
| 10 | Active navigation survives workspace switches | **Not run** | Start a route in a map workspace, switch to another, return: route and guidance still there |
| 11 | Music keeps playing while switching | **Not run** | Play in one workspace, switch; audio uninterrupted; widget state |
| 12 | Map gestures don't switch workspaces | **Not run** | Pan/zoom the map; only the bar/empty-space swipe changes page (default setting) |
| 13 | Rapid switching (20+ times) | **Not run** | No flicker, no ANR, map returns |
| 14 | Memory over a long connection | **Not run** | No growth per switch; surfaces released |
| 15 | Head-unit disconnect/reconnect | **Not run** | AARE's reconnect path unchanged; workspaces reload with the session |
| 16 | Day/night themes | **Not run** | Both palettes readable; rail dark-bar still works |
| 17 | 1280×720, 1920×720, portrait | **Not run** | Grid and bar don't clip; portrait turn |
| 18 | No root, no GMS | **Not run** | App works with AARE's non-root features; no GMS crash |

### How to run the device checks

```sh
# Build and install
./gradlew dashgridApk
adb install -r app/build/outputs/dashgrid/DashGrid-v1.0.2.apk

# Preview without a car
# DashGrid → Settings → Workspaces → Preview car screen

# With the car
# Connect USB, accept the Android Auto accessory prompt for DashGrid,
# trust the car's certificate when asked, then use Settings → Workspaces to build pages.
```

For the update test, rebuild with `-PversionName=1.0.2 -PversionCode=10002` (same keystore) and
`adb install -r`; settings must remain.

## Known gaps and risks

- **No physical validation**: Huawei Mate X3/EMUI, VW Golf 7.5 USB, and any real head unit are
  untested. AARE itself has car reports; DashGrid adds no protocol changes, but the host's identity
  and layout changes still need a real run.
- Car App Library apps that allow-list AARE's signing digest must allow DashGrid's; this cannot be
  verified without those apps.
- Two navigation apps at once is constrained by Android and by one surface per connection; see
  ARCHITECTURE.md.
- The classic layout does not use workspaces (it keeps AARE's app-grid home).
- The editor preview is landscape-only; the car turns the grid on portrait head units.
- Performance was reasoned about (lazy pager, no off-screen surfaces) but not profiled on a device.
