# Testing

## Automated tests

```sh
./gradlew testDebugUnitTest
```

Result for this build:

```
870 tests, 0 failures, 0 errors, 0 skipped
BUILD SUCCESSFUL
```

That is AARE's whole unit-test suite plus 28 new workspace tests
(`app/src/test/java/net/prototux/aare/workspace/WorkspaceModelTest.kt`), covering:

- cyclic indexing and stepping in both directions, and the shortest ring distance;
- virtual pager pages mapping back to workspaces (the seamless `last → first` wrap);
- every layout preset: inside the 10 × 6 grid, no overlaps, exactly fills it, for both axes;
- the 70/30 split and the portrait turn preserving area;
- adding items to preset slots, free-cell placement, refusing a layout with fewer slots;
- reflow after removal, overlap rejection, custom-grid switching;
- live app panes (`pane`), JSON round-trip and duplicate/invalid pane handling;
- removing the active/default workspace, refusing to remove the only one;
- reordering, tolerant decoding of broken/duplicated/overlapping data;
- migration of the old dashboard, including 5–7 cards into a custom grid without loss.

## Build verification

```sh
./gradlew assembleDebug       # successful
./gradlew dashgridStandaloneApk   # published release APK
```

Published artifact:

```
app/build/outputs/dashgrid/DashGrid-standalone-v1.0.4.apk
19,430,832 bytes
SHA-256 8cf8887082c804106eeeaef3e88e5def383df12bf7c4ebd95e0b72563889f417
```

Verified from the APK itself:

```sh
apksigner verify --print-certs app/build/outputs/dashgrid/DashGrid-standalone-v1.0.4.apk
aapt2 dump badging app/build/outputs/dashgrid/DashGrid-standalone-v1.0.4.apk
aapt2 dump xmltree --file AndroidManifest.xml app/build/outputs/dashgrid/DashGrid-standalone-v1.0.4.apk
```

- `package: io.dashgrid.app`, `versionCode 10004`, `versionName 1.0.4`;
- `application-label: DashGrid`;
- signature: v1 + v2, one signer, `CN=DashGrid`, SHA-256
  `4c464d2c35c723c2bc4ca30dafa84c3dbdf16473d61ec9961e37dc6e973420a5`;
- the standalone APK declares `android.car.permission.TEMPLATE_RENDERER` (protection level
  normal); the side-by-side build (`./gradlew dashgridApk`) only uses it, for installing beside
  AARE;
- provider authorities: `io.dashgrid.app.car`, `io.dashgrid.app.shizuku`,
  `io.dashgrid.app.androidx-startup` — no AARE authority;
- assistant-link scheme: `dashgrid`;
- USB accessory filter still declares the Android Auto models (`Android Auto`,
  `Android Open Automotive Protocol`), unchanged for the protocol;
- adaptive icon, monochrome icon and legacy PNGs are packaged.

## Manual verification steps

```sh
adb uninstall net.prototux.aare   # required for the standalone APK
adb install -r app/build/outputs/dashgrid/DashGrid-standalone-v1.0.4.apk

# Preview without a car
# DashGrid → Settings → Workspaces → Preview car screen

# With the car
# Connect USB, accept the DashGrid accessory prompt, trust the car's certificate when asked,
# then build workspaces in Settings → Workspaces.
```

For an update, rebuild with a higher `versionCode` and the same keystore, then `adb install -r`.

## Compatibility notes

- **Yandex Music**: the inspected APK declares `MusicBrowserService` with
  `android:enabled="false"`, so its full library is not available to a third-party host. The Music
  widget shows and controls the active media session (artwork, metadata, progress/seek,
  play/pause, previous/next). Details in [docs/MEDIA_COMPATIBILITY.md](docs/MEDIA_COMPATIBILITY.md).
- **Two navigation apps** can be connected (split screen/different workspaces); only one is the
  active navigation app for the dashboard map, cluster and "navigate to". Android may pause the
  background app's surface.
- **One live map surface per `CarAppConnection`**; a workspace that is not current shows a
  placeholder for its map.
- **Standalone vs side-by-side**: the standalone APK cannot be installed while AARE is installed
  because both define `TEMPLATE_RENDERER` with different certificates.
