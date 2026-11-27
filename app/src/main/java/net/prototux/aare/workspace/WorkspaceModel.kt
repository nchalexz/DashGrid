package net.prototux.aare.workspace

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** The workspace grid: [WORKSPACE_COLUMNS] × [WORKSPACE_ROWS] cells, shared by every workspace. */
const val WORKSPACE_COLUMNS = 10
const val WORKSPACE_ROWS = 6

/** The smallest item the editor lets a user make. */
const val MIN_ITEM_COLUMNS = 1
const val MIN_ITEM_ROWS = 1

/**
 * The preinstalled workspace that shows VAG Performance Monitor (VPM) as a native DashGrid cockpit.
 *
 * The id is fixed: an update adds the workspace once, and a second load must find the same one
 * instead of adding a copy. The item is a built-in widget (`vpm`), not another app's pane: DashGrid
 * draws the cockpit itself and reads VPM's read-only telemetry snapshot, so VPM stays the only
 * owner of the ELM327 adapter.
 */
const val CAR_WORKSPACE_ID = "workspace-car"
const val CAR_VPM_ITEM_ID = "workspace-car-vpm"

/** The widget id of DashGrid's own VPM cockpit (see `ui/car/vpm`). */
const val VPM_COCKPIT_WIDGET_ID = "vpm"

/**
 * The widget id AARE's own OBD cockpit card used to have. It is no longer offered in the editor:
 * [WorkspaceConfig.withVpmCockpit] replaces a saved item with [VPM_COCKPIT_WIDGET_ID] on load, so an
 * update shows the VPM cockpit where the old AARe one was.
 */
const val LEGACY_OBD_COCKPIT_WIDGET_ID = "car"

/**
 * How a workspace arranges its items. Each preset has a fixed number of slots; [CUSTOM] stores
 * every item's own [WorkspaceRect]. Applying a preset never drops an item: with more items than
 * slots the preset doesn't fit and [Workspace.withLayout] returns null.
 *
 * The ids are stored in the settings: never change one once released.
 */
enum class WorkspaceLayout(val id: String) {
    /** One item, the whole workspace. */
    FULL("full"),

    /** Two items, 50/50. */
    SPLIT_50("split50"),

    /** Two items, the first one 60%, the second 40%. */
    SPLIT_60_40("split60_40"),

    /** Two items, the first one 70%, the second 30%. */
    SPLIT_70_30("split70_30"),

    /** Two items, the first one 30%, the second 70%. */
    SPLIT_30_70("split30_70"),

    /** Three items in three columns (40/30/30 of the grid). */
    THREE("three"),

    /** Four items in a 2×2 grid. */
    FOUR("four"),

    /** The items' own cells (moved, resized or merged in the editor). */
    CUSTOM("custom");

    companion object {
        fun byId(id: String?): WorkspaceLayout = entries.firstOrNull { it.id == id } ?: FULL
    }
}

/** Whether a two-item workspace splits left/right or top/bottom. */
enum class WorkspaceAxis(val id: String) {
    HORIZONTAL("horizontal"),
    VERTICAL("vertical");

    companion object {
        fun byId(id: String?): WorkspaceAxis = entries.firstOrNull { it.id == id } ?: HORIZONTAL
    }
}

/**
 * What an item on a workspace is: a shortcut to an app, one of AARE's dashboard widgets, or a
 * standard Android App Widget hosted through [android.appwidget.AppWidgetHost].
 */
enum class WorkspaceItemKind(val id: String) {
    /** A shortcut tile: the app's icon and label; opening it shows the app full screen. */
    APP("app"),

    /** The app itself, rendered into the item's cells (templates, map, media, a launched app…). */
    PANE("pane"),

    WIDGET("widget"),

    /**
     * A standard Android App Widget: the provider's own RemoteViews, drawn into the item's cells.
     * [WorkspaceItem.appWidgetId] is the id the host allocated and bound to the provider.
     */
    ANDROID_WIDGET("android_widget");

    companion object {
        fun byId(id: String?): WorkspaceItemKind? = entries.firstOrNull { it.id == id }
    }
}

/** An item's cells: [col], [row] from the top left, [colSpan] × [rowSpan] cells. */
data class WorkspaceRect(val col: Int, val row: Int, val colSpan: Int, val rowSpan: Int) {
    val right: Int get() = col + colSpan
    val bottom: Int get() = row + rowSpan

    fun overlaps(other: WorkspaceRect): Boolean =
        col < other.right && other.col < right && row < other.bottom && other.row < bottom

