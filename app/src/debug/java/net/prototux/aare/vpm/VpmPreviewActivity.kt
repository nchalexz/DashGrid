package net.prototux.aare.vpm

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Build
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import net.prototux.aare.ui.car.vpm.VpmCockpitView
import net.prototux.aare.ui.theme.CarTheme
import net.prototux.aare.ui.theme.CarThemes

/**
 * DEBUG ONLY: the cockpit on the real car display, with the demo values of VPM's own mockups, for
 * checking the layout and gestures on an emulator or a head unit. It is not part of a release build
 * and it never reads an adapter: the snapshot is a fixture, exactly like VPM's DEMO mode.
 *
 * `adb shell am start -n io.dashgrid.app/net.prototux.aare.vpm.VpmPreviewActivity --ei page 1`
 */
class VpmPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (intent.getBooleanExtra("car_shell", false)) {
            // The real car screen, with the preset workspaces and the rail: `--ez car_shell true`.
            startActivity(Intent(this, net.prototux.aare.ui.CarPreviewActivity::class.java))
            finish()
            return
        }
        val page = intent.getIntExtra("page", 0)
        val night = intent.getBooleanExtra("night", true)
        setContent {
            CarTheme(night = night, theme = CarThemes.byId("aare")) {
                VpmCockpitView(
                    snapshot = VpmPreviewData.snapshot(),
                    modifier = Modifier.fillMaxSize(),
                    initialPage = page,
                )
            }
        }
        // A head unit has no status bar; hide the emulator's so the screenshot is the cockpit only.
        // After setContent: the window's decor view (and its insets controller) exists only then.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.systemBars())
        }
    }
}
