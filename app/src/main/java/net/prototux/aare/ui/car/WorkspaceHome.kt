package net.prototux.aare.ui.car

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.prototux.aare.ui.theme.LocalCarDimens
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import net.prototux.aare.R
import net.prototux.aare.car.NextEvent
import net.prototux.aare.obdapp.ObdApp
import net.prototux.aare.ui.car.obd.ObdDashCard
import net.prototux.aare.workspace.WORKSPACE_COLUMNS
import net.prototux.aare.workspace.WORKSPACE_ROWS
import net.prototux.aare.workspace.VIRTUAL_PAGE_COUNT
import net.prototux.aare.workspace.Workspace
import net.prototux.aare.workspace.WorkspaceItem
import net.prototux.aare.workspace.WorkspaceItemKind
import net.prototux.aare.workspace.WorkspaceRect
import net.prototux.aare.workspace.portraitRect
import net.prototux.aare.workspace.nearestVirtualPage
import net.prototux.aare.workspace.realIndexFor
import net.prototux.aare.workspace.virtualPageFor

/**
 * The car screen's home: the active workspace, and a compact pager to switch between workspaces.
 *
 * Only the current page is live. A workspace's map widget gets its app surface only when its page
 * is on screen, so switching workspaces doesn't take a surface away from a map on another one or
 * draw the map twice. The other widgets are ordinary Compose content: rendering the pages beside
 * the current one is cheap and keeps a swipe smooth, but no app surface or media session is
 * started for them.
 *
 * Swipes work on the pager bar (and on empty workspace space) by default, leaving a map's own
 * gestures alone. `swipeAnywhere` turns the whole page into the pager's gesture area instead.
 */
@Composable
internal fun WorkspaceHome(
    state: CarUiState,
    actions: CarActions,
    appContent: @Composable (CarAppEntry, Modifier) -> Unit,
    navMap: @Composable (CarAppEntry, Modifier) -> Unit,
    modifier: Modifier,
) {
    val config = state.workspaceConfig
    val workspaces = config.workspaces
    if (workspaces.isEmpty()) {
        // The runtime couldn't load anything (it migrates on first use): the legacy dashboard is a
        // better fallback than an empty screen, and never writes over the stored config.
        Dashboard(state, actions, navMap, modifier)
        return
    }
    val active = config.activeOrStart(state.activeWorkspaceId) ?: workspaces.first()
    val activeIndex = config.indexOf(active.id).coerceAtLeast(0)
    key(state.workspaceGeneration) {
        Box(modifier.fillMaxSize()) {
            Wallpaper(state, Modifier.fillMaxSize())
            if (workspaces.size == 1) {
                WorkspacePage(workspaces.first(), state, actions, appContent, navMap, Modifier.fillMaxSize(), active = true)
            } else {
                WorkspacePager(workspaces, activeIndex, state, actions, appContent, navMap, Modifier.fillMaxSize())
            }
        }
    }
}

