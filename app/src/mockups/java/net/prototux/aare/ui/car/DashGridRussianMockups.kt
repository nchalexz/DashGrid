package net.prototux.aare.ui.car

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import net.prototux.aare.ui.theme.CarTheme
import net.prototux.aare.ui.theme.DashGridTheme
import net.prototux.aare.workspace.Workspace
import net.prototux.aare.workspace.WorkspaceConfig
import net.prototux.aare.workspace.WorkspaceItem
import net.prototux.aare.workspace.WorkspaceItemKind
import net.prototux.aare.workspace.WorkspaceLayout
import net.prototux.aare.workspace.WorkspaceRect
import org.junit.Rule
import org.junit.Test

/**
 * Russian car-screen renders: Cyrillic in the pager bar, widget placeholders and the empty-workspace
 * state. The locale is paparazzi's, so `stringResource` resolves values-ru exactly as on a device.
 */
class DashGridRussianMockups {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(
            screenWidth = 1280,
            screenHeight = 720,
            xdpi = 160,
            ydpi = 160,
            density = Density.MEDIUM,
            orientation = ScreenOrientation.LANDSCAPE,
            softButtons = false,
            locale = "ru",
        ),
        theme = "android:Theme.Material.NoActionBar",
    )

    @Test
    fun workspaceRussian() {
        val state = russianState().apply {
            workspaceConfig = WorkspaceConfig(
                workspaces = listOf(
                    Workspace(
                        id = "city", name = "Город", layout = WorkspaceLayout.FOUR,
                        items = listOf(
                            WorkspaceItem("w-map", WorkspaceItemKind.PANE, WorkspaceRect(0, 0, 5, 3), appId = "car:maps"),
                            WorkspaceItem("w-media", WorkspaceItemKind.WIDGET, WorkspaceRect(5, 0, 5, 3), widgetId = "media"),
                            WorkspaceItem("w-weather", WorkspaceItemKind.WIDGET, WorkspaceRect(0, 3, 5, 3), widgetId = "weather"),
                            WorkspaceItem("w-calendar", WorkspaceItemKind.WIDGET, WorkspaceRect(5, 3, 5, 3), widgetId = "calendar"),
                        ),
                    ),
                    Workspace(
                        id = "road", name = "Трасса", layout = WorkspaceLayout.FULL,
                        items = listOf(WorkspaceItem("r-map", WorkspaceItemKind.WIDGET, WorkspaceRect.full(), widgetId = "map")),
                    ),
                    Workspace(
                        id = "car", name = "Автомобиль", layout = WorkspaceLayout.FULL,
                        items = listOf(WorkspaceItem("c-media", WorkspaceItemKind.WIDGET, WorkspaceRect.full(), widgetId = "media")),
                    ),
                ),
                defaultId = "city", lastId = "city", showBar = true,
            )
            activeWorkspaceId = "city"
        }
        paparazzi.snapshot {
            CarTheme(night = true, theme = DashGridTheme) {
                CarShell(state, RussianActions, appContent = { app, m -> RussianAppPane(app.label, m) }, navMap = { _, m -> RussianMap(m) })
            }
        }
    }

    @Test
    fun emptyWorkspaceRussian() {
        val state = russianState().apply {
            workspaceConfig = WorkspaceConfig(
                workspaces = listOf(Workspace(id = "empty", name = "Пусто", items = emptyList())),
                defaultId = "empty", lastId = "empty",
            )
            activeWorkspaceId = "empty"
        }
        paparazzi.snapshot {
            CarTheme(night = true, theme = DashGridTheme) {
                CarShell(state, RussianActions, appContent = { app, m -> RussianAppPane(app.label, m) }, navMap = { _, m -> RussianMap(m) })
            }
        }
    }

    private fun russianState() = CarUiState().apply {
        night = true
        theme = DashGridTheme
        layout = CarLayout.TASKBAR
        screen = CarScreen.Dashboard
        apps += listOf(
            CarAppEntry("car:maps", "mock.maps", "Карты", null, CarAppKind.NAVIGATION),
            CarAppEntry("media:music", "mock.music", "Музыка", null, CarAppKind.MEDIA),
        )
        activeNavAppId = "car:maps"
        activeMediaAppId = "media:music"
        // Weather, calendar, notifications and Home actions are deliberately absent: the widgets
        // then show their Russian placeholders, which is what this render checks.
    }

    private object RussianActions : CarActions {
        override fun openApp(app: CarAppEntry) = Unit
        override fun goHome() = Unit
        override fun selectWorkspace(id: String) = Unit
        override fun cycleWorkspace(step: Int) = Unit
        override fun openLauncher() = Unit
        override fun assistant() = Unit
        override fun mediaPlayPause() = Unit
        override fun mediaNext() = Unit
        override fun mediaPrevious() = Unit
        override fun dismissNotice(notice: CarNotice) = Unit
        override fun answerCall() = Unit
        override fun endCall() = Unit
        override fun navigateTo(place: String) = Unit
        override fun openBeside(app: CarAppEntry) = Unit
        override fun closeSplit() = Unit
        override fun commitSplitRatio(raw: Float) = Unit
        override fun focusPane(pane: CarPane) = Unit
        override fun runHomeAction(id: String) = Unit
    }
}

@androidx.compose.runtime.Composable
private fun RussianMap(modifier: Modifier) {
    Canvas(modifier.fillMaxSize().background(Color(0xFF1B2330))) {
        val road = Color(0xFF34404F)
        for (i in 1..4) drawLine(road, Offset(0f, size.height * i / 5f), Offset(size.width, size.height * i / 5f - 40f), strokeWidth = 14f)
        for (i in 1..3) drawLine(road, Offset(size.width * i / 4f, 0f), Offset(size.width * i / 4f + 40f, size.height), strokeWidth = 14f)
        drawLine(Color(0xFF4285F4), Offset(size.width * 0.5f, size.height), Offset(size.width * 0.7f, size.height * 0.45f), strokeWidth = 16f)
    }
}

/** A stand-in for a live app pane: what an app's own UI occupies in the cell. */
@androidx.compose.runtime.Composable
private fun RussianAppPane(label: String, modifier: Modifier) {
    Box(
        modifier.fillMaxSize().background(Color(0xFF14262B)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.Map, null, tint = Color(0xFF36D6C3), modifier = Modifier.size(36.dp))
            Text(label, color = Color(0xFFF1F5F9), style = MaterialTheme.typography.titleMedium)
            Text("живое приложение", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
        }
    }
}
