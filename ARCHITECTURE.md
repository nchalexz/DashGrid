# DashGrid architecture

DashGrid is AARE plus a workspace layer. This document describes where the workspace layer sits,
what it reuses, how app sessions and surfaces behave while switching, and what is deliberately not
possible. Names below were checked against the code in this repository; the AARE parts are
unmodified except where noted.

## 1. What AARE already provides

```
car head unit
   │  USB accessory / Wi-Fi (AAP, TLS)
   ▼
transport/ + aap/                      Android Auto projection protocol
   ▼
car/CarSession                         handshake, video, audio, input; drives the car display
   ▼
host/CarRuntime                        app list, hosted car apps, media, notices, the Compose tree
   ├─ host/CarAppConnection             one AndroidX Car App Library connection (templates, map surface)
   ├─ host/CarAppScreen                 draws one connection's template / map SurfaceView
   ├─ media/MediaHub, notify/…          music, messages, calls, guidance
   └─ ui/car/CarShell                   the car screen's chrome and layouts
        ├─ CarLayout.TASKBAR → rail + ContentArea
        ├─ CarLayout.DOCK    → DockShell
        └─ CarLayout.CLASSIC → ClassicShell
```

Two entry points drive the same `CarRuntime`:

- **A real connection:** `service/ProjectionService` creates a `CarSession`, which creates a
  `CarRuntime` and renders it into the projection display.
- **Preview on the phone:** `ui/CarPreviewActivity` creates a `CarRuntime` directly and renders
  `runtime.Root()`. AARE's `headunit/` desktop module is the other end of a real connection, not
  a UI preview.

`ui/car/CarUiState` is the observable state the car UI reads; `CarActions` is what it can ask for.
`CarRuntime` implements `CarActions` and owns the connections to apps.

### Input

- Touch: the head unit's touch channel is mapped by `car/TouchMapper` (screen/visible-area margins,
  touch calibration) and dispatched to the Compose display.
- Physical keys/rotary: `CarSession.onKey` maps AAP key codes (media, voice, call, custom
  `KEYCODE_MEDIA`/`KEYCODE_NAVIGATION`/`KEYCODE_TEL`/…); the rotary knob and D-pad move Compose
  focus (TAB/DPAD injections). Focus highlights come from `carClickable`/`carFocusHighlight`.
- The car's Back goes through `CarRuntime.back()`: assistant, split pane, an app's own back stack,
  then the dashboard.

## 2. Workspace layer

New code lives in `net.prototux.aare.workspace` (pure model + storage + catalog) and
`ui/car/WorkspaceHome.kt` / `ui/WorkspaceSettings.kt` (UI). AARE's packages are untouched so
upstream merges stay simple.

```
ui/car/WorkspaceHome.kt        WorkspaceHome → WorkspacePager → WorkspacePage → widget/app items
        │                                      └─ WorkspaceBar (name, dots, arrows, swipe zone)
        ▼
ui/car/CarUiState               workspaceConfig, activeWorkspaceId, workspaceGeneration
        ▼
ui/car/CarModel                 WorkspaceConfig/Workspace/WorkspaceItem, cyclic helpers, cell math
        ▼
workspace/WorkspaceStore        SharedPreferences JSON, migration from the old dashboard
        ▼
Settings.workspacesJson         one versioned JSON string under key "workspaces"
```

The phone editor (`ui/WorkspaceSettings.kt`) uses the same `WorkspaceStore` and
`workspace/WorkspaceCatalog` (installed Car App Library and media apps). It never talks to a
`CarRuntime`: a running connection picks up each save through the existing
`Settings.registerListener` mechanism (`CarRuntime.reloadWorkspaces`).

### 2.1 Data model

`WorkspaceConfig` (version 1):

