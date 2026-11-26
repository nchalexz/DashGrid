package net.prototux.aare.ui

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.ui.CarPreviewActivity
import net.prototux.aare.workspace.WORKSPACE_COLUMNS
import net.prototux.aare.workspace.WORKSPACE_ROWS
import net.prototux.aare.workspace.Workspace
import net.prototux.aare.workspace.WorkspaceAppChoice
import net.prototux.aare.workspace.WorkspaceAxis
import net.prototux.aare.workspace.WorkspaceCatalog
import net.prototux.aare.workspace.WorkspaceConfig
import net.prototux.aare.workspace.WorkspaceItem
import net.prototux.aare.workspace.WorkspaceItemKind
import net.prototux.aare.workspace.WorkspaceLayout
import net.prototux.aare.workspace.WorkspaceRect
import net.prototux.aare.workspace.WorkspaceStore
import net.prototux.aare.widgets.AndroidWidgets
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The phone's Workspaces page: the list of workspaces, the editor for one of them and the
 * switching settings. The car screen stays a viewer — everything that changes the layout, the
 * names or the items is here, and a running connection picks each save up through the settings
 * listener (CarRuntime.reloadWorkspaces).
 *
 * The editor's grid is the same 10×6 logical grid the car draws (turned for portrait screens).
 * Long-press an item to drag it; the selected item's width and height merge or give back cells.
 */