    /** This rect inside the grid, at least one cell wide and tall. */
    fun clamped(): WorkspaceRect {
        val w = colSpan.coerceIn(MIN_ITEM_COLUMNS, WORKSPACE_COLUMNS)
        val h = rowSpan.coerceIn(MIN_ITEM_ROWS, WORKSPACE_ROWS)
        return copy(
            col = col.coerceIn(0, WORKSPACE_COLUMNS - w),
            row = row.coerceIn(0, WORKSPACE_ROWS - h),
            colSpan = w,
            rowSpan = h,
        )
    }

    companion object {
        fun full() = WorkspaceRect(0, 0, WORKSPACE_COLUMNS, WORKSPACE_ROWS)
    }
}

/**
 * One app shortcut, dashboard widget or Android App Widget on a workspace. [appId] is a
 * `CarAppEntry.id` (hosted car app, media app, built-in app…) for an [WorkspaceItemKind.APP], and
 * optionally the app a map or media widget shows for a [WorkspaceItemKind.WIDGET]. [widgetId] is a
 * `DashCard.id`. [appWidgetId] and [appWidgetProvider] belong to an
 * [WorkspaceItemKind.ANDROID_WIDGET]: the id the [android.appwidget.AppWidgetHost] allocated and
 * the flattened component of its provider, so an unavailable provider can be named in the editor.
 * [settings] holds a widget's own settings (for example which navigation app its map shows); this
 * version stores strings only, so a future widget setting needs no schema change.
 */
data class WorkspaceItem(
    val id: String,
    val kind: WorkspaceItemKind,
    val rect: WorkspaceRect,
    val appId: String? = null,
    val widgetId: String? = null,
    val appWidgetId: Int? = null,
    val appWidgetProvider: String? = null,
    val settings: Map<String, String> = emptyMap(),
) {
    companion object {
        /** A new app shortcut, with a fresh id. */
        fun app(appId: String, rect: WorkspaceRect) = WorkspaceItem(
            id = UUID.randomUUID().toString(), kind = WorkspaceItemKind.APP, appId = appId, rect = rect.clamped(),
        )

        /** A new live app pane, with a fresh id. */
        fun pane(appId: String, rect: WorkspaceRect) = WorkspaceItem(
            id = UUID.randomUUID().toString(), kind = WorkspaceItemKind.PANE, appId = appId, rect = rect.clamped(),
        )

        /** A new widget, with a fresh id. */
        fun widget(widgetId: String, rect: WorkspaceRect, appId: String? = null, settings: Map<String, String> = emptyMap()) =
            WorkspaceItem(
                id = UUID.randomUUID().toString(), kind = WorkspaceItemKind.WIDGET, widgetId = widgetId,
                appId = appId, rect = rect.clamped(), settings = settings,
            )

        /** A new Android App Widget, with a fresh item id; [appWidgetId] is already bound by the host. */
        fun androidWidget(appWidgetId: Int, provider: String, rect: WorkspaceRect, appId: String? = null) =
            WorkspaceItem(
                id = UUID.randomUUID().toString(), kind = WorkspaceItemKind.ANDROID_WIDGET,
                appWidgetId = appWidgetId, appWidgetProvider = provider, appId = appId, rect = rect.clamped(),
            )
    }
}

/**
 * One workspace: its identity, its name, its grid layout and its items. Its [index] is its position
 * in [WorkspaceConfig.workspaces]: the order is stored once, in the list, so a reorder can't leave
 * two workspaces claiming the same number.
 */
