package net.prototux.aare.ui.car.vpm

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import net.prototux.aare.ui.theme.CarTheme
import net.prototux.aare.ui.theme.CarThemes
import net.prototux.aare.vpm.VPM_BRIDGE_VERSION
import net.prototux.aare.vpm.VpmCategory
import net.prototux.aare.vpm.VpmSnapshot
import net.prototux.aare.vpm.parseVpmSnapshot
import org.junit.Rule
import org.junit.Test

/** The values from VPM's own mockups (docs/dashgrid 01-main and 02-categories), so the drawings can be compared. */
private fun vpmMockSnapshot(): VpmSnapshot = requireNotNull(parseVpmSnapshot(
    version = VPM_BRIDGE_VERSION,
    connection = "demo",
    status = "ДЕМО",
    source = "",
    updatedAt = 1_700_000_000_000,
    signalsJson = """
        [
          {"id":"vehicle.speed","label":"Скорость","value":"105","unit":"км/ч","raw":105,"category":"engine"},
          {"id":"engine.rpm","label":"Обороты","value":"1874","unit":"об/мин","raw":1874,"category":"engine"},
          {"id":"engine.load_pct","label":"Engine load","value":"56","unit":"%","raw":56,"category":"engine"},
          {"id":"engine.throttle_pct","label":"Throttle","value":"24","unit":"%","raw":24,"category":"engine"},
          {"id":"engine.pedal_pct","label":"Pedal","value":"23","unit":"%","raw":23,"category":"engine"},
          {"id":"engine.map_kpa","label":"Intake pressure","value":"161","unit":"кПа","raw":161,"category":"engine"},
          {"id":"engine.baro_kpa","label":"Atmospheric","value":"100","unit":"кПа","raw":100,"category":"engine"},
          {"id":"engine.maf_gs","label":"Air flow","value":"45,3","unit":"г/с","raw":45.3,"category":"engine"},
          {"id":"electrical.voltage_v","label":"Battery","value":"14,8","unit":"В","raw":14.8,"category":"engine"},
          {"id":"engine.boost_kpa","label":"Наддув","value":"0,62","unit":"бар","raw":0.62,"category":"engine"},

          {"id":"engine.coolant_c","label":"ОЖ","value":"86","unit":"°C","raw":86,"category":"temperatures"},
          {"id":"engine.oil_c","label":"Масло","value":"95","unit":"°C","raw":95,"category":"temperatures"},
          {"id":"engine.intake_air_c","label":"Intake air","value":"31","unit":"°C","raw":31,"category":"temperatures"},
          {"id":"engine.ambient_c","label":"Ambient","value":"18","unit":"°C","raw":18,"category":"temperatures"},
          {"id":"emissions.catalyst_c","label":"Catalyst","value":"412","unit":"°C","raw":412,"category":"temperatures"},

          {"id":"fuel.level_pct","label":"Топливо","value":"58","unit":"%","raw":58,"category":"fuel"},
          {"id":"fuel.l100_km","label":"Расход","value":"5,0","unit":"л/100 км","raw":5.0,"category":"fuel"},
          {"id":"fuel.rate_lph","label":"Fuel rate","value":"1,2","unit":"л/ч","raw":1.2,"category":"fuel"},
          {"id":"fuel.trim_short_pct","label":"Trim short","value":"−2,3","unit":"%","raw":-2.3,"category":"fuel"},
          {"id":"fuel.trim_long_pct","label":"Trim long","value":"1,6","unit":"%","raw":1.6,"category":"fuel"},

          {"id":"vehicle.gear","label":"Передача","value":"5","unit":"","raw":5,"category":"gearbox"},

          {"id":"engine.boost_kpa","label":"Boost","value":"0,62","unit":"бар","raw":0.62,"category":"turbo"},
          {"id":"engine.map_kpa","label":"Intake pressure","value":"161","unit":"кПа","raw":161,"category":"turbo"},
          {"id":"engine.baro_kpa","label":"Atmospheric","value":"100","unit":"кПа","raw":100,"category":"turbo"},

          {"id":"electrical.voltage_v","label":"Battery","value":"14,8","unit":"В","raw":14.8,"category":"diagnostics"},
          {"id":"fuel.trim_short_pct","label":"Trim short","value":"−2,3","unit":"%","raw":-2.3,"category":"diagnostics"},
          {"id":"fuel.trim_long_pct","label":"Trim long","value":"1,6","unit":"%","raw":1.6,"category":"diagnostics"},
          {"id":"engine.egr_pct","label":"EGR","value":"0","unit":"%","raw":0.0,"category":"diagnostics"},
          {"id":"emissions.catalyst_c","label":"Catalyst","value":"412","unit":"°C","raw":412,"category":"diagnostics"},
          {"id":"engine.run_time_s","label":"Run time","value":"812","unit":"с","raw":812,"category":"diagnostics"}
        ]
    """.trimIndent(),
))

/**
 * JVM renders of the native cockpit for the README: VPM's 0.4.1 composition with VPM's own mock
 * values. These are drawings, not photographs of a head unit, and the data is VPM's demo sample.
 */
class VpmCockpitMockups {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(
            screenWidth = 1920,
            screenHeight = 720,
            xdpi = 160,
            ydpi = 160,
            density = Density.MEDIUM,
            orientation = ScreenOrientation.LANDSCAPE,
            softButtons = false,
        ),
        theme = "android:Theme.Material.NoActionBar",
    )

    @Test fun cockpitMainNight() = shot("vpm-cockpit-main-night", night = true, page = 0)
    @Test fun cockpitCategoriesNight() = shot("vpm-cockpit-categories-night", night = true, page = 1)
    @Test fun cockpitMainDay() = shot("vpm-cockpit-main-day", night = false, page = 0)
    @Test fun cockpitCategoriesDay() = shot("vpm-cockpit-categories-day", night = false, page = 1)

    private fun shot(name: String, night: Boolean, page: Int) {
        val snapshot = vpmMockSnapshot()
        paparazzi.snapshot(name) {
            CarTheme(night = night, theme = CarThemes.byId("aare")) {
                VpmCockpitView(
                    snapshot = snapshot,
                    modifier = Modifier.fillMaxSize(),
                    initialPage = page,
                    initialCategory = VpmCategory.ENGINE,
                )
            }
        }
    }
}

/** The same cockpit on a 1280 × 480 unit, where the gauges and the grid must still fit. */
class VpmCockpitSmallMockups {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(
            screenWidth = 1280,
            screenHeight = 480,
            xdpi = 160,
            ydpi = 160,
            density = Density.MEDIUM,
            orientation = ScreenOrientation.LANDSCAPE,
            softButtons = false,
        ),
        theme = "android:Theme.Material.NoActionBar",
    )

    @Test fun cockpitMainSmall() = shot("vpm-cockpit-main-1280x480", page = 0)
    @Test fun cockpitCategoriesSmall() = shot("vpm-cockpit-categories-1280x480", page = 1)

    private fun shot(name: String, page: Int) {
        val snapshot = vpmMockSnapshot()
        paparazzi.snapshot(name) {
            CarTheme(night = true, theme = CarThemes.byId("aare")) {
                VpmCockpitView(
                    snapshot = snapshot,
                    modifier = Modifier.fillMaxSize(),
                    initialPage = page,
                    initialCategory = VpmCategory.ENGINE,
                )
            }
        }
    }
}