@Composable
internal fun WorkspacesSettings(
    settings: Settings,
    initialEditingId: String? = null,
    /** A config to show instead of the saved one (headless previews); changes stay in this composition. */
    initialConfig: WorkspaceConfig? = null,
) {
    val ctx = LocalContext.current
    val store = remember(settings) { WorkspaceStore(settings, ctx.getString(R.string.workspaces_default_home)) }
    var config by remember(store, initialConfig) { mutableStateOf(initialConfig ?: store.load()) }
    var editingId by rememberSaveable { mutableStateOf(initialEditingId) }
    var deleteId by remember { mutableStateOf<String?>(null) }
    var addWidgetTo by remember { mutableStateOf<String?>(null) }
    var androidWidgetTo by remember { mutableStateOf<String?>(null) }
    var appPicker by remember { mutableStateOf<AppPickerRequest?>(null) }
    val apps by produceState(initialValue = emptyList<WorkspaceAppChoice>(), settings) {
        value = withContext(Dispatchers.IO) { WorkspaceCatalog.apps(ctx) }
    }
    val appLabels = remember(apps) { apps.associate { it.id to it.label } }

    fun commit(next: WorkspaceConfig) {
        if (next == config) return
        if (initialConfig != null) {
            config = next
            return
        }
        // The car keeps the last workspace shown in the same file; don't overwrite a switch made
        // while this page was open with the value it was opened with.
        val savedLast = store.load().lastId
        val merged = if (savedLast != null && next.lastId != savedLast) next.copy(lastId = savedLast) else next
        config = merged
        store.save(merged)
    }

    fun workspaceById(id: String): Workspace? = config.workspace(id)
    fun update(workspace: Workspace) = commit(config.withUpdated(workspace))
    /** Moves [item] to [targetId], keeping its id and (for an Android widget) its bound appWidgetId. */
    fun moveItem(item: WorkspaceItem, targetId: String) {
        val source = config.workspaces.firstOrNull { ws -> ws.items.any { it.id == item.id } } ?: return
        val target = config.workspace(targetId) ?: return
        if (source.id == target.id) return
        if (item.kind != WorkspaceItemKind.ANDROID_WIDGET && item.appId != null && item.appId in target.appIds() ||
            item.kind == WorkspaceItemKind.WIDGET && item.widgetId != null && item.widgetId in target.widgetIds()
        ) {
            Toast.makeText(ctx, R.string.workspaces_move_duplicate, Toast.LENGTH_SHORT).show()
            return
        }
        val updatedSource = source.withRemovedItem(item.id)
        val updatedTarget = target.withAddedItem(item)
        if (updatedTarget == null) {
            Toast.makeText(ctx, R.string.workspaces_no_space, Toast.LENGTH_SHORT).show()
            return
        }
        commit(config.withUpdated(updatedSource).withUpdated(updatedTarget))
    }
    val editing = config.workspace(editingId)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Section(stringResource(R.string.workspaces_section_list)) {
            Text(stringResource(R.string.workspaces_intro), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            config.workspaces.forEachIndexed { index, workspace ->
                WorkspaceRow(
                    workspace = workspace,
                    index = index,
                    count = config.workspaces.size,
                    isDefault = workspace.id == config.defaultId,
                    itemSummary = stringResource(R.string.workspaces_item_count, workspace.items.size),
                    onEdit = { editingId = if (editingId == workspace.id) null else workspace.id },
                    onUp = { commit(config.withMoved(workspace.id, -1)) },
                    onDown = { commit(config.withMoved(workspace.id, 1)) },
                    onDuplicate = {
                        // An App Widget id belongs to one item: a copy would show the same bound
                        // widget twice (one live view) and either copy's removal would release the
                        // other's id. The copy keeps everything else and says what was left out.
                        val droppedWidgets = workspace.items.count { it.kind == WorkspaceItemKind.ANDROID_WIDGET }
                        val copy = workspace.copy(
                            id = UUID.randomUUID().toString(),
                            name = ctx.getString(R.string.workspaces_duplicate_name, workspace.name),
                            items = workspace.items.filterNot { it.kind == WorkspaceItemKind.ANDROID_WIDGET }
                                .map { it.copy(id = UUID.randomUUID().toString()) },
                        )
                        if (droppedWidgets > 0) Toast.makeText(ctx, R.string.widget_duplicate_dropped, Toast.LENGTH_LONG).show()
                        commit(config.withAdded(copy, index + 1))
                        editingId = copy.id
                    },
                    onDefault = { commit(config.withDefault(workspace.id).withLast(workspace.id)) },
                    onDelete = { deleteId = workspace.id },
                )
                if (index != config.workspaces.lastIndex) HorizontalDivider(Modifier.padding(vertical = 4.dp))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = {
                val created = Workspace.create(ctx.getString(R.string.workspaces_default_name, config.workspaces.size + 1))
                commit(config.withAdded(created))
                editingId = created.id
            }) {
                Icon(Icons.Rounded.Add, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.workspaces_add))
            }
        }

        if (editing != null) {
            Section(stringResource(R.string.workspaces_section_edit, editing.name)) {
                WorkspaceEditor(
                    context = ctx,
                    workspace = editing,
                    appLabels = appLabels,
                    otherWorkspaces = config.workspaces.filter { it.id != editing.id },
                    onChange = ::update,
                    onMoveItem = ::moveItem,
                    onClose = { editingId = null },
                    onAddWidget = { addWidgetTo = editing.id },
                    onAddAndroidWidget = { androidWidgetTo = editing.id },
                    onPickApp = { appPicker = it },
                )
            }
        }

        Section(stringResource(R.string.workspaces_section_switching)) {
            Toggle(
                stringResource(R.string.workspaces_swipe_anywhere_title),
                stringResource(R.string.workspaces_swipe_anywhere_subtitle),
                config.swipeAnywhere,
            ) { enabled -> commit(config.copy(swipeAnywhere = enabled)) }
            Spacer(Modifier.height(8.dp))
            Toggle(
                stringResource(R.string.workspaces_show_bar_title),
                stringResource(R.string.workspaces_show_bar_subtitle),
                config.showBar,
            ) { shown -> commit(config.copy(showBar = shown)) }
            Spacer(Modifier.height(8.dp))
            Toggle(
                stringResource(R.string.workspaces_show_name_title),
                stringResource(R.string.workspaces_show_name_subtitle),
                config.showBarName,
            ) { shown -> commit(config.copy(showBarName = shown)) }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { ctx.startActivity(android.content.Intent(ctx, CarPreviewActivity::class.java)) }) {
                Icon(Icons.Rounded.Visibility, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.workspaces_preview))
            }
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.workspaces_preview_hint), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    deleteId?.let { id ->
        val name = config.workspace(id)?.name ?: return@let
        AlertDialog(
            onDismissRequest = { deleteId = null },
            title = { Text(stringResource(R.string.workspaces_delete_title, name)) },
            text = { Text(stringResource(R.string.workspaces_delete_text)) },
            confirmButton = {
                TextButton(onClick = {
                    config.without(id)?.let { next ->
                        // Only once the removal is certain: the last workspace is refused above.
                        config.workspace(id)?.items?.forEach { item -> item.appWidgetId?.let(AndroidWidgets::delete) }
                        commit(next)
                        if (editingId == id) editingId = next.startWorkspace()?.id
                    }
                    deleteId = null
                }) { Text(stringResource(R.string.workspaces_delete)) }
            },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }

    addWidgetTo?.let { workspaceId ->
        val workspace = config.workspace(workspaceId)
        if (workspace == null) { addWidgetTo = null; return@let }
        val available = widgetIds.filter { it !in workspace.widgetIds() }
        AlertDialog(
            onDismissRequest = { addWidgetTo = null },
            title = { Text(stringResource(R.string.workspaces_choose_widget)) },
            text = {
                if (available.isEmpty()) Text(stringResource(R.string.workspaces_no_widgets_left))
                else LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(available) { id ->
                        ChoiceRow(icon = widgetIcon(id), label = stringResource(widgetLabel(id))) {
                            val added = workspace.withAddedItem(WorkspaceItem.widget(id, WorkspaceRect(0, 0, 1, 1)))
                            if (added == null) {
                                Toast.makeText(ctx, R.string.workspaces_no_space, Toast.LENGTH_SHORT).show()
                            } else {
                                update(added)
                            }
                            addWidgetTo = null
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { addWidgetTo = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }

    appPicker?.let { request ->
        val workspace = request.workspaceId?.let(::workspaceById)
        val choices = if (request.navigationOnly) apps.filter { it.navigation } else apps
        AlertDialog(
            onDismissRequest = { appPicker = null },
            title = { Text(stringResource(request.titleRes)) },
            text = {
                LazyColumn(Modifier.heightIn(max = 460.dp)) {
                    if (request.allowAutomatic) item {
                        ChoiceRow(icon = null, label = stringResource(R.string.workspaces_app_automatic)) {
                            request.apply(null)
                            appPicker = null
                        }
                    }
                    items(choices, key = { it.id }) { choice ->
                        ChoiceRow(icon = null, label = choice.label, bitmapId = choice.icon) {
                            request.apply(choice.id)
                            appPicker = null
                        }
                    }
                    if (choices.isEmpty()) item {
                        Text(stringResource(R.string.workspaces_no_apps), modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { appPicker = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }

    androidWidgetTo?.let { workspaceId ->
        val workspace = config.workspace(workspaceId)
        if (workspace == null) { androidWidgetTo = null; return@let }
        AndroidWidgetPicker(
            onDismiss = { androidWidgetTo = null },
            onAdd = { choice, appWidgetId ->
                val (cols, rows) = defaultWidgetRect(choice.minWidthDp, choice.minHeightDp)
                val item = WorkspaceItem.androidWidget(appWidgetId, choice.providerKey, WorkspaceRect(0, 0, cols, rows))
                val added = workspace.withAddedItem(item)
                if (added == null) false else { update(added); true }
            },
        )
    }
}

/** What an app picker is for, and what it does with the chosen id (null = automatic). */
private data class AppPickerRequest(
    val workspaceId: String?,
    val titleRes: Int,
    val navigationOnly: Boolean = false,
    val allowAutomatic: Boolean = false,
    val apply: (String?) -> Unit,
)

/** The workspace list's row: name, summary and the actions menu. */
@Composable
private fun WorkspaceRow(
    workspace: Workspace,
    index: Int,
    count: Int,
    isDefault: Boolean,
    itemSummary: String,
    onEdit: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onDuplicate: () -> Unit,
    onDefault: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clickable(onClick = onEdit).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(workspace.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (isDefault) {
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 8.dp, vertical = 2.dp)) {
                        Text(stringResource(R.string.workspaces_default_badge), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
            Text(itemSummary + " · " + stringResource(layoutLabel(workspace.layout)),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, stringResource(R.string.workspaces_more)) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.workspaces_edit)) }, onClick = { menu = false; onEdit() })
                DropdownMenuItem(text = { Text(stringResource(R.string.workspaces_set_default)) }, enabled = !isDefault,
                    onClick = { menu = false; onDefault() }, leadingIcon = { Icon(Icons.Rounded.Star, null) })
                DropdownMenuItem(text = { Text(stringResource(R.string.workspaces_up)) }, enabled = index > 0,
                    onClick = { menu = false; onUp() }, leadingIcon = { Icon(Icons.Rounded.ArrowUpward, null) })
                DropdownMenuItem(text = { Text(stringResource(R.string.workspaces_down)) }, enabled = index < count - 1,
                    onClick = { menu = false; onDown() }, leadingIcon = { Icon(Icons.Rounded.ArrowDownward, null) })
                DropdownMenuItem(text = { Text(stringResource(R.string.workspaces_duplicate)) },
                    onClick = { menu = false; onDuplicate() }, leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) })
                DropdownMenuItem(text = { Text(stringResource(R.string.workspaces_delete_title_short)) }, enabled = count > 1,
                    onClick = { menu = false; onDelete() }, leadingIcon = { Icon(Icons.Rounded.Delete, null) })
            }
        }
    }
}

/** The editor for one workspace: name, layout, the grid, the selected item and the add buttons. */
@Composable
private fun WorkspaceEditor(
    context: android.content.Context,
    workspace: Workspace,
    appLabels: Map<String, String>,
    otherWorkspaces: List<Workspace>,
    onChange: (Workspace) -> Unit,
    onMoveItem: (WorkspaceItem, String) -> Unit,
    onClose: () -> Unit,
    onAddWidget: () -> Unit,
    onAddAndroidWidget: () -> Unit,
    onPickApp: (AppPickerRequest) -> Unit,
) {
    var name by remember(workspace.id, workspace.name) { mutableStateOf(workspace.name) }
    var selectedId by remember(workspace.id) { mutableStateOf<String?>(null) }
    val selected = workspace.item(selectedId)
    val toast = { res: Int -> Toast.makeText(context, res, Toast.LENGTH_SHORT).show() }

    fun resize(item: WorkspaceItem, dW: Int, dH: Int) {
        val rect = item.rect.copy(
            colSpan = (item.rect.colSpan + dW).coerceIn(1, WORKSPACE_COLUMNS),
            rowSpan = (item.rect.rowSpan + dH).coerceIn(1, WORKSPACE_ROWS),
        )
        val next = workspace.withItemRect(item.id, rect)
        if (next == null) toast(R.string.workspaces_no_space) else onChange(next)
    }

    fun move(item: WorkspaceItem, col: Int, row: Int) {
        val rect = item.rect.copy(
            col = col.coerceIn(0, WORKSPACE_COLUMNS - item.rect.colSpan),
            row = row.coerceIn(0, WORKSPACE_ROWS - item.rect.rowSpan),
        )
        if (rect == item.rect) return
        val next = workspace.withItemRect(item.id, rect)
        if (next == null) toast(R.string.workspaces_no_space) else onChange(next)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.workspaces_editing), style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(R.string.common_close)) }
        }
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.workspaces_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (name.trim().isNotEmpty() && name.trim() != workspace.name) {
                    IconButton(onClick = { onChange(workspace.copy(name = name.trim())) }) {
                        Icon(Icons.Rounded.Check, stringResource(R.string.workspaces_rename))
                    }
                }
            },
        )
        Select(
            title = stringResource(R.string.workspaces_layout),
            options = WorkspaceLayout.entries.map { it.id to stringResource(layoutLabel(it)) },
            selected = workspace.layout.id,
        ) { id ->
            val layout = WorkspaceLayout.byId(id)
            val next = workspace.withLayout(layout)
            if (next == null) toast(R.string.workspaces_layout_too_small) else onChange(next)
        }
        if (workspace.layout in splitLayouts) {
            Select(
                title = stringResource(R.string.workspaces_axis),
                options = listOf(
                    WorkspaceAxis.HORIZONTAL.id to stringResource(R.string.workspaces_axis_horizontal),
                    WorkspaceAxis.VERTICAL.id to stringResource(R.string.workspaces_axis_vertical),
                ),
                selected = workspace.axis.id,
            ) { id -> workspace.withLayout(workspace.layout, WorkspaceAxis.byId(id))?.let(onChange) }
        }
        Text(
            if (workspace.layout == WorkspaceLayout.CUSTOM) stringResource(R.string.workspaces_custom_hint)
            else stringResource(R.string.workspaces_capacity, workspace.items.size, workspace.capacity),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Toggle(
            stringResource(R.string.workspaces_close_hidden_title),
            stringResource(R.string.workspaces_close_hidden_subtitle),
            workspace.closeAppsWhenHidden,
        ) { close -> onChange(workspace.copy(closeAppsWhenHidden = close)) }
        Toggle(
            stringResource(R.string.workspaces_pane_tap_title),
            stringResource(R.string.workspaces_pane_tap_subtitle),
            workspace.openPaneOnTap,
        ) { open -> onChange(workspace.copy(openPaneOnTap = open)) }

        SelectionSummary(workspace, selected, appLabels)
        WorkspaceGrid(
            workspace = workspace,
            appLabels = appLabels,
            selectedId = selectedId,
            onSelect = { selectedId = it },
            onMove = ::move,
        )

        if (selected != null) {
            ItemInspector(
                item = selected,
                appLabels = appLabels,
                onResize = { dW, dH -> resize(selected, dW, dH) },
                otherWorkspaces = otherWorkspaces,
                onMoveTo = { targetId -> onMoveItem(selected, targetId) },
                onRemove = {
                    selected.appWidgetId?.let(AndroidWidgets::delete)
                    onChange(workspace.withRemovedItem(selected.id))
                    selectedId = null
                },
                onPickApp = { automatic ->
                    onPickApp(AppPickerRequest(
                        workspaceId = workspace.id,
                        titleRes = R.string.workspaces_choose_app,
                        navigationOnly = selected.widgetId == "map",
                        allowAutomatic = automatic,
                        apply = { appId ->
                            val item = workspace.item(selected.id) ?: return@AppPickerRequest
                            onChange(workspace.withItem(item.copy(appId = appId)))
                        },
                    ))
                },
            )
        } else {
            Text(stringResource(R.string.workspaces_tap_item), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Text(stringResource(R.string.workspaces_pane_hint), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onAddWidget) {
                Icon(Icons.Rounded.Add, null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.workspaces_add_widget))
            }
            OutlinedButton(onClick = onAddAndroidWidget) {
                Icon(Icons.Rounded.Widgets, null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.workspaces_add_android_widget))
            }
            OutlinedButton(onClick = {
                onPickApp(AppPickerRequest(
                    workspaceId = workspace.id,
                    titleRes = R.string.workspaces_choose_app,
                    apply = { appId ->
                        if (appId != null && appId !in workspace.appIds()) {
                            val added = workspace.withAddedItem(WorkspaceItem.pane(appId, WorkspaceRect(0, 0, 1, 1)))
                            if (added == null) Toast.makeText(context, R.string.workspaces_no_space, Toast.LENGTH_SHORT).show()
                            else onChange(added)
                        }
                    },
                ))
            }) {
                Icon(Icons.Rounded.Apps, null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.workspaces_add_pane))
            }
            OutlinedButton(onClick = {
                onPickApp(AppPickerRequest(
                    workspaceId = workspace.id,
                    titleRes = R.string.workspaces_choose_app,
                    apply = { appId ->
                        if (appId != null && appId !in workspace.appIds()) {
                            val added = workspace.withAddedItem(WorkspaceItem.app(appId, WorkspaceRect(0, 0, 1, 1)))
                            if (added == null) Toast.makeText(context, R.string.workspaces_no_space, Toast.LENGTH_SHORT).show()
                            else onChange(added)
                        }
                    },
                ))
            }) {
                Icon(Icons.Rounded.Add, null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.workspaces_add_shortcut))
            }
        }
    }
}

