package net.prototux.aare.ui.car.obd

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import net.prototux.aare.R
import net.prototux.aare.obd.ObdReadings
import net.prototux.aare.obd.Vag
import net.prototux.aare.obdapp.ObdApp
import net.prototux.aare.ui.theme.CarTheme
import net.prototux.aare.ui.theme.DashGridTheme
import org.junit.Rule
import org.junit.Test

/** The made-up drive the Cockpit mockups show. */
private fun drive() = ObdApp.Snapshot(
    readings = ObdReadings(
        rpm = 1840f, speed = 96f, boost = 0.62f, coolant = 86f, intakeAir = 34f,
        battery = 14.6f, fuel = 58f, outside = 12f, load = 48f,
        manifoldKpa = 162f, barometricKpa = 100f, maf = 42.5f, fuelRate = 4.8f,
        throttle = 21f, pedal = 19f, oil = 95f, egt1 = 402f, egt2 = 318f, railKpa = 82_000f,
        // What UDS adds once the car's identifiers are known (made up here).
        vag = mapOf(
            Vag.Signal("7E1", 0x2104, R.string.obd_vag_gearbox_oil, "°C", offset = -40f, min = 0f, max = 150f) to 84f,
            Vag.Signal("7E0", 0x11C2, R.string.obd_vag_egt3, "°C", scale = 0.1f, offset = -40f) to 286f,
            Vag.Signal("7E0", 0x11C4, R.string.obd_vag_injector1, "mg", scale = 0.01f, offset = -10f, decimals = 2) to -0.4f,
        ),
    ),
    gear = 5,
    kmPerLiter = 20.0f,
    showsBoost = true,
    gauge = 0.62f,
)

/**
 * The Cockpit's Values page on a 1280 × 720 head unit: three native-style dials and the tiles of
 * one group (engine, temperatures, fuel, gearbox).
 * Run `./gradlew :app:recordPaparazziDebug -Pmockups --tests '*ObdValuesMockup*'`.
 */
class ObdValuesMockup {
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
        ),
        theme = "android:Theme.Material.NoActionBar",
    )

    @Test
    fun values() = page(0, CockpitStyle.VW)

    @Test
    fun valuesPorsche() = page(0, CockpitStyle.PORSCHE)

    @Test
    fun valuesHybrid() = page(0, CockpitStyle.HYBRID)

    @Test
    fun valuesTemperatures() = page(1, CockpitStyle.VW)

    @Test
    fun valuesPorscheTemperatures() = page(1, CockpitStyle.PORSCHE)

    private fun page(group: Int, style: CockpitStyle) {
        paparazzi.snapshot {
            CarTheme(night = true, theme = DashGridTheme) {
                Box(Modifier.fillMaxSize().background(cockpitBackdrop()).padding(16.dp)) {
                    CompositionLocalProvider(
                        LocalObdScale provides 1.2f,
                        LocalTextStyle provides TextStyle(lineHeight = TextUnit.Unspecified),
                    ) {
                        ValuesTab(drive(), wide = true, initialGroup = group, style = style)
                    }
                }
            }
        }
    }
}

/**
 * The first tab of the full window: the same card, stretched over a 1280 × 720 head unit screen.
 */
class ObdOverviewMockup {
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
        ),
        theme = "android:Theme.Material.NoActionBar",
    )

    @Test
    fun overview() {
        paparazzi.snapshot {
            CarTheme(night = true, theme = DashGridTheme) {
                Box(Modifier.fillMaxSize().background(cockpitBackdrop()).padding(16.dp)) {
                    CompositionLocalProvider(LocalTextStyle provides TextStyle(lineHeight = TextUnit.Unspecified)) {
                        CockpitCard(drive(), style = CockpitStyle.HYBRID)
                    }
                }
            }
        }
    }
}

/**
 * The compact card for a live app pane: three dials and five main tiles on a 640 × 360 pane, as it
 * would sit in a workspace cell.
 */
class ObdCardMockup {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(
            screenWidth = 640,
            screenHeight = 360,
            xdpi = 160,
            ydpi = 160,
            density = Density.MEDIUM,
            orientation = ScreenOrientation.LANDSCAPE,
            softButtons = false,
        ),
        theme = "android:Theme.Material.NoActionBar",
    )

    @Test
    fun card() = card(CockpitStyle.VW)

    @Test
    fun cardPorsche() = card(CockpitStyle.PORSCHE)

    @Test
    fun cardHybrid() = card(CockpitStyle.HYBRID)

    private fun card(style: CockpitStyle) {
        paparazzi.snapshot {
            CarTheme(night = true, theme = DashGridTheme) {
                Box(Modifier.fillMaxSize().background(cockpitBackdrop()).padding(10.dp)) {
                    CompositionLocalProvider(LocalTextStyle provides TextStyle(lineHeight = TextUnit.Unspecified)) {
                        CockpitCard(drive(), style = style)
                    }
                }
            }
        }
    }
}