```json
{
  "version": 1,
  "default": "<workspace id>",
  "last": "<workspace id>",
  "swipeAnywhere": false,
  "showBar": true,
  "workspaces": [
    {
      "id": "<uuid>",
      "name": "City",
      "layout": "split70_30",
      "axis": "horizontal",
      "items": [
        { "id": "<uuid>", "kind": "widget", "widget": "map",
          "col": 0, "row": 0, "colSpan": 7, "rowSpan": 6, "settings": {} },
        { "id": "<uuid>", "kind": "widget", "widget": "media",
          "col": 7, "row": 0, "colSpan": 3, "rowSpan": 6 }
      ]
    }
  ]
}
```

- `Workspace.index` is the item's position in `workspaces`; the order is stored once, in the list,
  so reordering can never leave two workspaces with the same number.
- `WorkspaceItem.kind` is `app` (a shortcut; `app` holds an entry id such as `phone`, or a package
  name for a Car App Library app) or `widget` (`widget` is a `DashCard` id: `map`, `media`,
  `calendar`, `notification`, `weather`, `home`, `car`).
- `WorkspaceItem.appId` on a `map` or `media` widget is optional: it pins that widget to an app;
  without it the widget follows the car's active navigation/media app.
- `WorkspaceItem.settings` is a string map for per-widget settings. This version only uses
  `appId`; a future widget setting does not need a schema change.
- A widget appears at most once per workspace (one map surface owner), an app shortcut once per
  workspace. Across workspaces the same app or widget can appear freely; the app session is shared.

`WorkspaceJson` is deliberately forgiving: unknown fields are ignored, a newer `version` still
loads known fields, duplicated ids are regenerated, overlapping items are moved to free cells and
items that cannot be placed are left out. A document that cannot be parsed at all is kept under
`workspaces_broken` and a fresh default is used, so a bad file cannot silently destroy the user's
workspaces.

### 2.2 Layouts and the cell grid

The logical grid is **10 columns × 6 rows** in landscape. Presets:

| Layout | Cells |
|---|---|
| `full` | 10×6 |
| `split50` | 5+5 columns, or 3+3 rows |
| `split70_30` | 7+3 columns, or 4+2 rows |
| `split30_70` | 3+7 columns, or 2+4 rows |
| `three` | 4+3+3 columns |
| `four` | 2×2 cells of 5×3 |
| `custom` | each item's own `col,row,colSpan,rowSpan` |

Applying a preset reflows existing items into its slots; it **refuses** a preset with fewer slots
than items (`Workspace.withLayout` returns null) rather than dropping one. Removing an item from a
preset reflows the rest; from a custom grid it keeps every remaining cell. Moving or resizing an
item switches the workspace to `custom`; an overlap is rejected.

On a portrait head unit (height > width × 1.1) the same logical grid is turned 90°:
`(col,row,w,h) → (row, 10−col−w, h, w)`, so a left/right 70/30 split becomes a
bottom/top 70/30 split and areas are preserved. The phone editor previews the landscape grid.

### 2.3 Rendering

`WorkspaceHome` replaces the dashboard in the taskbar content area and in `DockShell`. It draws the
wallpaper once, then a pager:

- With one workspace there is no pager: the page is drawn directly and the bar is hidden.
- With more, `HorizontalPager` uses a **virtual** page number: page count 2 000 000, start at
  1 000 000, and `real = virtual mod count`. Swiping right past the last page lands on page
  `virtual+1`, which maps to workspace 0, so the ring is seamless in both directions and no
  recentering jump is ever visible. The arrows animate exactly one virtual page; a dot animates to
  the nearest virtual page with the wanted modulo (`nearestVirtualPage`).
- The pager's own gestures are **off** by default. A horizontal drag on the bottom bar, or on empty
  workspace space (a background layer under the items), posts `cycleWorkspace(±1)`. That keeps the
  map's pan gestures working. `swipeAnywhere` turns the pager's gestures on instead.
- The bar is `WorkspaceBar`: workspace name, one dot per workspace (up to eight, then “3/5”), and
  arrows. Dots and arrows use `carClickable`, so rotary/D-pad focus can reach them. It is hidden
  when `showBar` is off or there is one workspace. Pages get bottom padding for it, so it never
  covers content.

