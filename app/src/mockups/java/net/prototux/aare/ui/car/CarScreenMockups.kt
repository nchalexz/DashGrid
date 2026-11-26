package net.prototux.aare.ui.car

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import net.prototux.aare.car.NextEvent
import net.prototux.aare.car.Weather
import net.prototux.aare.notify.PhoneGuidance
import net.prototux.aare.ui.theme.CarAccents
import net.prototux.aare.ui.theme.CarTheme
import net.prototux.aare.ui.theme.CarThemes
import net.prototux.aare.ui.theme.DashGridTheme
import net.prototux.aare.ui.theme.DockIconPreview
import net.prototux.aare.ui.theme.DockIconTone
import net.prototux.aare.ui.theme.withAccent
import net.prototux.aare.R
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import net.prototux.aare.workspace.Workspace
import net.prototux.aare.workspace.WorkspaceAxis
import net.prototux.aare.workspace.WorkspaceConfig
import net.prototux.aare.workspace.WorkspaceItem
import net.prototux.aare.workspace.WorkspaceItemKind
import net.prototux.aare.workspace.WorkspaceLayout
import net.prototux.aare.workspace.WorkspaceRect
import org.junit.Rule
import org.junit.Test

/**
 * Mockups of the car screen in each style, for the README: drawn on the JVM by Paparazzi with made-up
 * apps, music and directions (no emulator, no phone). The navigation app's map is a drawn stand-in,
 * and app icons and album art are generated. Run `./gradlew :app:recordPaparazziDebug --tests
 * '*CarScreenMockups*'`; the images land in app/src/test/snapshots/images. A real car screen can
 * differ (fonts, the map, the phone's status and colours).
 */
class CarScreenMockups {
    @get:Rule
    val paparazzi = Paparazzi(
        // A common head-unit size, at 160 dpi so 1 dp is 1 pixel.
        deviceConfig = DeviceConfig.PIXEL_5.copy(
            screenWidth = 1280,
            screenHeight = 720,
            xdpi = 160,
            ydpi = 160,
            density = Density.MEDIUM,
            orientation = ScreenOrientation.LANDSCAPE,
            softButtons = false,
        ),
        theme = "android:Theme.Material.NoActionBar",
    )

    @Test fun aare() = shot("aare", night = true)
    @Test fun aareDay() = shot("aare", night = false)
    @Test fun androidAuto() = shot("android_auto", night = true)
    @Test fun androidAuto2019() = shot("android_auto_2019", night = true, screen = CarScreen.Launcher)
    @Test fun carPlay() = shot("carplay", night = true)
    @Test fun carPlayDay() = shot("carplay", night = false)
    @Test fun materialYou() = shot("material_you", night = true)
    @Test fun highContrast() = shot("high_contrast", night = true)
    @Test fun nightAmber() = shot("night", night = true)
    @Test fun nightRed() = shot("night", night = true, accent = "red")
    @Test fun retro() = shot("retro", night = true)
    @Test fun retroAmber() = shot("retro", night = true, accent = "amber")
    @Test fun customDashboard() = shot("aare", night = true, cards = listOf(DashCard.MEDIA, DashCard.MAP, DashCard.WEATHER))

    /** DashGrid's own home: three workspaces, the first one (map + music) showing. */
    @Test fun dashgridWorkspaces() = workspaceShot(night = true, workspaces = cityWorkspaces(), active = "city")