data class Workspace(
    val id: String,
    val name: String,
    val layout: WorkspaceLayout = WorkspaceLayout.SPLIT_70_30,
    val axis: WorkspaceAxis = WorkspaceAxis.HORIZONTAL,
    val items: List<WorkspaceItem> = emptyList(),
    /** When switching away, release this workspace's car app sessions and surfaces. */
    val closeAppsWhenHidden: Boolean = false,
    /** A tap on a live app's free space opens that app full screen, as tapping a widget card does. */
    val openPaneOnTap: Boolean = false,
) {
    /** A new, empty workspace: a fresh id and a preset wide enough for its first items. */
    companion object {
        fun create(name: String, layout: WorkspaceLayout = WorkspaceLayout.SPLIT_70_30): Workspace =
            Workspace(id = UUID.randomUUID().toString(), name = name.trim().ifBlank { "Workspace" }, layout = layout)

        /**
         * The preinstalled "Car" workspace: the full-screen [VPM_COCKPIT_WIDGET_ID] widget. Its ids
         * are fixed, so adding it is idempotent and a test can name the item.
         */
        fun carPreset(name: String = "Car"): Workspace = Workspace(
            id = CAR_WORKSPACE_ID,
            name = name.trim().ifBlank { "Car" },
            layout = WorkspaceLayout.FULL,
            items = listOf(
                WorkspaceItem(
                    id = CAR_VPM_ITEM_ID,
                    kind = WorkspaceItemKind.WIDGET,
                    rect = WorkspaceRect.full(),
                    widgetId = VPM_COCKPIT_WIDGET_ID,
                ),
            ),
        )
    }

    /** The app ids this workspace already has as shortcuts or as a widget's app. */
    fun appIds(): Set<String> = items.mapNotNull { it.appId }.toSet()

    /** The widget ids on this workspace (a widget appears at most once per workspace). */
    fun widgetIds(): Set<String> = items.mapNotNull { it.widgetId }.toSet()

    /** The Android App Widget ids on this workspace, one per bound provider instance. */
    fun appWidgetIds(): Set<Int> = items.mapNotNull { it.appWidgetId }.toSet()

    /** The cells of the preset's slots, in slot order, for this workspace's axis. */
    fun presetRects(): List<WorkspaceRect> = layout.slots(axis)

    /** How many items the preset holds; [Int.MAX_VALUE] for [WorkspaceLayout.CUSTOM]. */
    val capacity: Int get() = if (layout == WorkspaceLayout.CUSTOM) WORKSPACE_COLUMNS * WORKSPACE_ROWS else presetRects().size

    fun item(id: String?): WorkspaceItem? = items.firstOrNull { it.id == id }

    /**
     * This workspace with [item] added in the next free preset slot, else the first free cells of
     * the grid (a 2×2 item, else 1×1). Null when the grid has no room left: the editor then asks
     * for a roomier layout instead of silently stacking or dropping something.
     */
    fun withAddedItem(item: WorkspaceItem): Workspace? {
        val occupied = items.map { it.rect }
        // In a free grid, an item that asks for several cells (an Android App Widget sized from its
        // provider's minimum) keeps that size where there is room. A preset keeps its own slots:
        // every item there is placed (and reflowed) by the preset, as before.
        val wanted = item.rect
        val preferred = if (layout == WorkspaceLayout.CUSTOM && (wanted.colSpan > 1 || wanted.rowSpan > 1)) {
            wanted.clamped().takeIf { r -> occupied.none { it.overlaps(r) } }
                ?: firstFreeRect(occupied, wanted.colSpan, wanted.rowSpan)
        } else null
        val preset = presetRects()
        val slot = preset.getOrNull(items.size)?.takeIf { r -> occupied.none { it.overlaps(r) } }
        val rect = preferred
            ?: slot
            ?: firstFreeRect(occupied, 2, 2)
            ?: firstFreeRect(occupied, 1, 1)
            ?: return null
        return copy(items = items + item.copy(rect = rect))
    }

    /** This workspace with [item] removed; a preset reflows what's left so no hole stays behind. */
    fun withRemovedItem(itemId: String): Workspace {
        val remaining = items.filterNot { it.id == itemId }
        return if (remaining.size == items.size) this else reflow(remaining)
    }

    /** This workspace with [item]'s cells changed (the layout becomes [WorkspaceLayout.CUSTOM]). */
    fun withItemRect(itemId: String, rect: WorkspaceRect): Workspace? {
        val item = item(itemId) ?: return this
        val clamped = rect.clamped()
        if (items.any { it.id != itemId && it.rect.overlaps(clamped) }) return null
        return copy(layout = WorkspaceLayout.CUSTOM, items = items.map { if (it.id == itemId) it.copy(rect = clamped) else it })
    }

    /** This workspace with [item] changed (for example a widget's app); the id and cells stay. */
    fun withItem(item: WorkspaceItem): Workspace =
        if (items.none { it.id == item.id }) this else copy(items = items.map { if (it.id == item.id) item.copy(rect = item.rect.clamped()) else it })

    /**
     * This workspace with AARe's old OBD cockpit widget ([LEGACY_OBD_COCKPIT_WIDGET_ID]) replaced by
     * DashGrid's VPM cockpit. The first old item keeps its id and cells, so a saved layout doesn't
     * move; if the workspace already has the VPM cockpit, the old item is dropped instead of making a
     * second one. Unchanged when there is nothing to migrate.
     */
    fun withVpmCockpit(): Workspace {
        val legacy = items.filter { it.widgetId == LEGACY_OBD_COCKPIT_WIDGET_ID }
        if (legacy.isEmpty()) return this
        if (items.any { it.widgetId == VPM_COCKPIT_WIDGET_ID }) {
            return legacy.fold(this) { ws, item -> ws.withRemovedItem(item.id) }
        }
        val first = legacy.first()
        return copy(items = items.mapNotNull { item ->
            when {
                item.id == first.id -> item.copy(widgetId = VPM_COCKPIT_WIDGET_ID)
                item.widgetId == LEGACY_OBD_COCKPIT_WIDGET_ID -> null
                else -> item
            }
        })
    }

    /** This workspace with a different preset, or null when its items don't fit that preset. */
    fun withLayout(layout: WorkspaceLayout, axis: WorkspaceAxis = this.axis): Workspace? {
        if (layout == WorkspaceLayout.CUSTOM) return copy(layout = layout, axis = axis)
        val rects = layout.slots(axis)
        if (items.size > rects.size) return null
        val placed = items.mapIndexed { i, item -> item.copy(rect = rects[i]) }
        return copy(layout = layout, axis = axis, items = placed)
    }

    /** Items in their slots for the current preset, in order; [CUSTOM] keeps every cell as it is. */
    private fun reflow(items: List<WorkspaceItem>): Workspace {
        if (layout == WorkspaceLayout.CUSTOM) return copy(items = items)
        val rects = presetRects()
        return copy(items = items.mapIndexed { i, item -> if (i < rects.size) item.copy(rect = rects[i]) else item })
    }

    /** The cells a new item could take at [rect], or the nearest free ones; null when the grid is full. */
    fun freeRectLike(rect: WorkspaceRect): WorkspaceRect? {
        val occupied = items.map { it.rect }
        return if (occupied.none { it.overlaps(rect) }) rect else firstFreeRect(occupied, rect.colSpan, rect.rowSpan)
    }
}

