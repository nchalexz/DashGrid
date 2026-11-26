package net.prototux.aare.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.prototux.aare.R
import net.prototux.aare.widgets.AndroidWidgetCatalog
import net.prototux.aare.widgets.AndroidWidgetChoice
import net.prototux.aare.widgets.AndroidWidgets

/**
 * The phone's picker for a standard Android App Widget. It lists every provider installed on the
 * phone, allocates an id, binds it (asking the system's permission dialog when this app may not
 * bind that provider yet) and, when the provider has one, opens its own configuration screen.
 * Both system screens open on the phone, as they must; the widget then appears on the car screen.
 *
 * [onAdd] adds the bound widget to a workspace and returns false when it can't (the grid is full):
 * the id is released again in that case, so a failed add never leaves a bound widget behind.
 */
@Composable
internal fun AndroidWidgetPicker(
    onDismiss: () -> Unit,
    onAdd: (AndroidWidgetChoice, appWidgetId: Int) -> Boolean,
) {
    val ctx = LocalContext.current
    val choices by produceState<List<AndroidWidgetChoice>?>(initialValue = null, key1 = Unit) {
        value = withContext(Dispatchers.IO) { AndroidWidgetCatalog.choices(ctx) }
    }
    val toast: (Int) -> Unit = { res -> Toast.makeText(ctx, res, Toast.LENGTH_LONG).show() }
    // The flow outlives recompositions (the editor can save while the picker is open), so its
    // callbacks read the latest lambdas instead of the ones captured when it was created.
    val currentDismiss by rememberUpdatedState(onDismiss)
    val currentAdd by rememberUpdatedState(onAdd)
    val flow = remember(ctx) {
        WidgetAddFlow(ctx, { currentDismiss() }, { choice, id -> currentAdd(choice, id) }, toast)
    }
    val bindLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        flow.onBindResult(result)
    }
    val configureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        flow.onConfigureResult(result)
    }
    SideEffect {
        flow.launchBind = { intent -> runCatching { bindLauncher.launch(intent) }.onFailure { flow.discard(R.string.widget_bind_failed) } }
        flow.launchConfigure = { intent -> runCatching { configureLauncher.launch(intent) }.onFailure { flow.discard(R.string.widget_configure_failed) } }
    }

    DisposableEffect(Unit) {
        AndroidWidgets.pickerOpened()
        onDispose { AndroidWidgets.pickerClosed() }
    }

    Dialog(
        onDismissRequest = { if (!flow.busy) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = !flow.busy, dismissOnClickOutside = !flow.busy),
    ) {
        Surface(shape = RoundedCornerShape(24.dp), tonalElevation = 6.dp) {
            Column(Modifier.fillMaxWidth().heightIn(max = 640.dp).padding(vertical = 8.dp)) {
                Text(
                    stringResource(R.string.widget_picker_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                Text(
                    stringResource(R.string.widget_picker_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Spacer(Modifier.height(12.dp))
                when (val list = choices) {
                    null -> Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    else -> if (list.isEmpty()) Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.widget_picker_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else LazyColumn(Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
                        items(list, key = { it.providerKey }) { choice ->
                            WidgetChoiceRow(choice) { flow.startAdd(choice) }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss, enabled = !flow.busy) { Text(stringResource(R.string.common_cancel)) }
                }
            }
        }
    }
}

/**
 * The allocate → bind → configure → add flow, kept out of composition so the two system activities
 * can return into it in any order. The launchers are installed by the composable through
 * [launchBind] and [launchConfigure]; [pending] is the provider whose system screen is open.
 */
private class WidgetAddFlow(
    private val ctx: Context,
    private val onDismiss: () -> Unit,
    private val onAdd: (AndroidWidgetChoice, Int) -> Boolean,
    private val toast: (Int) -> Unit,
) {
    var pending by mutableStateOf<Pair<AndroidWidgetChoice, Int>?>(null)
        private set
    var launchBind: ((Intent) -> Unit)? = null
    var launchConfigure: ((Intent) -> Unit)? = null

    /** A system screen is open: the picker must not be dismissed from under it. */
    val busy: Boolean get() = pending != null

    fun startAdd(choice: AndroidWidgetChoice) {
        val id = AndroidWidgets.allocate(ctx)
        if (id < 0) {
            toast(R.string.widget_allocate_failed)
            return
        }
        if (AndroidWidgets.canBind(id, choice.component)) {
            afterBind(choice, id)
        } else {
            pending = choice to id
            val launch = launchBind
            if (launch == null) discard(R.string.widget_bind_failed) else launch(AndroidWidgets.bindIntent(id, choice.component))
        }
    }

    fun onBindResult(result: ActivityResult) {
        val (choice, id) = pending ?: return
        if (result.resultCode == Activity.RESULT_OK) afterBind(choice, id) else discard(R.string.widget_bind_failed)
    }

    fun onConfigureResult(result: ActivityResult) {
        val (choice, id) = pending ?: return
        if (result.resultCode == Activity.RESULT_OK) finish(choice, id) else discard(R.string.widget_configure_cancelled)
    }

    private fun afterBind(choice: AndroidWidgetChoice, id: Int) {
        val info = AndroidWidgets.info(id)
        if (info == null) {
            discard(R.string.widget_unavailable)
            return
        }
        val configure = AndroidWidgets.configureIntent(id, info)
        if (configure == null) {
            finish(choice, id)
            return
        }
        pending = choice to id
        val launch = launchConfigure
        if (launch == null) discard(R.string.widget_configure_failed) else launch(configure)
    }

    private fun finish(choice: AndroidWidgetChoice, id: Int) {
        pending = null
        if (onAdd(choice, id)) onDismiss()
        else {
            AndroidWidgets.delete(id)
            toast(R.string.widget_no_space)
        }
    }

    fun discard(res: Int? = null) {
        pending?.let { (_, id) -> AndroidWidgets.delete(id) }
        pending = null
        res?.let(toast)
    }
}

/** One provider: the app's icon and name, the widget's name, its minimum size and its preview. */
@Composable
private fun WidgetChoiceRow(choice: AndroidWidgetChoice, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
            val icon = choice.appIcon
            if (icon != null) Image(icon.asImageBitmap(), choice.appLabel, Modifier.size(36.dp))
            else Icon(Icons.Rounded.Widgets, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(choice.widgetLabel, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                choice.appLabel + " · " + stringResource(R.string.widget_provider_size, choice.minWidthDp, choice.minHeightDp) +
                    " · " + stringResource(if (choice.resizable) R.string.widget_provider_resizable else R.string.widget_provider_fixed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        choice.preview?.let { preview ->
            Spacer(Modifier.width(12.dp))
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow), contentAlignment = Alignment.Center) {
                Image(preview.asImageBitmap(), null, Modifier.size(56.dp))
            }
        }
    }
}

/** The cells a new widget asks for, from the provider's minimum size and a typical car cell. */
internal fun defaultWidgetRect(minWidthDp: Int, minHeightDp: Int): Pair<Int, Int> {
    val cols = ((minWidthDp + 99) / 100).coerceIn(1, 10)
    val rows = ((minHeightDp + 79) / 80).coerceIn(1, 6)
    return cols to rows
}
