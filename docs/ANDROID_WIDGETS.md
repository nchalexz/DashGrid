# Android App Widgets on DashGrid workspaces

DashGrid can place a standard Android **App Widget** — the RemoteViews a widget provider
publishes — directly in the cells of a workspace. The widget is live: it updates itself, its
buttons work, and a scrollable widget scrolls. It is not a screenshot and it is not an icon.

This document describes how it works, what it can and cannot do, and how to add one.

## 1. Architecture

```
phone app process (net.prototux.aare)
├─ widgets/AndroidWidgets           one process-wide AppWidgetHost (HOST_ID "DGRD")
│    ├─ allocate / bind / configure / delete ids
│    ├─ startListening / stopListening          (ref-counted sessions)
│    └─ createView(context, id, provider)       (context = the car screen's Presentation)
├─ widgets/AndroidWidgetCatalog     AppWidgetManager.getInstalledProviders(), labels, previews
├─ ui/AndroidWidgetPicker           phone-side chooser; the system's bind and configure screens
├─ ui/car/AndroidWidgetCell         Compose → AndroidView(AppWidgetHostView) in the cells
└─ workspace/WorkspaceModel         kind = "android_widget", appWidgetId, appWidgetProvider
```

* The host is created with the application context (its package identifies the host to the
  system), but every view is created with the **context passed by Compose**, which inside the car
  screen is AARE's `Presentation` on the private virtual display. That context carries the virtual
  display's density, so a provider's dp values come out at the car screen's scale.
* `AppWidgetHost.createView` is the documented path: it fetches the provider's current RemoteViews
  and registers the view with the host, so later RemoteViews updates reach it. We do not inflate
  RemoteViews by hand and we do not cache screenshots.
* The host listens only while a car runtime (a projection session or the phone preview) is
  running. The listener is reference-counted, so the preview and the car session can coexist and
  the last one to stop closes the binder connection.
* `AppWidgetManager.updateAppWidgetOptions` reports the real cell size in dp every time the cells
  change (a different screen, a rotation, a resize in the editor). A resizable provider reflows to
  those dimensions; DashGrid never scales or stretches the result.

## 2. Data model

A workspace item of the new kind is added to the existing 10 × 6 model, next to the AARE widgets:

```json
{ "id": "<uuid>", "kind": "android_widget", "col": 5, "row": 3, "colSpan": 5, "rowSpan": 3,
  "appWidgetId": 17, "appWidgetProvider": "com.example/.DashboardWidget" }
```

* `appWidgetId` is the id `AppWidgetHost.allocateAppWidgetId()` returned and bound to the
  provider. It is stable across restarts and reconnects; that is what restores the binding.
* `appWidgetProvider` is the flattened provider component, used for the editor's label and to
  recognise a widget whose app was removed.
* The old workspace JSON has neither field; it loads unchanged. A config with an Android-widget
  item but without both fields drops that item rather than leaving a dead cell.
* An `appWidgetId` may appear in **one** item of the whole configuration. Duplicating a workspace
  therefore does not copy its Android widgets (the editor says so): one bound widget cannot be
  owned by two cells. A second instance of the same provider is fine, because it gets its own id.

## 3. Adding a widget

1. Open **Settings → Workspaces** on the phone and edit the workspace.
2. Tap **Android widget** at the bottom of the editor.
3. The list shows every App Widget installed on the phone — the app's icon, the widget's name, its
   minimum size in dp, whether it resizes, and a preview when it has one. Nothing is filtered by a
   built-in list.
4. Choose one. DashGrid allocates an id and asks the system to bind it. The first widget of a given
   provider shows Android's own *"Allow … to access this widget?"* dialog on the **phone**; later
   widgets of that provider are allowed already.
5. If the provider has a configuration screen (a calendar's account, a clock's style…), it opens
   on the phone. Configure it and confirm.
6. The widget is placed in a free area of the workspace, at a size derived from its minimum, and
   appears on the car screen as soon as the page is shown.