/**
 * Every workspace, their order, the default and the last one shown, plus how switching works. This
 * is what is stored as JSON under one settings key, so a change to the list and the selection is
 * saved atomically.
 */
data class WorkspaceConfig(
    val workspaces: List<Workspace> = emptyList(),
    val defaultId: String? = null,
    val lastId: String? = null,
    /** Swipe anywhere on the workspace, not just on the pager bar. Off: a map keeps its own gestures. */
    val swipeAnywhere: Boolean = false,
    /** Show the pager bar (dots and, optionally, the workspace's name) when there is more than one. */
    val showBar: Boolean = true,
    /** Show the workspace's name on the pager bar (dots stay). */
    val showBarName: Boolean = true,
    val version: Int = VERSION,
) {
    val isEmpty: Boolean get() = workspaces.isEmpty()

    /** The workspace the car should start on: the last one shown, else the default, else the first. */
    fun startWorkspace(): Workspace? =
        workspaces.firstOrNull { it.id == lastId } ?: workspaces.firstOrNull { it.id == defaultId } ?: workspaces.firstOrNull()

    /** [id] if it is still there, else [startWorkspace]'s. */
    fun activeOrStart(id: String?): Workspace? = workspaces.firstOrNull { it.id == id } ?: startWorkspace()

    fun indexOf(id: String?): Int = workspaces.indexOfFirst { it.id == id }

    fun workspace(id: String?): Workspace? = workspaces.firstOrNull { it.id == id }

    fun defaultWorkspace(): Workspace? = workspace(defaultId) ?: workspaces.firstOrNull()

    /** This config with [workspace] added at [index] (clamped to the end), made default if there was none. */
    fun withAdded(workspace: Workspace, index: Int = workspaces.size): WorkspaceConfig {
        val at = index.coerceIn(0, workspaces.size)
        val list = workspaces.toMutableList().apply { add(at, workspace) }
        return copy(workspaces = list, defaultId = defaultId ?: workspace.id)
    }

    /** This config with [workspace] (same id) replaced. */
    fun withUpdated(workspace: Workspace): WorkspaceConfig =
        copy(workspaces = workspaces.map { if (it.id == workspace.id) workspace else it })

    /**
     * This config without [id]: the default moves to a neighbour if it was the default, and the
     * last one also moves if it was the last. Removing the only workspace is refused (null).
     */
    fun without(id: String): WorkspaceConfig? {
        val index = indexOf(id)
        if (index < 0 || workspaces.size <= 1) return null
        val list = workspaces.filterNot { it.id == id }
        val neighbour = list[index.coerceAtMost(list.lastIndex)].id
        return copy(
            workspaces = list,
            defaultId = if (defaultId == id) neighbour else defaultId,
            lastId = if (lastId == id) neighbour else lastId,
        )
    }

    /** This config with [id] moved [by] places (the editor's up/down); out of range, unchanged. */
    fun withMoved(id: String, by: Int): WorkspaceConfig {
        val index = indexOf(id)
        if (index < 0) return this
        val list = workspaces.movedBy(index, by)
        return copy(workspaces = list)
    }

    /** This config with [id] as the default; null when it isn't a workspace. */
    fun withDefault(id: String): WorkspaceConfig =
        if (workspace(id) == null) this else copy(defaultId = id)

    /** This config with [id] as the last shown workspace; null when it isn't a workspace. */
    fun withLast(id: String?): WorkspaceConfig =
        if (id == null || workspace(id) != null) copy(lastId = id) else this

    /** This config with the preinstalled "Car" workspace (VPM cockpit), added exactly once. */
    fun withCarPreset(name: String = "Car"): WorkspaceConfig {
        // The version is the marker: a config at VERSION has already been offered the preset, so
        // deleting the workspace must not bring it back on the next load.
        if (version >= VERSION) return this
        val added = if (workspaces.any { it.id == CAR_WORKSPACE_ID }) this else withAdded(Workspace.carPreset(name))
        return added.copy(version = VERSION)
    }

    /**
     * This config with every saved AARe OBD cockpit widget ([LEGACY_OBD_COCKPIT_WIDGET_ID]) turned
     * into the VPM cockpit, so an update from a build that still offered the old widget shows the
     * cockpit the editor offers now. Idempotent: after the first load there is nothing to replace.
     */
    fun withVpmCockpit(): WorkspaceConfig {
        val migrated = workspaces.map { it.withVpmCockpit() }
        return if (migrated == workspaces) this else copy(workspaces = migrated)
    }

    companion object {
        /** Version 2 introduced the preinstalled "Car" workspace (the VPM cockpit). */
        const val VERSION = 2

        /** What an install with no saved config starts with: Home (map and music) and Car (VPM). */
        fun default(): WorkspaceConfig {
            val workspace = Workspace(
                id = UUID.randomUUID().toString(),
                name = "Home",
                layout = WorkspaceLayout.SPLIT_70_30,
                axis = WorkspaceAxis.HORIZONTAL,
                items = listOf(
                    WorkspaceItem.widget("map", WorkspaceRect(0, 0, 7, 6)),
                    WorkspaceItem.widget("media", WorkspaceRect(7, 0, 3, 6)),
                ),
            )
            val car = Workspace.carPreset()
            return WorkspaceConfig(workspaces = listOf(workspace, car), defaultId = workspace.id, lastId = workspace.id)
        }

        /** An empty config, for a runtime that hasn't loaded the settings yet. */
        fun empty() = WorkspaceConfig()
    }
}