    /** Three equal widgets on one workspace. */
    @Test fun dashgridThreeWidgets() = workspaceShot(
        night = true,
        workspaces = listOf(
            Workspace(
                id = "three", name = "Route", layout = WorkspaceLayout.THREE,
                items = listOf(
                    WorkspaceItem("t-map", WorkspaceItemKind.WIDGET, WorkspaceRect(0, 0, 4, 6), widgetId = "map"),
                    WorkspaceItem("t-media", WorkspaceItemKind.WIDGET, WorkspaceRect(4, 0, 3, 6), widgetId = "media"),
                    WorkspaceItem("t-weather", WorkspaceItemKind.WIDGET, WorkspaceRect(7, 0, 3, 6), widgetId = "weather"),
                ),
            ),
            Workspace(
                id = "three-spare", name = "Spare", layout = WorkspaceLayout.FULL,
                items = listOf(WorkspaceItem("ts-media", WorkspaceItemKind.WIDGET, WorkspaceRect.full(), widgetId = "media")),
            ),
        ),
        active = "three",
    )

    /** Four widgets in a 2×2 grid. */
    @Test fun dashgridFourWidgets() = workspaceShot(
        night = true,
        workspaces = listOf(
            Workspace(
                id = "four", name = "Everything", layout = WorkspaceLayout.FOUR,
                items = listOf(
                    WorkspaceItem("f-map", WorkspaceItemKind.WIDGET, WorkspaceRect(0, 0, 5, 3), widgetId = "map"),
                    WorkspaceItem("f-media", WorkspaceItemKind.WIDGET, WorkspaceRect(5, 0, 5, 3), widgetId = "media"),
                    WorkspaceItem("f-weather", WorkspaceItemKind.WIDGET, WorkspaceRect(0, 3, 5, 3), widgetId = "weather"),
                    WorkspaceItem("f-calendar", WorkspaceItemKind.WIDGET, WorkspaceRect(5, 3, 5, 3), widgetId = "calendar"),
                ),
            ),
            Workspace(
                id = "four-spare", name = "Spare", layout = WorkspaceLayout.FULL,
                items = listOf(WorkspaceItem("fs-media", WorkspaceItemKind.WIDGET, WorkspaceRect.full(), widgetId = "media")),
            ),
        ),
        active = "four",
    )

    /** The same configuration in day colours. */
    @Test fun dashgridDay() = workspaceShot(night = false, workspaces = cityWorkspaces(), active = "city")

    /** Both fixed dock buttons on a workspace: Home and App Dash, next to the voice button. */
    @Test fun dashgridPinnedButtons() = workspaceShot(
        night = true,
        workspaces = cityWorkspaces(),
        active = "city",
        modify = { state ->
            state.alwaysShowHome = true
            state.alwaysShowAppDash = true
        },
    )

    /** The dock's monochrome icons in the light theme's default tone (dark grey). */
    @Test fun dashgridMonochromeLight() = workspaceShot(
        night = false,
        workspaces = cityWorkspaces(),
        active = "city",
        modify = { state ->
            state.dockMonochrome = true
            state.dockToneLight = DockIconTone.DARK_GREY
            state.dockToneDark = DockIconTone.WHITE
        },
    )

    /** The dock's monochrome icons in the dark theme's default tone (white). */
    @Test fun dashgridMonochromeDark() = workspaceShot(
        night = true,
        workspaces = cityWorkspaces(),
        active = "city",
        modify = { state ->
            state.dockMonochrome = true
            state.dockToneLight = DockIconTone.DARK_GREY
            state.dockToneDark = DockIconTone.WHITE
        },
    )

    /** Every tone on the built-in app icons: a documentation sheet for the dock's four settings. */
    @Test fun dockTonesLight() = dockToneSheet(paparazzi, night = false)
    @Test fun dockTonesDark() = dockToneSheet(paparazzi, night = true)