/** The line above the grid: what is selected, or what the workspace holds. */
@Composable
private fun SelectionSummary(workspace: Workspace, selected: WorkspaceItem?, appLabels: Map<String, String>) {
    val text = if (selected != null) {
        itemLabel(selected, appLabels) + " · " + stringResource(
            R.string.workspaces_item_geometry,
            selected.rect.colSpan, selected.rect.rowSpan, selected.rect.col, selected.rect.row,
        )
    } else {
        stringResource(R.string.workspaces_items_count, workspace.items.size)
    }
    Text(text, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
}

/**
 * The workspace on its 10×6 grid, as the car draws it (one landscape preview; the car turns it for
 * a portrait screen). Long-press an item to drag it to other cells.
 */
@Composable
private fun WorkspaceGrid(
    workspace: Workspace,
    appLabels: Map<String, String>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onMove: (WorkspaceItem, Int, Int) -> Unit,
) {
    val gap = 6.dp
    val shape = RoundedCornerShape(16.dp)
    val outline = MaterialTheme.colorScheme.outlineVariant
    BoxWithConstraints(
        Modifier.fillMaxWidth().aspectRatio(WORKSPACE_COLUMNS.toFloat() / WORKSPACE_ROWS)
            .clip(shape).background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, outline, shape).padding(6.dp),
    ) {
        val cellWidth = (maxWidth - gap * (WORKSPACE_COLUMNS - 1)) / WORKSPACE_COLUMNS
        val cellHeight = (maxHeight - gap * (WORKSPACE_ROWS - 1)) / WORKSPACE_ROWS
        Canvas(Modifier.fillMaxSize()) {
            val stepX = ((gap + cellWidth).toPx())
            val stepY = ((gap + cellHeight).toPx())
            val line = outline.copy(alpha = 0.25f)
            for (c in 0..WORKSPACE_COLUMNS) drawLine(line, Offset(c * stepX, 0f), Offset(c * stepX, size.height), strokeWidth = 1f)
            for (r in 0..WORKSPACE_ROWS) drawLine(line, Offset(0f, r * stepY), Offset(size.width, r * stepY), strokeWidth = 1f)
        }
        workspace.items.forEach { item ->
            key(item.id) {
            val selected = item.id == selectedId
            var drag by remember(item.id) { mutableStateOf(Offset.Zero) }
            Box(
                Modifier
                    .offset(x = (cellWidth + gap) * item.rect.col, y = (cellHeight + gap) * item.rect.row)
                    .size(
                        width = cellWidth * item.rect.colSpan + gap * (item.rect.colSpan - 1).coerceAtLeast(0),
                        height = cellHeight * item.rect.rowSpan + gap * (item.rect.rowSpan - 1).coerceAtLeast(0),
                    )
                    .zIndex(if (selected) 1f else 0f)
                    .graphicsLayer { translationX = drag.x; translationY = drag.y }
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else outline, RoundedCornerShape(10.dp))
                    .pointerInput(item.id, cellWidth, cellHeight) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { onSelect(item.id) },
                            onDragEnd = {
                                val dx = (drag.x / (cellWidth + gap).toPx()).roundToInt()
                                val dy = (drag.y / (cellHeight + gap).toPx()).roundToInt()
                                drag = Offset.Zero
                                if (dx != 0 || dy != 0) onMove(item, item.rect.col + dx, item.rect.row + dy)
                            },
                            onDragCancel = { drag = Offset.Zero },
                            onDrag = { change, amount -> change.consume(); drag += amount },
                        )
                    }
                    .clickable { onSelect(item.id) }
                    .padding(6.dp),
            ) {
                Text(
                    itemLabel(item, appLabels),
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            }
        }
    }
}

