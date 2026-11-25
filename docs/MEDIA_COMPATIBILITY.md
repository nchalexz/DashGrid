# Media compatibility: Yandex Music

This page records what DashGrid can and cannot do with the official **Yandex Music** app on a
Huawei/EMUI phone without root. It is based on the code in this repository and on a static
inspection of a Yandex Music APK.

## What AARE already had

- `media/MediaDiscovery` finds apps that export a **media3 `MediaLibraryService`** or a legacy
  **`android.media.browse.MediaBrowserService`**. It ignores non-exported services and disabled
  components (the platform does not resolve them by default).
- `media/MediaConnection` opens a media3 `MediaBrowser` (and, for legacy apps, a framework
  `MediaBrowser` for Android Auto hints) to one of those apps and gives the full library UI:
  root/tabs, lists and grids, search, now playing, queue and custom transport actions.
- `media/SessionTracker` follows **all active platform `MediaSession`s** through
  `MediaSessionManager` when DashGrid holds notification access. It gives metadata, artwork,
  progress, play/pause, skip and seek for an app DashGrid never connected to.
- `media/DefaultMediaHub` chooses what "now playing" and the steering-wheel/media keys follow
  (`pickSource`): a playing source wins, then the previous pick, then the system's order.
- The regular **Music** widget (`DashCard.MEDIA`) shows that playback: artwork, title, artist,
  progress (seekable when the session allows it), play/pause, previous/next. It is not tied to a
  workspace lifetime and never stops playback.

## What the Yandex Music APK actually declares

Inspected APK: `ru.yandex.music`, versionName `2020.11.1 #3608`, versionCode `23020551` (a public
2020 build; current builds may differ). From its manifest:

```xml
<service
    android:name="ru.yandex.music.common.media.mediabrowser.MusicBrowserService"
    android:enabled="false"
    android:exported="true">
    <intent-filter>
        <action android:name="android.media.browse.MediaBrowserService" />
    </intent-filter>
    <intent-filter>
        <action android:name="yandex.auto.media.action.YANDEXRADIO" />
        <action android:name="yandex.auto.media.action.MUSIC" />
    </intent-filter>
</service>

<service
    android:name="ru.yandex.music.common.service.player.MediaSessionService"
    android:enabled="true"
    android:exported="false"
    android:foregroundServiceType="mediaPlayback" />

<receiver android:name="ru.yandex.music.ui.widget.WidgetProvider">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
    </intent-filter>
    <meta-data android:name="android.appwidget.provider" … />
</receiver>
```

The important line is **`android:enabled="false"`** on the MediaBrowser service. It is exported, but
disabled in the manifest, so:

- `PackageManager.queryIntentServices` does not return it to DashGrid/AARE;
- DashGrid cannot add Yandex Music as a normal media-library app, and the full-screen library
  (My Wave, playlists, favourites, search, albums/artists, queue) is unavailable;
- binding it would require changing the app's component state (`pm enable`) through root/Shizuku,
  modifying the APK, or the app itself enabling it (it may do that when Android Auto/GMS is
  present; EMUI without GMS is exactly the case where it may not).

The `MediaSessionService` is not exported, which is normal: the system still exposes its
**MediaSession** to `MediaSessionManager`, so playback metadata and transport are available without
binding the service. That is the basis of the fallback.

A Yandex Music **Android AppWidget** (`ru.yandex.music.ui.widget.WidgetProvider`) is also declared.
Hosting Android AppWidgets is a separate platform API (`AppWidgetHost`), not implemented in this
release; it is the only way to show a true app-provided widget rather than a live app pane.

## Fallback: the regular Music widget

DashGrid deliberately does **not** ship a separate "media bridge" widget. The existing Music
widget already follows session-only players through `SessionTracker`:

- artwork, title and artist/album;
- progress bar with seeking when the session supports `ACTION_SEEK_TO`;
- play/pause, previous/next;
- automatic active-player detection; if Yandex Music is playing, the Music widget follows it;
- it does not create a second player or disconnect the app, so switching workspaces does not
  interrupt playback.

What it does **not** do without a MediaBrowser connection: browse My Wave/playlists/favourites,
search, albums/artists or the queue; launch an app that is not running; offer a source picker. Those
need either a working `MusicBrowserService` or a separate UI that this release does not include.

## Diagnostic procedure (on the phone)

1. Grant DashGrid **notification access** (needed for `getActiveSessions`).
2. Start Yandex Music manually and play something.
3. Check the session is visible to the system:

   ```sh
   adb shell dumpsys media_session | grep -i -A 20 yandex
   ```

   A `MediaSession` for `ru.yandex.music` with `state=PlaybackState {state=3 ...}` means the Music
   widget can control it.
4. Check the MediaBrowser component state:

   ```sh
   adb shell dumpsys package ru.yandex.music | grep -i -A 8 MusicBrowserService
   ```

   `enabled=false` (or the component missing from `queryIntentServices`) explains why the library
   is not listed. `pm enable` needs shell/root; DashGrid does not do it.
5. In DashGrid add the **Music** widget, play Yandex Music, and verify track/artist/artwork,
   play/pause, next/previous and the seek bar.
6. EMUI: exclude DashGrid and Yandex Music from battery optimisation; both must be allowed to run
   in the background. Without notification access DashGrid cannot see sessions at all.

## Acceptance matrix for this build

| Criterion | Status |
|---|---|
| Yandex Music sound through the car's audio system | Uses AARE's existing audio path (screen capture / Shizuku / root / Bluetooth); unchanged by the widget |
| Music widget shows current track, artist, artwork, progress | Implemented via `MediaSession` |
| Play/pause, next/previous, seek from the car | Implemented (`MediaController`); steering-wheel media keys keep using `MediaHub`'s target |
| Workspace switching does not interrupt music | The session is independent of the workspace and is never disconnected by the widget |
| Steering-wheel buttons | Existing AARE key handling; the widget follows the active session |
| No mandatory root/GMS | Uses framework `MediaSessionManager`; root not required, GMS not required |
| Full library (My Wave, playlists, favourites, search, albums/artists, queue) | **Not possible** when `MusicBrowserService` is disabled; would require enabling it via root/Shizuku or modifying the app, which this project does not do |
| Other music apps | Works with any app publishing a compatible media session; browser-capable apps keep AARE's full library UI |

## Known limitations and follow-ups

- Current Yandex Music versions may enable the browser service conditionally (e.g. when Android
  Auto is installed). If it is enabled on a given phone, AARE's existing library UI can connect;
  if not, only session control is possible. The APK inspected here is from 2020, so this can differ
  on the exact Yandex version installed on the Huawei.
- The Music widget does not show the session queue; the full player does for a connected app.
- The seek bar is a Material slider, allowlisted by the car reachability test; rotary stepping of
  the slider is a follow-up (same limitation as AARE's full player seek bar).
- An `AppWidgetHost` for Yandex Music's real Android widget is possible but not implemented; it
  needs a host ID, user binding and RemoteViews interaction on the car screen.