// ------------------------------------------------------------------ cyclic navigation

/** [index] wrapped into `0 until count` (both directions); 0 for an empty list. */
fun cyclicIndex(index: Int, count: Int): Int = if (count <= 0) 0 else ((index % count) + count) % count

/** The index after [current] when stepping [step] workspaces forward (negative: back). */
fun cyclicStep(current: Int, count: Int, step: Int = 1): Int = cyclicIndex(cyclicIndex(current, count) + step, count)

/** The shortest signed distance from [from] to [to] around the ring, in `-(count/2) until count/2`. */
fun cyclicDelta(from: Int, to: Int, count: Int): Int {
    if (count <= 0) return 0
    val forward = cyclicIndex(to - from, count)
    return if (forward > count / 2) forward - count else forward
}

/**
 * The page of a large Virtual pager that shows workspace [realIndex]. The pager's page count is
 * much larger than the workspaces', so a swipe past the last workspace continues into the first:
 * the modulo of the page number is the workspace.
 */
fun virtualPageFor(realIndex: Int, count: Int, center: Int = VIRTUAL_PAGE_CENTER): Int {
    if (count <= 0) return center
    return center - (center % count) + cyclicIndex(realIndex, count)
}

/** The workspace shown at [virtualPage]. */
fun realIndexFor(virtualPage: Int, count: Int): Int = cyclicIndex(virtualPage, count)

/** The virtual page nearest [current] that shows workspace [realIndex] (for the dots). */
fun nearestVirtualPage(realIndex: Int, current: Int, count: Int): Int {
    if (count <= 0) return current
    val delta = cyclicDelta(realIndexFor(current, count), realIndex, count)
    return current + delta
}

/** Far enough from 0 and [Int.MAX_VALUE] that a session never reaches an edge; its middle is stable. */
const val VIRTUAL_PAGE_CENTER = 1_000_000
const val VIRTUAL_PAGE_COUNT = 2_000_000

// ------------------------------------------------------------------ layout presets

