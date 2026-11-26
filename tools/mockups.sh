#!/bin/sh
# Draws the car screen mockups for the README (docs/screenshots/styles) with Paparazzi, on the JVM:
# no emulator, no phone. They are mockups: made-up apps and music, a drawn map; a real car screen
# can look a bit different. Needs ~3 GB of memory.
set -e
cd "$(dirname "$0")/.."
./build.sh :app:recordPaparazziDebug -Pmockups --no-configuration-cache --tests '*CarScreenMockups*' "$@"
mkdir -p docs/screenshots/styles
for f in app/src/test/snapshots/images/*CarScreenMockups*.png; do
    # net.prototux.aare.ui.car_CarScreenMockups_retroAmber.png -> retroAmber.png
    name=$(basename "$f" .png | sed 's/.*CarScreenMockups_//')
    cp "$f" "docs/screenshots/styles/$name.png"
done
# The 1.1.0 documentation images (dock tones, monochrome dock, fixed buttons).
cp app/src/test/snapshots/images/*CarScreenMockups_dockTonesLight.png docs/screenshots/dock-tones-light.png
cp app/src/test/snapshots/images/*CarScreenMockups_dockTonesDark.png docs/screenshots/dock-tones-dark.png
cp app/src/test/snapshots/images/*CarScreenMockups_dashgridMonochromeLight.png docs/screenshots/dock-monochrome-light.png
cp app/src/test/snapshots/images/*CarScreenMockups_dashgridMonochromeDark.png docs/screenshots/dock-monochrome-dark.png
cp app/src/test/snapshots/images/*CarScreenMockups_dashgridPinnedButtons.png docs/screenshots/pinned-buttons.png
# The phone editor with an Android-widget item (the second snapshot class).
./build.sh :app:recordPaparazziDebug -Pmockups --no-configuration-cache --tests '*WorkspaceRussianMockup*' "$@"
mkdir -p docs/screenshots
cp app/src/test/snapshots/images/*WorkspaceRussianMockup_editorRussian.png docs/screenshots/workspace-editor-android-widget.png
rm -rf app/src/test/snapshots
ls docs/screenshots/styles
