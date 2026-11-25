# Release signing key

`dashgrid-release.jks` is DashGrid's own release key (generated once, not AARE's). The APK in
`dist/` is signed with it. Keep `keystore.properties` next to it: a build without that file
produces an unsigned release APK, and Android refuses to install an update signed with a different
key over an installed DashGrid.

The `.jks` and `keystore.properties` are deliberately ignored by git. Back them up; if they are
lost, an installed DashGrid can only be updated after an uninstall (which removes its data).

To verify a build:

```sh
apksigner verify --print-certs app/build/outputs/dashgrid/DashGrid-v1.0.0.apk
```