    private fun cityWorkspaces(): List<Workspace> = listOf(
        Workspace(
            id = "city", name = "City", layout = WorkspaceLayout.SPLIT_70_30,
            items = listOf(
                WorkspaceItem("city-map", WorkspaceItemKind.WIDGET, WorkspaceRect(0, 0, 7, 6), widgetId = "map"),
                WorkspaceItem("city-media", WorkspaceItemKind.WIDGET, WorkspaceRect(7, 0, 3, 6), widgetId = "media"),
            ),
        ),
        Workspace(
            id = "highway", name = "Highway", layout = WorkspaceLayout.SPLIT_50,
            items = listOf(
                WorkspaceItem("road-map", WorkspaceItemKind.WIDGET, WorkspaceRect(0, 0, 5, 6), widgetId = "map"),
                WorkspaceItem("road-weather", WorkspaceItemKind.WIDGET, WorkspaceRect(5, 0, 5, 6), widgetId = "weather"),
            ),
        ),
        Workspace(
            id = "car", name = "Car", layout = WorkspaceLayout.FULL,
            items = listOf(WorkspaceItem("car-obd", WorkspaceItemKind.WIDGET, WorkspaceRect.full(), widgetId = "car")),
        ),
    )

    private fun workspaceShot(night: Boolean, workspaces: List<Workspace>, active: String, modify: (CarUiState) -> Unit = {}) {
        val state = mockState(night, CarScreen.Dashboard).apply {
            theme = DashGridTheme
            workspaceConfig = WorkspaceConfig(
                workspaces = workspaces,
                defaultId = active,
                lastId = active,
                showBar = true,
            )
            activeWorkspaceId = active
            modify(this)
        }
        paparazzi.snapshot {
            CarTheme(night = night, theme = DashGridTheme) {
                CarShell(state, NoActions, appContent = { _, m -> MapStandIn(night, m) }, navMap = { _, m -> MapStandIn(night, m) })
            }
        }
    }

    /** The style made of [themeId] and its layout, as the settings' Style picker would set it. */
    private fun shot(
        themeId: String,
        night: Boolean,
        accent: String? = null,
        screen: CarScreen = CarScreen.Dashboard,
        cards: List<DashCard> = DashCard.DEFAULT,
    ) {
        val style = CarStyles.all.first { it.themeId == themeId }
        val state = mockState(night, screen).apply {
            theme = CarThemes.byId(themeId).withAccent(CarAccents.byId(accent))
            layout = style.layout
            dashboardCards = cards
        }
        paparazzi.snapshot {
            CarTheme(night = state.night, theme = state.theme) {
                CarShell(state, NoActions, appContent = { _, m -> MapStandIn(night, m) }, navMap = { _, m -> MapStandIn(night, m) })
            }
        }
    }

    private fun mockState(night: Boolean, screen: CarScreen) = CarUiState().apply {
        this.night = night
        this.screen = screen
        val maps = app("maps", "Maps", CarAppKind.NAVIGATION, 0xFF34A853.toInt())
        val music = app("music", "Music", CarAppKind.MEDIA, 0xFFE8453C.toInt())
        apps += listOf(
            maps, music,
            app("podcasts", "Podcasts", CarAppKind.MEDIA, 0xFF8E44AD.toInt()),
            app("radio", "Radio", CarAppKind.MEDIA, 0xFFF39C12.toInt()),
            app("messages", "Messages", CarAppKind.MESSAGING, 0xFF1A73E8.toInt()),
            app("phone", "Phone", CarAppKind.PHONE, 0xFF0F9D58.toInt()),
            app("notifications", "Notifications", CarAppKind.NOTIFICATIONS, 0xFF5F6368.toInt()),
            app("settings", "Settings", CarAppKind.SETTINGS, 0xFF607D8B.toInt()),
        )
        activeNavAppId = maps.id
        activeMediaAppId = music.id
        recentAppIds += listOf("podcasts", "messages")
        nowPlaying = NowPlaying(music.id, "Night Drive", "The Synthwaves", albumArt(), playing = true,
            positionMs = 95_000, durationMs = 240_000, updatedAtMs = 0)
        // Directions only off the dashboard: on it, they would replace the map with a directions card.
        if (screen != CarScreen.Dashboard) phoneGuidance = PhoneGuidance("mock", "maps", "Maps", "300 m", "Turn right onto Aarstrasse",
            "12 min · 8.4 km", arrow = null, distance = null, instruction = "Turn right onto Aarstrasse")
        weather = Weather(18, "°C", 1, day = !night)
        nextEvents = listOf(NextEvent("Coffee with Sam", startMs = System.currentTimeMillis() + 45 * 60_000, allDay = false, location = "Bern"))
    }