/** The slots of [layout] for [axis], in order. [CUSTOM] has none: every item carries its own cells. */
fun WorkspaceLayout.slots(axis: WorkspaceAxis): List<WorkspaceRect> = when (this) {
    WorkspaceLayout.FULL -> listOf(WorkspaceRect.full())
    WorkspaceLayout.SPLIT_50 -> if (axis == WorkspaceAxis.HORIZONTAL)
        listOf(WorkspaceRect(0, 0, 5, 6), WorkspaceRect(5, 0, 5, 6))
    else listOf(WorkspaceRect(0, 0, 10, 3), WorkspaceRect(0, 3, 10, 3))
    WorkspaceLayout.SPLIT_60_40 -> if (axis == WorkspaceAxis.HORIZONTAL)
        listOf(WorkspaceRect(0, 0, 6, 6), WorkspaceRect(6, 0, 4, 6))
    else listOf(WorkspaceRect(0, 0, 10, 4), WorkspaceRect(0, 4, 10, 2))
    WorkspaceLayout.SPLIT_70_30 -> if (axis == WorkspaceAxis.HORIZONTAL)
        listOf(WorkspaceRect(0, 0, 7, 6), WorkspaceRect(7, 0, 3, 6))
    else listOf(WorkspaceRect(0, 0, 10, 4), WorkspaceRect(0, 4, 10, 2))
    WorkspaceLayout.SPLIT_30_70 -> if (axis == WorkspaceAxis.HORIZONTAL)
        listOf(WorkspaceRect(0, 0, 3, 6), WorkspaceRect(3, 0, 7, 6))
    else listOf(WorkspaceRect(0, 0, 10, 2), WorkspaceRect(0, 2, 10, 4))
    WorkspaceLayout.THREE -> listOf(
        WorkspaceRect(0, 0, 4, 6),
        WorkspaceRect(4, 0, 3, 6),
        WorkspaceRect(7, 0, 3, 6),
    )
    WorkspaceLayout.FOUR -> listOf(
        WorkspaceRect(0, 0, 5, 3),
        WorkspaceRect(5, 0, 5, 3),
        WorkspaceRect(0, 3, 5, 3),
        WorkspaceRect(5, 3, 5, 3),
    )
    WorkspaceLayout.CUSTOM -> emptyList()
}

/** This list with the item at [index] moved [by] places (clamped); a helper shared by settings lists. */
internal fun <T> List<T>.movedBy(index: Int, by: Int): List<T> {
    if (index !in indices || by == 0) return this
    val list = toMutableList()
    list.add((index + by).coerceIn(0, lastIndex), list.removeAt(index))
    return list
}

// ------------------------------------------------------------------ cells

/** The first free [w]×[h] cells row by row, or null when the grid has no such hole. */
fun firstFreeRect(occupied: List<WorkspaceRect>, w: Int = 1, h: Int = 1): WorkspaceRect? {
    val width = w.coerceIn(MIN_ITEM_COLUMNS, WORKSPACE_COLUMNS)
    val height = h.coerceIn(MIN_ITEM_ROWS, WORKSPACE_ROWS)
    for (row in 0..WORKSPACE_ROWS - height) {
        for (col in 0..WORKSPACE_COLUMNS - width) {
            val candidate = WorkspaceRect(col, row, width, height)
            if (occupied.none { it.overlaps(candidate) }) return candidate
        }
    }
    return null
}

/** Whether [rect] is inside the grid and at least one cell. */
fun WorkspaceRect.isInsideGrid(): Boolean =
    col >= 0 && row >= 0 && colSpan >= MIN_ITEM_COLUMNS && rowSpan >= MIN_ITEM_ROWS &&
        col + colSpan <= WORKSPACE_COLUMNS && row + rowSpan <= WORKSPACE_ROWS

/**
 * The cells of a landscape rect on a portrait screen: the grid is turned 90° so a left/right split
 * becomes top/bottom and a 70/30 split keeps its 70% side, now above or below. The portrait grid
 * is [WORKSPACE_ROWS] columns and [WORKSPACE_COLUMNS] rows.
 */
fun WorkspaceRect.portraitRect(): WorkspaceRect =
    WorkspaceRect(
        col = row,
        row = WORKSPACE_COLUMNS - col - colSpan,
        colSpan = rowSpan,
        rowSpan = colSpan,
    )

// ------------------------------------------------------------------ JSON

/**
 * WorkspaceConfig as JSON, in the one settings string. Parsing is deliberately forgiving: a
 * config written by a newer DashGrid, or hand-edited, loads what it can instead of losing every
 * workspace. Items that overlap are moved to free cells, duplicate ids are regenerated, and
 * unknown fields are kept in the file only by future writers, not by this version.
 */