/** The horizontal, cyclic pager: the page number is virtual, so swiping past the last workspace shows the first. */
@Composable
private fun WorkspacePager(
    workspaces: List<Workspace>,
    startIndex: Int,
    state: CarUiState,
    actions: CarActions,
    appContent: @Composable (CarAppEntry, Modifier) -> Unit,
    navMap: @Composable (CarAppEntry, Modifier) -> Unit,
    modifier: Modifier,
) {
    val count = workspaces.size
    val pager = rememberPagerState(initialPage = virtualPageFor(startIndex, count)) { VIRTUAL_PAGE_COUNT }
    val scope = rememberCoroutineScope()
    val barHeight = if (state.showsWorkspaceBar) WORKSPACE_BAR_HEIGHT else 0.dp

    /** Moves to the workspace at [index] by the shortest way round the ring. */
    fun goTo(index: Int) {
        scope.launch { pager.animateScrollToPage(nearestVirtualPage(index, pager.currentPage, count)) }
    }

    /** One workspace forward (positive) or back, always animating one page, never jumping the ring. */
    fun step(by: Int) {
        scope.launch {
            if (pager.isScrollInProgress) return@launch
            pager.animateScrollToPage((pager.currentPage + by).coerceIn(1, VIRTUAL_PAGE_COUNT - 2))
        }
    }

    // The selection can also change from the phone's editor (a default, a deletion): follow it.
    LaunchedEffect(state.activeWorkspaceId, count) {
        val target = state.workspaceConfig.indexOf(state.activeWorkspaceId)
        if (target < 0) return@LaunchedEffect
        if (realIndexFor(pager.currentPage, count) != target && !pager.isScrollInProgress) {
            pager.animateScrollToPage(nearestVirtualPage(target, pager.currentPage, count))
        }
    }
    // A settled page is the driver's choice: remember it (and let the runtime follow its map app).
    LaunchedEffect(pager, count, workspaces) {
        snapshotFlow { pager.currentPage to pager.isScrollInProgress }
            .distinctUntilChanged()
            .collect { (page, scrolling) ->
                if (scrolling) return@collect
                val workspace = workspaces[realIndexFor(page, count)]
                if (workspace.id != state.activeWorkspaceId) actions.selectWorkspace(workspace.id)
            }
    }

    Box(modifier) {
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 0,
            userScrollEnabled = state.workspaceConfig.swipeAnywhere,
            key = { it },
        ) { page ->
            val index = realIndexFor(page, count)
            val workspace = workspaces[index]
            val current = page == pager.currentPage
            key(workspace.id) {
                WorkspacePage(
                    workspace = workspace,
                    state = state,
                    actions = actions,
                    appContent = appContent,
                    navMap = navMap,
                    modifier = Modifier.fillMaxSize().padding(bottom = barHeight),
                    active = current,
                    onSwipe = if (state.workspaceConfig.swipeAnywhere) null else ({ direction -> step(direction) }),
                )
            }
        }
        if (state.showsWorkspaceBar) {
            WorkspaceBar(
                count = count,
                activeIndex = realIndexFor(pager.currentPage, count),
                activeName = workspaces[realIndexFor(pager.currentPage, count)].name,
                names = workspaces.map { it.name },
                showName = state.workspaceConfig.showBarName,
                onSelect = ::goTo,
                onSwipe = ::step,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/** One workspace: its items on the 10×6 grid (turned for portrait screens), over the wallpaper. */
@Composable
internal fun WorkspacePage(
    workspace: Workspace,
    state: CarUiState,
    actions: CarActions,
    appContent: @Composable (CarAppEntry, Modifier) -> Unit,
    navMap: @Composable (CarAppEntry, Modifier) -> Unit,
    modifier: Modifier,
    active: Boolean,
    /** Swipe the empty space: +1 for the next workspace, -1 for the previous; null to leave it to the pager. */
    onSwipe: ((Int) -> Unit)? = null,
) {
    val outer = LocalCarDimens.current.gutter
    BoxWithConstraints(modifier.fillMaxSize().padding(outer)) {
        val portrait = maxHeight > maxWidth * 1.1f
        val columns = if (portrait) WORKSPACE_ROWS else WORKSPACE_COLUMNS
        val rows = if (portrait) WORKSPACE_COLUMNS else WORKSPACE_ROWS
        val gap = LocalCarDimens.current.gutter
        val cellWidth = (maxWidth - gap * (columns - 1)) / columns
        val cellHeight = (maxHeight - gap * (rows - 1)) / rows

        // The empty space between items is the swipe area: no widget on top of it, so a map keeps
        // its own drag gestures, and the driver can still flick the workspace away.
        if (onSwipe != null) {
            Box(
                Modifier.fillMaxSize().pointerInput(onSwipe) {
                    var distance = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { distance = 0f },
                        onDragEnd = {
                            val threshold = 48.dp.toPx()
                            if (distance > threshold) onSwipe(-1) else if (distance < -threshold) onSwipe(1)
                        },
                        onHorizontalDrag = { change, amount -> distance += amount; change.consume() },
                    )
                },
            )
        }

        if (workspace.items.isEmpty()) {
            EmptyWorkspace(actions, Modifier.align(Alignment.Center))
        }
        workspace.items.forEach { item ->
            val rect = if (portrait) item.rect.portraitRect() else item.rect
            key(item.id) {
                Box(
                    Modifier
                        .offset(x = (cellWidth + gap) * rect.col, y = (cellHeight + gap) * rect.row)
                        .size(
                            width = cellWidth * rect.colSpan + gap * (rect.colSpan - 1).coerceAtLeast(0),
                            height = cellHeight * rect.rowSpan + gap * (rect.rowSpan - 1).coerceAtLeast(0),
                        ),
                ) {
                    WorkspaceItemContent(item, workspace, state, actions, appContent, navMap, active)
                }
            }
        }
    }
}

/** An app shortcut or a dashboard widget, filling its cells. */
@Composable
private fun WorkspaceItemContent(
    item: WorkspaceItem,
    workspace: Workspace,
    state: CarUiState,
    actions: CarActions,
    appContent: @Composable (CarAppEntry, Modifier) -> Unit,
    navMap: @Composable (CarAppEntry, Modifier) -> Unit,
    active: Boolean,
) {
    when (item.kind) {
        WorkspaceItemKind.APP -> {
            val app = state.workspaceApp(item.appId)
            if (app != null) WorkspaceAppTile(app, Modifier.fillMaxSize()) { actions.openApp(app) }
            else WidgetPlaceholder(Icons.Rounded.Widgets, stringResource(R.string.workspace_missing_app), null)
        }
        WorkspaceItemKind.PANE -> {
            val app = state.workspaceApp(item.appId)
            when {
                app == null -> WidgetPlaceholder(Icons.Rounded.Widgets, stringResource(R.string.workspace_missing_app), null)
                // Only the page on screen draws a live app: two pages must not own the same
                // connection's surface, and inactive pages must not run a second session.
                !active -> WidgetPlaceholder(Icons.Rounded.Apps, app.label, stringResource(R.string.workspace_pane_inactive))
                else -> androidx.compose.foundation.layout.Box(
                    Modifier.fillMaxSize().clip(RoundedCornerShape(LocalCarDimens.current.cardRadius)),
                ) { appContent(app, Modifier.fillMaxSize()) }
            }
        }
        WorkspaceItemKind.WIDGET -> WorkspaceWidget(item, state, actions, navMap, active)
    }
}

@Composable
private fun WorkspaceWidget(
    item: WorkspaceItem,
    state: CarUiState,
    actions: CarActions,
    navMap: @Composable (CarAppEntry, Modifier) -> Unit,
    active: Boolean,
) {
    when (item.widgetId) {
        "map" -> {
            if (!active) {
                // While the page is beside the visible one only the placeholder is drawn: a live
                // map here would take the app's one surface from the map actually on screen.
                val nav = state.workspaceApp(item.appId) ?: state.app(state.activeNavAppId)
                WidgetPlaceholder(Icons.Rounded.Map, nav?.label ?: stringResource(R.string.dash_card_map), null)
            } else {
                NavCard(state, actions, navMap, Modifier.fillMaxSize(), navAppId = state.workspaceApp(item.appId)?.id)
            }
        }
        "media" -> BoxWithConstraints(Modifier.fillMaxSize()) {
            MediaCard(state, actions, compact = maxHeight < 220.dp, modifier = Modifier.fillMaxSize(),
                mediaAppId = state.workspaceApp(item.appId)?.id)
        }
        "calendar" -> {
            val events: List<NextEvent> = state.nextEvents
            if (events.isEmpty()) WidgetPlaceholder(Icons.Rounded.CalendarMonth, stringResource(R.string.dash_card_calendar), stringResource(R.string.workspace_no_events))
            else Card(Modifier.fillMaxSize()) { CalendarAgendaCard(events, actions::navigateTo, Modifier.fillMaxSize()) }
        }
        "notification" -> {
            val latest = state.history.firstOrNull()
            val bell = state.apps.firstOrNull { it.kind == CarAppKind.NOTIFICATIONS }
            if (latest == null) WidgetPlaceholder(Icons.Rounded.Notifications, stringResource(R.string.dash_card_notification), stringResource(R.string.workspace_no_notifications))
            else {
                val openBell: (() -> Unit)? = if (bell != null) ({ actions.openApp(bell) }) else null
                Card(Modifier.fillMaxSize(), onClick = openBell) {
                    LatestNotification(latest, state.drivingRestricted)
                }
            }
        }
        "weather" -> {
            val weather = state.weather
            if (weather == null) WidgetPlaceholder(Icons.Rounded.Cloud, stringResource(R.string.dash_card_weather), stringResource(R.string.workspace_no_weather))
            else Card(Modifier.fillMaxSize()) { WeatherCard(weather) }
        }
        "home" -> {
            if (state.home.isEmpty) WidgetPlaceholder(Icons.Rounded.Home, stringResource(R.string.dash_card_home), stringResource(R.string.workspace_no_home))
            else BoxWithConstraints(Modifier.fillMaxSize()) {
                val homeApp = state.apps.firstOrNull { it.kind == CarAppKind.HOME }
                Card(Modifier.fillMaxSize()) {
                    HomeCard(state.home, actions, compact = maxHeight < 220.dp, onOpen = { homeApp?.let(actions::openApp) })
                }
            }
        }
        "car" -> {
            val obd by ObdApp.snapshot.collectAsState()
            if (obd.readings == null) WidgetPlaceholder(Icons.Rounded.DirectionsCar, stringResource(R.string.obd_dash_card), stringResource(R.string.workspace_no_obd))
            else {
                val openApp = state.apps.firstOrNull { it.kind == CarAppKind.OBD }
                val open: (() -> Unit)? = if (openApp != null) ({ actions.openApp(openApp) }) else null
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    Card(Modifier.fillMaxSize(), onClick = open) { ObdDashCard(obd, maxHeight < 220.dp) }
                }
            }
        }
        else -> WidgetPlaceholder(Icons.Rounded.Widgets, stringResource(R.string.workspace_widget_unavailable), null)
    }
}

/** A shortcut to any app on the car screen, at whatever size its cells give it. */
@Composable
private fun WorkspaceAppTile(app: CarAppEntry, modifier: Modifier, onClick: () -> Unit) {
    val radius = LocalCarDimens.current.cardRadius
    Card(modifier, onClick = onClick) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val shortest = minOf(maxWidth, maxHeight)
            val iconSize = (shortest.value * 0.42f).toInt().coerceIn(40, 120)
            val showLabel = maxHeight >= 120.dp || maxWidth >= 160.dp
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AppIcon(app, iconSize)
                if (showLabel) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        app.label,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/** What a widget shows when it has nothing to show, or while it isn't the live page: a calm, readable tile. */
@Composable
private fun WidgetPlaceholder(icon: ImageVector, title: String, hint: String?) {
    Card(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
            val compact = maxHeight < 150.dp
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(if (compact) 28.dp else 40.dp))
                if (!compact) {
                    Spacer(Modifier.height(10.dp))
                    Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
                if (hint != null && !compact) {
                    Spacer(Modifier.height(4.dp))
                    Text(hint, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

/** The first-run / all-items-removed workspace: says what to do instead of showing an empty grid. */
@Composable
private fun EmptyWorkspace(actions: CarActions, modifier: Modifier) {
    Column(modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.Widgets, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.workspace_empty_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.workspace_empty_hint), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        val shape = RoundedCornerShape(50)
        Row(
            Modifier.clip(shape).background(MaterialTheme.colorScheme.primaryContainer)
                .carClickable(shape) { actions.openLauncher() }.padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Apps, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.size(10.dp))
            Text(stringResource(R.string.workspace_open_apps), style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

private val WORKSPACE_BAR_HEIGHT = 64.dp

/**
 * The compact pager: the workspace's name, how many there are (each dot a jump target), and the
 * two arrows. A horizontal drag anywhere on the bar switches workspaces too, so a touch screen
 * without a knob can flick; the arrows and the dots are focusable for a rotary head unit.
 */
@Composable
private fun WorkspaceBar(
    count: Int,
    activeIndex: Int,
    activeName: String,
    names: List<String>,
    showName: Boolean,
    onSelect: (Int) -> Unit,
    onSwipe: (Int) -> Unit,
    modifier: Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(WORKSPACE_BAR_HEIGHT)
            .background(Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = 0.92f))))
            .pointerInput(count) {
                var distance = 0f
                detectHorizontalDragGestures(
                    onDragStart = { distance = 0f },
                    onDragEnd = {
                        val threshold = 40.dp.toPx()
                        if (distance > threshold) onSwipe(-1) else if (distance < -threshold) onSwipe(1)
                    },
                    onHorizontalDrag = { change, amount -> distance += amount; change.consume() },
                )
            }
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (showName) {
            Text(
                activeName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 180.dp),
            )
            Spacer(Modifier.size(12.dp))
        }
        if (count <= 8) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(count) { i ->
                    val selected = i == activeIndex
                    Box(
                        Modifier.size(36.dp).clip(CircleShape)
                            .carClickable(CircleShape) { onSelect(i) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .size(width = if (selected) 20.dp else 8.dp, height = 8.dp)
                                .clip(CircleShape)
                                .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)),
                        )
                    }
                }
            }
        } else {
            // More workspaces than dots fit: the counter itself opens the full list, so a
            // specific one is still one tap away.
            var menu by remember { mutableStateOf(false) }
            Box {
                Text(
                    "${activeIndex + 1}/$count",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(RoundedCornerShape(50))
                        .carClickable(RoundedCornerShape(50)) { menu = true }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    names.forEachIndexed { i, name ->
                        DropdownMenuItem(text = { Text(name) }, onClick = { menu = false; onSelect(i) })
                    }
                }
            }
        }
    }
}