/** The selected item's settings: its size, its app (map and music widgets) and removing it. */
@Composable
private fun ItemInspector(
    item: WorkspaceItem,
    appLabels: Map<String, String>,
    onResize: (Int, Int) -> Unit,
    otherWorkspaces: List<Workspace>,
    onMoveTo: (String) -> Unit,
    onRemove: () -> Unit,
    onPickApp: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(itemLabel(item, appLabels), style = MaterialTheme.typography.titleSmall)
        if (item.kind == WorkspaceItemKind.WIDGET && (item.widgetId == "map" || item.widgetId == "media")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(if (item.widgetId == "map") R.string.workspaces_map_app else R.string.workspaces_media_app),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { onPickApp(true) }) {
                    Text(item.appId?.let { appLabels[it] ?: it } ?: stringResource(R.string.workspaces_app_automatic))
                }
            }
            Text(stringResource(R.string.workspaces_widget_app_hint), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.workspaces_width), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            IconButton(onClick = { onResize(-1, 0) }, enabled = item.rect.colSpan > 1) { Icon(Icons.Rounded.Remove, null) }
            Text("${item.rect.colSpan}", style = MaterialTheme.typography.bodyLarge)
            IconButton(onClick = { onResize(1, 0) }, enabled = item.rect.col + item.rect.colSpan < WORKSPACE_COLUMNS) { Icon(Icons.Rounded.Add, null) }
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.workspaces_height), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            IconButton(onClick = { onResize(0, -1) }, enabled = item.rect.rowSpan > 1) { Icon(Icons.Rounded.Remove, null) }
            Text("${item.rect.rowSpan}", style = MaterialTheme.typography.bodyLarge)
            IconButton(onClick = { onResize(0, 1) }, enabled = item.rect.row + item.rect.rowSpan < WORKSPACE_ROWS) { Icon(Icons.Rounded.Add, null) }
        }
        if (item.kind == WorkspaceItemKind.ANDROID_WIDGET) {
            Text(stringResource(R.string.widget_remove_frees_id), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (item.kind == WorkspaceItemKind.APP || item.kind == WorkspaceItemKind.PANE) {
                TextButton(onClick = { onPickApp(false) }) { Text(stringResource(R.string.workspaces_change_app)) }
            }
            if (otherWorkspaces.isNotEmpty()) {
                var moveMenu by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { moveMenu = true }) { Text(stringResource(R.string.workspaces_move_to)) }
                    DropdownMenu(expanded = moveMenu, onDismissRequest = { moveMenu = false }) {
                        otherWorkspaces.forEach { target ->
                            DropdownMenuItem(
                                text = { Text(target.name) },
                                onClick = { moveMenu = false; onMoveTo(target.id) },
                            )
                        }
                    }
                }
            }
            TextButton(onClick = onRemove) {
                Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.workspaces_remove), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/** A picker row: an icon or a bitmap, and a label. */