object WorkspaceJson {
    fun encode(config: WorkspaceConfig): String = JSONObject().apply {
        put("version", config.version)
        config.defaultId?.let { put("default", it) }
        config.lastId?.let { put("last", it) }
        put("swipeAnywhere", config.swipeAnywhere)
        put("showBar", config.showBar)
        put("showName", config.showBarName)
        put("workspaces", JSONArray().apply {
            config.workspaces.forEach { ws ->
                put(JSONObject().apply {
                    put("id", ws.id)
                    put("name", ws.name)
                    put("layout", ws.layout.id)
                    put("axis", ws.axis.id)
                    put("closeAppsWhenHidden", ws.closeAppsWhenHidden)
                    put("openPaneOnTap", ws.openPaneOnTap)
                    put("items", JSONArray().apply {
                        ws.items.forEach { item ->
                            put(JSONObject().apply {
                                put("id", item.id)
                                put("kind", item.kind.id)
                                put("col", item.rect.col)
                                put("row", item.rect.row)
                                put("colSpan", item.rect.colSpan)
                                put("rowSpan", item.rect.rowSpan)
                                item.appId?.let { put("app", it) }
                                item.widgetId?.let { put("widget", it) }
                                item.appWidgetId?.let { put("appWidgetId", it) }
                                item.appWidgetProvider?.let { put("appWidgetProvider", it) }
                                if (item.settings.isNotEmpty()) put("settings", JSONObject().apply {
                                    item.settings.forEach { (k, v) -> put(k, v) }
                                })
                            })
                        }
                    })
                })
            }
        })
    }.toString()

    /** The config in [text], or null when it holds no workspace at all (an empty or unrelated string). */
    fun decode(text: String?): WorkspaceConfig? {
        if (text.isNullOrBlank()) return null
        val root = runCatching { JSONObject(text) }.getOrNull() ?: return null
        // A config written before the field existed is treated as pre-VPM (version 1), so the
        // preinstalled Car workspace is offered to it too.
        val version = root.optInt("version", 1).coerceAtLeast(1)
        val array = root.optJSONArray("workspaces") ?: return null
        val workspaces = ArrayList<Workspace>(array.length())
        for (i in 0 until array.length()) {
            decodeWorkspace(array.optJSONObject(i))?.let(workspaces::add)
        }
        if (workspaces.isEmpty()) return null
        val ids = workspaces.map { it.id }.toSet()
        val default = root.optString("default").takeIf { it in ids } ?: workspaces.first().id
        val last = root.optString("last").takeIf { it in ids } ?: default
        return WorkspaceConfig(
            workspaces = sanitizeWorkspaces(workspaces),
            defaultId = default,
            lastId = last,
            swipeAnywhere = root.optBoolean("swipeAnywhere", false),
            showBar = root.optBoolean("showBar", true),
            showBarName = root.optBoolean("showName", true),
            version = version,
        )
    }

    private fun decodeWorkspace(json: JSONObject?): Workspace? {
        if (json == null) return null
        val id = json.optString("id").ifBlank { UUID.randomUUID().toString() }
        val name = json.optString("name").trim().ifBlank { "Workspace" }
        val layout = WorkspaceLayout.byId(json.optString("layout"))
        val axis = WorkspaceAxis.byId(json.optString("axis"))
        val closeAppsWhenHidden = json.optBoolean("closeAppsWhenHidden", false)
        val openPaneOnTap = json.optBoolean("openPaneOnTap", false)
        val itemsJson = json.optJSONArray("items")
        val items = ArrayList<WorkspaceItem>(itemsJson?.length() ?: 0)
        if (itemsJson != null) for (i in 0 until itemsJson.length()) {
            decodeItem(itemsJson.optJSONObject(i))?.let(items::add)
        }
        return Workspace(id = id, name = name, layout = layout, axis = axis, items = items,
            closeAppsWhenHidden = closeAppsWhenHidden, openPaneOnTap = openPaneOnTap)
    }

    private fun decodeItem(json: JSONObject?): WorkspaceItem? {
        if (json == null) return null
        val kind = WorkspaceItemKind.byId(json.optString("kind")) ?: return null
        val app = json.optString("app").takeIf { it.isNotBlank() }
        val widget = json.optString("widget").takeIf { it.isNotBlank() }
        if ((kind == WorkspaceItemKind.APP || kind == WorkspaceItemKind.PANE) && app == null) return null
        if (kind == WorkspaceItemKind.WIDGET && widget == null) return null
        // A widget whose id or provider didn't survive the file can't be drawn (and must not take
        // cells the user can't see): it is dropped here rather than left as a dead item.
        val appWidgetId = json.optInt("appWidgetId", -1).takeIf { it >= 0 }
        val appWidgetProvider = json.optString("appWidgetProvider").takeIf { it.isNotBlank() }
        if (kind == WorkspaceItemKind.ANDROID_WIDGET && (appWidgetId == null || appWidgetProvider == null)) return null
        val rect = WorkspaceRect(
            col = json.optInt("col", 0),
            row = json.optInt("row", 0),
            colSpan = json.optInt("colSpan", 2),
            rowSpan = json.optInt("rowSpan", 2),
        )
        val settings = HashMap<String, String>()
        json.optJSONObject("settings")?.let { obj ->
            obj.keys().forEach { key -> obj.optString(key).takeIf { it.isNotEmpty() }?.let { settings[key] = it } }
        }
        return WorkspaceItem(
            id = json.optString("id").ifBlank { UUID.randomUUID().toString() },
            kind = kind,
            rect = rect,
            appId = app,
            widgetId = widget,
            appWidgetId = appWidgetId,
            appWidgetProvider = appWidgetProvider,
            settings = settings,
        )
    }

