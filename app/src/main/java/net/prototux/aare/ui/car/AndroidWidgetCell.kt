package net.prototux.aare.ui.car

import android.appwidget.AppWidgetHostView
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import net.prototux.aare.R
import net.prototux.aare.ui.theme.LocalCarDimens
import net.prototux.aare.widgets.AndroidWidgets
import net.prototux.aare.workspace.WorkspaceItem
import kotlin.math.roundToInt

/**
 * A standard Android App Widget in its workspace cells: the provider's own live RemoteViews, not a
 * screenshot. The view is created with the car screen's context, so its dp follow the virtual
 * display's density, and the cells' size in dp is reported back to the provider so a resizable
 * widget can reflow to them. No filter, scale or recolour is applied: the widget is drawn exactly
 * as its app designed it.
 *
 * Only the workspace on screen hosts the widget (the others show a quiet placeholder): two live
 * views of one bound widget would fight over the same state, and a page beside the current one
 * doesn't need to run a widget's timers.
 */
@Composable
internal fun AndroidWidgetCell(item: WorkspaceItem, state: CarUiState, active: Boolean, modifier: Modifier) {
    val context = LocalContext.current
    val id = item.appWidgetId
    // Read the revision so a provider that appeared, went away or changed re-reads its info.
    val revision = state.appWidgetRevision
    val info = remember(id, revision) { id?.let(AndroidWidgets::info) }
    when {
        id == null -> WidgetPlaceholder(Icons.Rounded.Widgets, stringResource(R.string.workspaces_android_widget), null)
        // The phone preview and the car screen would each create a live view for the same id; the
        // car is the one that must have it, so the preview says where the widget is.
        state.preview && AndroidWidgets.carScreenActive -> WidgetPlaceholder(
            Icons.Rounded.Widgets,
            runCatching { info?.label }.getOrNull() ?: stringResource(R.string.workspaces_android_widget),
            stringResource(R.string.widget_on_car_screen),
        )
        info == null -> WidgetPlaceholder(
            Icons.Rounded.Widgets,
            stringResource(R.string.widget_unavailable),
            stringResource(R.string.widget_unavailable_hint),
        )
        !active -> WidgetPlaceholder(
            Icons.Rounded.Widgets,
            runCatching { info.label }.getOrNull() ?: stringResource(R.string.workspaces_android_widget),
            stringResource(R.string.widget_inactive),
        )
        else -> {
            val shape = RoundedCornerShape(LocalCarDimens.current.cardRadius)
            val density = LocalDensity.current
            var lastSize by remember(id) { mutableStateOf(IntSize.Zero) }
            Box(modifier.clip(shape)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize().onSizeChanged { size ->
                        if (size == lastSize || size.width <= 0 || size.height <= 0) return@onSizeChanged
                        lastSize = size
                        // The remote views lay out in dp: tell the provider the cells it really has.
                        val w = with(density) { size.width.toDp().value }.roundToInt()
                        val h = with(density) { size.height.toDp().value }.roundToInt()
                        AndroidWidgets.resize(id, w, h)
                    },
                    factory = { ctx ->
                        AndroidWidgets.viewFor(ctx, id) ?: View(ctx)
                    },
                    update = { view ->
                        // The host pushes RemoteViews updates into this view itself; only keep the
                        // widget flush with its cells (no launcher padding of our own).
                        if (view is AppWidgetHostView) view.setPadding(0, 0, 0, 0)
                    },
                )
            }
        }
    }
}
