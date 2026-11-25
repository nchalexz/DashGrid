package net.prototux.aare.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import net.prototux.aare.Settings
import net.prototux.aare.workspace.Workspace
import net.prototux.aare.workspace.WorkspaceAxis
import net.prototux.aare.workspace.WorkspaceConfig
import net.prototux.aare.workspace.WorkspaceItem
import net.prototux.aare.workspace.WorkspaceItemKind
import net.prototux.aare.workspace.WorkspaceJson
import net.prototux.aare.workspace.WorkspaceLayout
import net.prototux.aare.workspace.WorkspaceRect
import org.junit.Rule
import org.junit.Test

/**
 * The phone editor in Russian, at a tall phone size: the long labels, the workspace rows and the
 * open editor (name, layout, grid, item inspector) must all fit without clipping or overlap.
 */
class WorkspaceRussianMockup {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_7.copy(screenWidth = 1080, screenHeight = 4200, locale = "ru"),
        theme = "android:Theme.Material.Light.NoActionBar",
    )

    @Test
    fun editorRussian() {
        val settings = Settings(paparazzi.context)
        val config = WorkspaceConfig(
                workspaces = listOf(
                    Workspace(
                        id = "city", name = "Город", layout = WorkspaceLayout.SPLIT_70_30, axis = WorkspaceAxis.HORIZONTAL,
                        items = listOf(
                            WorkspaceItem("m", WorkspaceItemKind.WIDGET, WorkspaceRect(0, 0, 7, 6), widgetId = "map"),
                            WorkspaceItem("mu", WorkspaceItemKind.WIDGET, WorkspaceRect(7, 0, 3, 6), widgetId = "media"),
                        ),
                    ),
                    Workspace(
                        id = "road", name = "Трасса", layout = WorkspaceLayout.FOUR,
                        items = listOf(
                            WorkspaceItem("r1", WorkspaceItemKind.WIDGET, WorkspaceRect(0, 0, 5, 3), widgetId = "map"),
                            WorkspaceItem("r2", WorkspaceItemKind.WIDGET, WorkspaceRect(5, 0, 5, 3), widgetId = "weather"),
                            WorkspaceItem("r3", WorkspaceItemKind.WIDGET, WorkspaceRect(0, 3, 5, 3), widgetId = "calendar"),
                            WorkspaceItem("r4", WorkspaceItemKind.WIDGET, WorkspaceRect(5, 3, 5, 3), widgetId = "media"),
                        ),
                    ),
                    Workspace(id = "empty", name = "Пусто"),
                ),
            defaultId = "city", lastId = "city",
        )
        paparazzi.snapshot {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    WorkspacesSettings(settings, initialEditingId = "city", initialConfig = config)
                }
            }
        }
    }
}