    private fun app(id: String, label: String, kind: CarAppKind, color: Int) = CarAppEntry(id, "mock.$id", label, appIcon(label, color), kind)

    /** A round icon in [color] with the label's initial. */
    private fun appIcon(label: String, color: Int): Bitmap {
        val b = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        c.drawCircle(48f, 48f, 48f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = android.graphics.Color.WHITE; textSize = 52f; textAlign = Paint.Align.CENTER }
        c.drawText(label.take(1), 48f, 66f, text)
        return b
    }

    /** A sunset gradient in place of album art. */
    private fun albumArt(): Bitmap {
        val b = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val p = Paint().apply { shader = LinearGradient(0f, 0f, 256f, 256f, 0xFFFF7E5F.toInt(), 0xFF6A3093.toInt(), Shader.TileMode.CLAMP) }
        Canvas(b).drawRect(0f, 0f, 256f, 256f, p)
        return b
    }

    private object NoActions : CarActions {
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

/**
 * A documentation sheet: DashGrid's real app icons (its own vector drawables) in each of the four
 * tones, on the theme's background. Not a settings page screenshot — the settings preview loads the
 * phone's own installed icons, which a JVM mockup can't see.
 */
private fun dockToneSheet(paparazzi: Paparazzi, night: Boolean) {
        val icons = listOf(
            R.drawable.ic_car_phone, R.drawable.ic_car_obd, R.drawable.ic_car_settings,
            R.drawable.ic_car_browser, R.drawable.ic_car_data, R.drawable.ic_car_notifications,
        ).mapNotNull { res -> runCatching { ContextCompat.getDrawable(paparazzi.context, res)?.toBitmap(96, 96) }.getOrNull() }
        paparazzi.snapshot {
            CarTheme(night = night, theme = DashGridTheme) {
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().padding(40.dp)) {
                        Text(
                            if (night) "Dock icons — dark theme" else "Dock icons — light theme",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "The same icons, one grayscale tone each. Shape, logo and detail stay; only the brightness range changes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(28.dp))
                        DockIconTone.entries.forEach { tone ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                                Text(
                                    stringResource(tone.labelRes),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(width = 150.dp, height = 24.dp),
                                )
                                icons.forEach { icon ->
                                    DockIconPreview(icon, tone, null, Modifier.size(64.dp))
                                }
                            }
                            Spacer(Modifier.height(20.dp))
                        }
                    }
                }
            }
        }
    }

/** A drawn stand-in for a navigation app's map: streets, a route and the car's position. */
@androidx.compose.runtime.Composable
private fun MapStandIn(night: Boolean, modifier: Modifier) {
    val ground = if (night) Color(0xFF1B2330) else Color(0xFFE8EAED)
    val street = if (night) Color(0xFF34404F) else Color.White
    Canvas(modifier.fillMaxSize().background(ground)) {
        val w = size.width
        val h = size.height
        for (i in 1..5) drawLine(street, Offset(0f, h * i / 6f), Offset(w, h * i / 6f - 30f), strokeWidth = 14f)
        for (i in 1..4) drawLine(street, Offset(w * i / 5f, 0f), Offset(w * i / 5f + 40f, h), strokeWidth = 14f)
        val route = Path().apply { moveTo(w * 0.5f, h); lineTo(w * 0.52f, h * 0.55f); lineTo(w * 0.8f, h * 0.5f) }
        drawPath(route, Color(0xFF4285F4), style = Stroke(width = 16f))
        drawCircle(Color.White, 18f, Offset(w * 0.505f, h * 0.85f))
        drawCircle(Color(0xFF4285F4), 12f, Offset(w * 0.505f, h * 0.85f))
    }
}
