# Upstream: AARE

DashGrid is a fork of **[AARE](https://github.com/prototux/AARE)**, “Android Auto, without Google”,
by prototux and the AARE contributors. AARE is licensed under **GPL-3.0-or-later**; DashGrid keeps
that licence and the original copyright notices. See [LICENSE](LICENSE).

This repository on GitHub is a fork of `prototux/AARE`, so GitHub displays the “forked from
prototux/AARE” note. The fork is maintained at
[github.com/nchalexz/DashGrid](https://github.com/nchalexz/DashGrid).

## Base

| | |
|---|---|
| Upstream | https://github.com/prototux/AARE |
| Base version | AARE 1.6.0 (`versionName` 1.6.0, `versionCode` 10600) |
| Base commit | `1aa62163` (`Merge pull request #412 …`) |
| Fork version | DashGrid 1.0.3 (`versionCode` 10003) |
| DashGrid branch | `feature/dashgrid-workspaces` |
| DashGrid default branch | `main` (the fork, including workspaces) |

DashGrid's code lives mostly in new files plus small hooks in AARE files. AARE's internal package
names (`net.prototux.aare…`) are kept on purpose, so an upstream change can usually be merged by
path rather than re-applied by hand.

## Remotes

```sh
git remote -v
# origin    git@github.com:nchalexz/DashGrid.git   (the fork, push here)
# upstream  https://github.com/prototux/AARE.git    (read-only, fetch updates)
```

If you cloned only the fork:

```sh
git remote add upstream https://github.com/prototux/AARE.git
```

## Bringing in an upstream release

1. **Read upstream's release notes and `docs/decisions.md`**; some upstream changes touch files
   DashGrid changed (`CarShell.kt`, `CarRuntime.kt`, `Settings.kt`, `strings.xml`,
   `app/build.gradle.kts`).
2. Fetch and inspect:

   ```sh
   git fetch upstream
   git log --oneline --no-merges HEAD..upstream/main
   git diff --stat HEAD...upstream/main
   ```

3. Merge upstream into the fork branch, not into a copy of upstream:

   ```sh
   git switch feature/dashgrid-workspaces
   git merge upstream/main
   ```

4. Resolve conflicts with these rules:
   - **Keep DashGrid's identity**: `applicationId`, `versionCode`/`versionName`, signing config,
     `dashgridApk`, the update-check default, the icon and `DashGridTheme`.
   - **Keep AARE's protocol/transport/audio/microphone behaviour** exactly as upstream unless there
     is a documented reason; DashGrid does not change the AAP layer.
   - **Keep workspace hooks**: `CarUiState.workspaceConfig`/`activeWorkspaceId`, the
     `reloadWorkspaces()` call in `CarRuntime.start()` and the settings listener, the
     `WorkspaceHome` calls where `Dashboard` used to be (taskbar content and `DockShell`), the
     workspace actions in `CarActions`, and the `WorkspaceStore` migration reading
     `Settings.dashboardCards`.
   - In `strings.xml`, re-apply the identity strings if upstream renamed them: `app_name`,
     `setup_tagline` (slogan), `about_text`, `about_upstream_*`, and the `workspaces_*` keys.
   - In `AndroidManifest.xml`, re-apply the authority (`io.dashgrid.app.car`), the `dashgrid`
     scheme and the fork's application id (Gradle sets the last one).
5. Build and test:

   ```sh
   ./gradlew testDebugUnitTest
   ./gradlew dashgridApk
   ```

   Check the APK identity and signature (TESTING.md has the commands). Test the preview on a phone
   if possible.
6. Raise `versionCode`/`versionName` and add a CHANGELOG entry. Commit the merge; never rewrite
   published tags.
7. Push `main` and the development branch:

   ```sh
   git push origin main feature/dashgrid-workspaces
   ```

   The fork's `main` is expected to be a fast-forward of upstream `main` plus DashGrid commits, so
   upstream merges remain reviewable.

## What must not change (Android Auto protocol / Android identity)

These are protocol or platform contracts, not branding. Changing them breaks head units, car apps
or updates.

- **USB accessory models** in `app/src/main/res/xml/accessory_filter.xml` and
  `transport/UsbAccessoryTransport.MODELS`: `Android Auto` and
  `Android Open Automotive Protocol`. Head units send these strings.
- **AAP wire protocol**: the `aap` module (protobuf definitions, message ids, channel ids, TLS
  handshake, service discovery, wireless UUID `AA_WIRELESS_UUID`, Bluetooth SDP names). DashGrid
  keeps AARE's `aap` module unchanged.
- **Android Auto certificates**: the certificate download source and the Google Automotive Link CA
  pin in `cert/CertificateDownload.kt`; the head unit's mutual-TLS check depends on them.
- **Car App Library permissions/actions**: `androidx.car.app.CarAppService`,
  `androidx.car.app.category.*`, `androidx.car.app.ACCESS_SURFACE`,
  `androidx.car.app.NAVIGATION_TEMPLATES`, `androidx.car.app.MAP_TEMPLATES`, and
  `android.car.permission.TEMPLATE_RENDERER` (declared as upstream does; some car apps trust any
  host holding it).
- **GMS-only integration package/service names** used by the Maps/Waze guidance and the private
  `ICarProjection` protocol (`com.google.android.gms.car…`, `com.google.android.apps.gmm…`).
- **Exported component contracts that are part of AARE's own API** may be renamed only with a
  migration note: the CarControlProvider authority and the assistant-link scheme are DashGrid's
  own (`io.dashgrid.app.car`, `dashgrid://`) because AARE must remain installable beside it. Third
  parties that hard-code AARE's values need a DashGrid variant.

## Identity invariants (safe to keep fork-specific)

| Item | DashGrid |
|---|---|
| `applicationId` | `io.dashgrid.app` |
| Xposed module `applicationId` | `io.dashgrid.app.xposed` |
| App label | `@string/app_name` = DashGrid |
| `versionCode` / `versionName` | `10003` / `1.0.3` |
| APK | `app/build/outputs/dashgrid/DashGrid-v<version>.apk` |
| Signing | `keystore/dashgrid-release.jks` + ignored `keystore.properties` |
| Car-control authority | `io.dashgrid.app.car` |
| Shizuku provider authority | `${applicationId}.shizuku` (automatic) |
| Assistant links | `dashgrid://` |
| Default car theme | `dashgrid` (`DashGridTheme`) |

**Never** commit `keystore.properties` or the `.jks` to a public repository. Losing the key means
an installed DashGrid must be uninstalled before a differently signed build can be installed,
which deletes its settings.

## Updating this document

When a merge changes the base version/commit, update the table at the top and add a line to
CHANGELOG.md. Keep this file and README’s “fork of AARE” note accurate: attribution is a licence
requirement, not decoration.