    /**
     * A loaded config made safe to draw: unique workspace ids and item ids, at most one shortcut
     * per app and one of each widget per workspace, no overlapping cells, and every rect inside
     * the grid. An item that can't be placed (the grid is full) is left out rather than drawn on
     * top of another.
     */
    internal fun sanitizeWorkspaces(workspaces: List<Workspace>): List<Workspace> {
        val workspaceIds = HashSet<String>()
        // An App Widget id belongs to one item in the whole config: duplicating a workspace copies
        // its items, but the copy must not own the same bound widget (one view, two cells).
        val usedAppWidgets = HashSet<Int>()
        return workspaces.mapNotNull { ws ->
            val id = ws.id.ifBlank { UUID.randomUUID().toString() }
            if (!workspaceIds.add(id)) return@mapNotNull null
            val usedItemIds = HashSet<String>()
            val usedApps = HashSet<String>()
            val usedWidgets = HashSet<String>()
            val placed = ArrayList<WorkspaceItem>(ws.items.size)
            val occupied = ArrayList<WorkspaceRect>(ws.items.size)
            for (item in ws.items) {
                if ((item.kind == WorkspaceItemKind.APP || item.kind == WorkspaceItemKind.PANE) &&
                    item.appId != null && !usedApps.add(item.appId)) continue
                if (item.kind == WorkspaceItemKind.WIDGET && item.widgetId != null && !usedWidgets.add(item.widgetId)) continue
                if (item.kind == WorkspaceItemKind.ANDROID_WIDGET) {
                    val widgetId = item.appWidgetId ?: continue
                    if (!usedAppWidgets.add(widgetId)) continue
                }
                var rect = item.rect.clamped()
                if (occupied.any { it.overlaps(rect) }) rect = firstFreeRect(occupied, rect.colSpan, rect.rowSpan) ?: continue
                var itemId = item.id.ifBlank { UUID.randomUUID().toString() }
                if (!usedItemIds.add(itemId)) { itemId = UUID.randomUUID().toString(); usedItemIds.add(itemId) }
                occupied.add(rect)
                placed.add(item.copy(id = itemId, rect = rect))
            }
            ws.copy(id = id, items = placed)
        }
    }
}

// ------------------------------------------------------------------ migration

/**
 * The first workspace for an install that only had AARE's single dashboard: its cards become
 * widgets in the order they had, in the preset that fits them. [widgetIds] are `DashCard` ids in
 * the user's old order; unknown ids are dropped by the caller.
 */
fun migrateDashboard(widgetIds: List<String>, name: String = "Home"): WorkspaceConfig {
    val ids = widgetIds.distinct().ifEmpty { listOf("map", "media") }
    // A preset holds at most four items. The old dashboard allowed seven cards, and none of them
    // may be lost in the migration: more than four become a custom grid of 3×2 cells (three per
    // row, two rows), which the 10×6 grid holds up to nine of.
    val layout = when (ids.size) {
        0, 1 -> WorkspaceLayout.FULL
        2 -> WorkspaceLayout.SPLIT_70_30
        3 -> WorkspaceLayout.THREE
        4 -> WorkspaceLayout.FOUR
        else -> WorkspaceLayout.CUSTOM
    }
    val items: List<WorkspaceItem> = if (layout == WorkspaceLayout.CUSTOM) {
        val occupied = ArrayList<WorkspaceRect>(ids.size)
        ids.mapNotNull { id ->
            val rect = firstFreeRect(occupied, 3, 2) ?: firstFreeRect(occupied, 2, 2) ?: return@mapNotNull null
            occupied.add(rect)
            WorkspaceItem.widget(id, rect)
        }
    } else {
        val rects = layout.slots(WorkspaceAxis.HORIZONTAL)
        ids.mapIndexed { i, id -> WorkspaceItem.widget(id, rects[i]) }
    }
    val workspace = Workspace(
        id = UUID.randomUUID().toString(),
        name = name.ifBlank { "Home" },
        layout = layout,
        items = items,
    )
    // Version 1 is the pre-VPM-cockpit config: WorkspaceStore then offers the "Car" preset once.
    return WorkspaceConfig(
        workspaces = listOf(workspace),
        defaultId = workspace.id,
        lastId = workspace.id,
        version = 1,
    )
}