Items are ordinary Compose: an app shortcut is a card with the app's icon and label; widgets are
the existing dashboard composables (`NavCard`, `MediaCard`, `CalendarAgendaCard`, `WeatherCard`,
`HomeCard`, `LatestNotification`, `ObdDashCard`) inside the same `Card`. Empty widgets show a calm
placeholder rather than collapsing the grid, so positions stay stable.

### 2.4 Surfaces: the one thing that isn't just Compose

A hosted navigation app draws its map into a `Surface` owned by its `CarAppConnection`
(`surfaceAvailable`, one surface owner per connection). `CarAppScreen` calls
`CarRuntime.acquireVisible/releaseVisible`; the last release is delayed 500 ms so moving the same
app between the dashboard card and full screen doesn't pause it.

Workspaces exploit that:

- **Only the page at `pager.currentPage` renders the live map widget.** Pages beside it (composed
  during a swipe) render `WidgetPlaceholder`. Two live maps would fight for the same connection's
  surface, and two different navigation apps would each need their own surface — possible, but not
  worth the flicker for a page the driver is leaving.
- When a page settles, the incoming map widget acquires the surface and the app redraws. The
  connection was never disconnected: route, guidance and the app's state survive.
- Two map widgets in one workspace are prevented by the model (one widget id per workspace).

`CarRuntime.syncWorkspaceNavigation()` makes the map widget's pinned app the car's active
navigation app when its workspace becomes visible (`ui.activeNavAppId`, `settings.lastNavApp`). It
does not disconnect any other navigation app; the other connection stays alive, which is what
preserves a route when switching between “City” (2GIS) and “Highway” (Yandex Navigator).

## 3. App lifecycle while switching workspaces

AARE separates the app's connection from its view already:

- `CarRuntime.connections` holds one `CarAppConnection` per app id for the whole runtime.
- `CarAppScreen`'s `DisposableEffect` only changes *visibility* (`setVisible`), never calls
  `release()` or `stop()`; `onAppPause`/`onAppStop` tell the app it is off screen.
- Media playback lives in `MediaHub`, independent of the screen.
- Notifications, calls, guidance and the assistant are runtime-level, not page-level.

Consequences used by workspaces:

| What the driver does | What happens to the app |
|---|---|
| Switches workspace with a map widget | Map page releases the surface, app is told it is not visible; connection and route stay |
| Opens a shortcut full screen | The same connection becomes visible full screen; no second session |
| Goes home | The workspace page is recomposed; the map surface is re-acquired |
| Switches between two nav apps' workspaces | Both connections exist; only the active one owns the dashboard map/cluster/focus |
| Leaves a workspace with a music widget | `MediaHub` keeps playing; the widget follows `NowPlaying` |

This is why no separate “workspace app” sessions are created: a workspace never creates a
connection, it only asks `CarActions.openApp`/the existing widgets to show one.

**What is not possible:** an app without a persistent session cannot be kept alive merely by being
on an inactive workspace. DashGrid doesn't render inactive workspaces, so there is nothing to keep
alive; a hosted Car App Library app that stops itself is restarted only when opened again, exactly
as in AARE. Likewise, one `CarAppConnection` cannot own two map surfaces; split screen already
solves the two-app case by giving each app its own connection.

### Two navigation apps at once

- Split screen (`settings.splitScreen`, content width ≥ 900 dp) can put a navigation app in the
  second pane (`canOpenBeside` includes `NAVIGATION`); each app has its own connection and surface.
- Only one app is `activeNavAppId`: the dashboard map, cluster turn card and `navigateTo` use it.
  The second app draws only in its pane.
- Android may pause or stop the background app's surface when the car screen shows another app;
  DashGrid doesn't force a second navigation session. Do not expect two apps to keep drawing maps
  while only one is visible.
- Two workspaces with different pinned navigation apps keep both connections; switching makes one
  active without disconnecting the other. Android's background limits still apply to the app that
  no longer has a surface, so a very memory-constrained phone may restart it later — the route is
  restored by the app itself, not by DashGrid.

## 4. Storage, migration and versioning