Cancelling the permission or the configuration screen releases the id and adds nothing. If the
grid has no room, the widget is released and an explanation is shown.

**Русская инструкция.** Настройки → Рабочие столы → правка нужного стола → кнопка
**«Android-виджет»** → выберите виджет из списка установленных. Системный запрос разрешения и
экран настройки виджета откроются на телефоне. После подтверждения виджет появится в ячейках
рабочего стола на экране автомобиля.

## 4. Lifecycle and failure handling

| Situation | What happens |
|---|---|
| Session starts | `startListening`; pages create views for their widget ids |
| Session ends | `stopListening`; views are kept by the host for the next session |
| Switch workspace / reconnect | only the visible page has a live view; the other pages show a quiet placeholder, so one widget is never shown twice |
| RemoteViews update | the host pushes it into its view; no recomposition needed |
| Resize in the editor | `updateAppWidgetOptions` with the new cells; the provider may reflow |
| Move to another workspace | the item keeps its id and provider; only its page changes |
| Remove the item | `deleteAppWidgetId`; the binding is released |
| Delete the workspace | all its app widget ids are released |
| Cancel add/configure | the allocated id is released |
| Provider app removed or disabled | `getAppWidgetInfo` returns null: the cell shows *Widget unavailable*; the item can be removed in the editor. A package broadcast refreshes the screen |
| Import settings / interrupted add | at the next session start, allocated ids that no workspace refers to are released |
| Provider not allowed to bind | the system permission dialog is used; no Shizuku and no root are required |

## 5. Verification status

* Unit tests cover the model (JSON round-trip, missing fields, one-id-one-item, placement in the
  free grid) and the dock settings; they run on the JVM.
* The code compiles for the release and standalone variants, and lint runs on them.
* **No widget has been run on a real car head unit or on EMUI in this build.** The virtual
  display is AARE's; an AppWidgetHostView is an ordinary Android view and is expected to draw and
  receive touches there, but that is a design expectation to be checked on a phone and a car. The
  testing checklist is in [TESTING.md](../TESTING.md).
* Activity `PendingIntent`s from a widget (a button that opens an app's activity) are sent by the
  system with the phone as the default display. Android may open them on the phone or refuse a
  background activity start. Widgets that use broadcasts or services (most clocks, media buttons,
  toggles) are not affected.

## 6. Target layouts

All four layouts from the task are built in the editor, not hard-coded:

1. *City*: 2GIS as a live pane on the left, Yandex Music (or its Android widget) on the right.
2. *Route*: Yandex Navigator on the left, the radar detector on the right (both are app panes).
3. *Dashboard*: the Cockpit (OBD) app full screen.
4. *Mixed*: navigator on the left (5×6 cells), music top-right (5×3), an Android OBD widget
   bottom-right (5×3) — a custom grid, with the widget added by the picker and resized to 5×3.
   The cyclic pager switches between them as before.

## 7. Deliberate limits

* The widget is drawn exactly as its provider designed it: no tint, no grayscale, no scaling, no
  theme override. If it looks light on a dark car theme, that is the provider's own design.
* A provider that only publishes a fixed layout gets the cells it asks for; DashGrid does not
  distort it to fill a bigger cell.
* The host does not override Android's `uiMode`: a provider that ships night resources follows the
  **phone's** system day/night setting, not the car's own day/night signal. The widget's layout,
  colours and fonts are always its provider's.
* With `swipe anywhere` on, a workspace that contains an Android widget keeps its normal pager
  gestures (the pager leaves the page alone, and the bar and the empty space still switch pages).
  Otherwise a widget's own horizontal gesture (a seek bar, a list) would be swallowed.
* A knob or D-pad reaches the workspace item, but focus inside arbitrary RemoteViews is up to the
  provider's layout; touch is the reliable input.
* Shizuku is not used for widgets. It remains an option only for AARE's other features; the
  standard `AppWidgetHost` + `bindAppWidgetIdIfAllowed` + user confirmation path is enough for
  hosting, and no privilege would make a provider render differently.