@Composable
private fun ChoiceRow(icon: ImageVector?, label: String, bitmapId: android.graphics.Bitmap? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (bitmapId != null) {
            Image(bitmapId.asImageBitmap(), null, Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)))
        } else if (icon != null) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        } else {
            Icon(Icons.Rounded.Star, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

private val splitLayouts = setOf(
    WorkspaceLayout.SPLIT_50, WorkspaceLayout.SPLIT_60_40, WorkspaceLayout.SPLIT_70_30,
    WorkspaceLayout.SPLIT_30_70,
)

/** The dashboard widgets a workspace can hold. */
private val widgetIds = listOf("map", "media", "calendar", "notification", "weather", "home", "car")

private fun widgetLabel(id: String): Int = when (id) {
    "map" -> R.string.dash_card_map
    "media" -> R.string.dash_card_media
    "calendar" -> R.string.dash_card_calendar
    "notification" -> R.string.dash_card_notification
    "weather" -> R.string.dash_card_weather
    "home" -> R.string.dash_card_home
    "car" -> R.string.obd_dash_card
    else -> R.string.workspaces_widget_unavailable
}

private fun widgetIcon(id: String): ImageVector = when (id) {
    "map" -> Icons.Rounded.Map
    "media" -> Icons.Rounded.MusicNote
    "calendar" -> Icons.Rounded.CalendarMonth
    "notification" -> Icons.Rounded.Notifications
    "weather" -> Icons.Rounded.Cloud
    "home" -> Icons.Rounded.Home
    "car" -> Icons.Rounded.DirectionsCar
    else -> Icons.Rounded.Widgets
}

private fun layoutLabel(layout: WorkspaceLayout): Int = when (layout) {
    WorkspaceLayout.FULL -> R.string.workspaces_layout_full
    WorkspaceLayout.SPLIT_50 -> R.string.workspaces_layout_split50
    WorkspaceLayout.SPLIT_60_40 -> R.string.workspaces_layout_split60_40
    WorkspaceLayout.SPLIT_70_30 -> R.string.workspaces_layout_split70_30
    WorkspaceLayout.SPLIT_30_70 -> R.string.workspaces_layout_split30_70
    WorkspaceLayout.THREE -> R.string.workspaces_layout_three
    WorkspaceLayout.FOUR -> R.string.workspaces_layout_four
    WorkspaceLayout.CUSTOM -> R.string.workspaces_layout_custom
}

/** An item's name for the editor: the app's, or the widget's (and the app a map or music widget follows). */
@Composable
private fun itemLabel(item: WorkspaceItem, appLabels: Map<String, String>): String = when (item.kind) {
    WorkspaceItemKind.APP -> item.appId?.let { appLabels[it] ?: it } ?: "?"
    WorkspaceItemKind.PANE -> item.appId?.let { appLabels[it] ?: it } ?: "?"
    WorkspaceItemKind.WIDGET -> item.widgetId?.let { id ->
        val base = stringResource(widgetLabel(id))
        val app = item.appId?.let { appLabels[it] ?: it }
        if (app != null) "$base · $app" else base
    } ?: "?"
    WorkspaceItemKind.ANDROID_WIDGET -> {
        val ctx = LocalContext.current
        val provider = remember(item.appWidgetProvider) {
            item.appWidgetProvider?.let { runCatching { android.content.ComponentName.unflattenFromString(it) }.getOrNull() }
        }
        val app = remember(provider) {
            provider?.packageName?.let { pkg ->
                runCatching { ctx.packageManager.getApplicationLabel(ctx.packageManager.getApplicationInfo(pkg, 0)).toString() }.getOrNull()
            }
        }
        if (app != null) stringResource(R.string.workspaces_android_widget_named, app) else stringResource(R.string.workspaces_android_widget)
    }
}