- One `SharedPreferences` file (`settings`, private to the app) holds `workspaces` as JSON. AARE's
  own settings keys are untouched, and AARE's instance of DashGrid (a different package) has a
  separate file, so the two apps never share settings.
- `WorkspaceStore.load()`:
  1. decodes `workspaces`; if valid, uses it;
  2. otherwise, if the string existed, copies it to `workspaces_broken` and logs;
  3. migrates the old single dashboard (`Settings.dashboardCards`) into the first workspace —
     map/media/calendar/notification/weather/home/car in the user's order, in the preset that fits
     (up to four) or a custom 3×2 grid (up to seven, so no card is lost);
  4. saves the result.
- `lastId` is written whenever the driver switches; `defaultId` is set from the editor. The car
  starts on `last` (falling back to `default`, then the first), which is what makes reconnecting
  feel continuous.
- The JSON has `version`; decoding ignores unknown fields and accepts a newer version, so a
  DashGrid update can add fields without a migration. A future structural change must add a
  version-specific migration in `WorkspaceJson`/`WorkspaceStore`.
- Settings backup (`SettingsBackup.ALLOWED`) includes `workspaces`, so an exported settings file
  carries the workspaces.

## 5. Editor

`Settings → Workspaces` (`ui/WorkspaceSettings.kt`):

- list with default badge, edit, set default, move up/down, duplicate, delete (the last workspace
  cannot be deleted);
- editor: name, layout preset, split axis, capacity hint, the 10×6 grid preview;
- long-press an item to drag it; width/height steppers merge or release cells; overlap is refused
  with a message;
- add widget (one of the seven, not already on the workspace) or add app (built-in apps, installed
  Car App Library apps, media apps found through their MediaBrowserService);
- map/music widget can be pinned to a specific app (`Automatic` clears it);
- switching settings (`swipeAnywhere`, `showBar`) and a button that opens `CarPreviewActivity`
  with the saved workspaces.

Every change writes the whole JSON through `WorkspaceStore`; a connected car reloads it through
the settings listener. The editor keeps the car's `lastId` when saving, so a switch made on the
car while the phone page was open isn't overwritten.

## 6. Performance and resources

- The pager composes the visible page (and neighbours only while a swipe is in progress,
  `beyondViewportPageCount = 0`); inactive workspaces have no composition and no surface.
- Workspace state is a few data classes; no extra dependency was added. App icons/bitmaps come
  from the existing `CarAppEntry`/catalog.
- Recomposition on `NowPlaying`/`weather`/`events` is limited to the composed page(s); the pager
  key keeps page identity (`key(workspace.id)`).
- The editor enumerates the catalog on `Dispatchers.IO`; the car runtime's settings listener runs
  on the main thread and decodes a small JSON only when the key changes.
- Map surfaces are acquired/released by the same counters AARE already used, so no surface or
  app session leaks on page disposal.

## 7. Where the code is

| File | Responsibility |
|---|---|
| `workspace/WorkspaceModel.kt` | Data classes, presets, cells, cyclic math, JSON, migration |
| `workspace/WorkspaceStore.kt` | Load/save/migrate through `Settings` |
| `workspace/WorkspaceCatalog.kt` | Apps the phone editor can add |
| `ui/car/WorkspaceHome.kt` | Pager, bar, page grid, widgets, placeholders |
| `ui/WorkspaceSettings.kt` | Phone editor and pickers |
| `ui/car/CarModel.kt` | Workspace fields in `CarUiState`, `selectWorkspace`/`cycleWorkspace` actions, `workspaceApp` resolver |
| `host/CarRuntime.kt` | Loading, listener, selection, pinned navigation app |
| `ui/car/CarShell.kt`, `DockShell.kt` | Home content replaced by `WorkspaceHome` |
| `ui/MainActivity.kt` | Workspaces settings page, About attribution |
| `ui/theme/CarThemes.kt` | `DashGridTheme` and the brand tokens |
| `app/build.gradle.kts` | Application ID, version, signing, `dashgridApk` |
