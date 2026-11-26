package net.prototux.aare.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import net.prototux.aare.workspace.WorkspaceItem

/**
 * The one [AppWidgetHost] of the process, and the small amount of bookkeeping a host needs:
 * allocating and releasing ids, starting and stopping the host's listener, creating the views that
 * draw on the car screen, and reporting each widget's cell size back to its provider.
 *
 * The host is created with the application context (its package is the host's identity), but every
 * view is created with the context passed to [viewFor]: the Compose UI runs inside a
 * [android.app.Presentation] on AARE's virtual display, so that context carries the car screen's
 * density. That keeps a widget's dp the same as the values its provider returned.
 *
 * Everything here runs on the main thread, like the rest of the car runtime. A worker thread must
 * not touch an AppWidgetHost.
 */
object AndroidWidgets {
    private const val TAG = "AndroidWidgets"

    /**
     * Identifies this app's host to the system. A constant, because widget ids are bound to the
     * host that allocated them: a changing id would lose every widget on an app update.
     */
    private const val HOST_ID = 0x44475244 // "DGRD"

    private const val PREFS = "android_widgets"
    private const val KEY_IDS = "allocated_ids"

    private lateinit var appContext: Context
    private val manager: AppWidgetManager get() = AppWidgetManager.getInstance(appContext)

    private var host: AppWidgetHost? = null
    private var listening = 0
    // Compose state: the phone preview's widget cells follow a car screen starting or stopping.
    private var carScreens by mutableIntStateOf(0)
    private var pickerDepth = 0

    /**
     * A real car screen is showing. The phone preview would otherwise create a second live view of
     * the same widget (one host view per id), so it shows a placeholder while the car has it.
     */
    val carScreenActive: Boolean get() = carScreens > 0

    /** The widget ids this app allocated, so an id no workspace uses can be released again. */
    private val allocated: MutableSet<Int> by lazy {
        (prefs.getStringSet(KEY_IDS, emptySet()) ?: emptySet()).mapNotNull { it.toIntOrNull() }.toMutableSet()
    }

    private val prefs: SharedPreferences
        get() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun init(context: Context) {
        if (!::appContext.isInitialized) appContext = context.applicationContext
    }

    /**
     * Starts listening (the first caller; a nested session only counts) and returns the host.
     * Called when the car screen starts and while the phone's widget picker is open.
     */
    fun begin(context: Context, carScreen: Boolean = false): AppWidgetHost {
        init(context)
        listening++
        if (carScreen) carScreens++
        val h = ensureHost()
        if (listening == 1) {
            runCatching { h.startListening() }.onFailure { Log.w(TAG, "startListening failed: ${it.message}") }
        }
        return h
    }

    /**
     * Ends a [begin] session. When the last one ends the host stops listening and is dropped: its
     * views were inflated with the presentation context of the session that just ended, and keeping
     * them would pin that window (and its virtual display) in memory until the process dies. The
     * widget ids stay bound to the same host id, so the next session's host recreates the views.
     */
    fun end(carScreen: Boolean = false) {
        if (listening == 0) return
        listening--
        if (carScreen && carScreens > 0) carScreens--
        if (listening == 0) {
            runCatching { host?.stopListening() }.onFailure { Log.w(TAG, "stopListening: ${it.message}") }
            host = null
        }
    }

    /** Marks the phone's picker open, so a reconcile doesn't release an id being bound. */
    fun pickerOpened() { pickerDepth++ }

    fun pickerClosed() { pickerDepth = (pickerDepth - 1).coerceAtLeast(0) }

    private fun ensureHost(): AppWidgetHost {
        host?.let { return it }
        val h = AppWidgetHost(appContext, HOST_ID)
        host = h
        return h
    }

    /** Allocates an id for a new widget (the host must be listening; see [begin]). -1 on failure. */
    fun allocate(context: Context): Int {
        init(context)
        val h = ensureHost()
        val temporary = listening == 0
        if (temporary) runCatching { h.startListening() }
        return try {
            val id = h.allocateAppWidgetId()
            if (id >= 0) {
                allocated.add(id)
                saveIds()
            }
            id
        } catch (e: Exception) {
            Log.w(TAG, "allocateAppWidgetId failed: ${e.message}")
            -1
        } finally {
            if (temporary) runCatching { h.stopListening() }
        }
    }

    /** Whether this app may bind [id] to [provider], or the user must confirm it first. */
    fun canBind(id: Int, provider: ComponentName): Boolean = try {
        manager.bindAppWidgetIdIfAllowed(id, provider)
    } catch (e: Exception) {
        Log.w(TAG, "bindAppWidgetIdIfAllowed: ${e.message}")
        false
    }

    /** The system dialog that asks the user to let this app host [provider]'s widget. */
    fun bindIntent(id: Int, provider: ComponentName): Intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
    }

    /** The provider's own configuration screen after binding, or null when it has none. */
    fun configureIntent(id: Int, info: AppWidgetProviderInfo): Intent? {
        val configure = info.configure ?: return null
        return Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
            component = configure
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        }
    }

    /** The provider info of [id], or null when its app was removed or the binding was lost. */
    fun info(id: Int): AppWidgetProviderInfo? = runCatching { manager.getAppWidgetInfo(id) }.getOrNull()

    /**
     * A view that draws [id] at the density of [context]. The host keeps one view per id, so it is
     * detached from an earlier parent first: the pager can move a widget between pages without the
     * old page still holding it. Null when the provider is unavailable.
     */
    fun viewFor(context: Context, id: Int): AppWidgetHostView? {
        val info = info(id) ?: return null
        val h = ensureHost()
        val view = try {
            h.createView(context, id, info)
        } catch (e: Exception) {
            Log.w(TAG, "createView($id): ${e.message}")
            return null
        }
        (view.parent as? ViewGroup)?.removeView(view)
        view.setPadding(0, 0, 0, 0)
        view.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        return view
    }

    /**
     * Reports the cells a widget occupies to its provider (dp), the documented way a host tells a
     * widget how much room it has. Providers that don't resize ignore it; RemoteViews that do lay
     * themselves out for exactly these dimensions, without any scaling of our own.
     */
    fun resize(id: Int, widthDp: Int, heightDp: Int) {
        val w = widthDp.coerceAtLeast(1)
        val h = heightDp.coerceAtLeast(1)
        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, w)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, h)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, w)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, h)
        }
        runCatching { manager.updateAppWidgetOptions(id, options) }
            .onFailure { Log.w(TAG, "updateAppWidgetOptions($id): ${it.message}") }
    }

    /** Releases a widget id (and its host view). Safe when the id was already released. */
    fun delete(id: Int) {
        val h = ensureHost()
        runCatching {
            // deleteAppWidgetId needs the service connection the listener sets up; a session that
            // was ended already (the editor releasing an id of a widget that wasn't drawn yet)
            // opens a short one.
            val temporary = listening == 0
            if (temporary) h.startListening()
            h.deleteAppWidgetId(id)
            if (temporary) h.stopListening()
        }.onFailure { Log.w(TAG, "deleteAppWidgetId($id): ${it.message}") }
        allocated.remove(id)
        saveIds()
    }

    /**
     * Releases ids no workspace refers to any more: a picker that was interrupted between the
     * allocation and the workspace save leaves one behind. Never runs while the phone's picker is
     * open, and never touches an id a workspace still holds.
     */
    fun reconcile(referenced: Set<Int>) {
        if (pickerDepth > 0) return
        // At this point no picker is mid-flow, so an allocated id no workspace names is stale: a
        // flow interrupted between the allocation and the workspace save, a removed workspace, or
        // imported settings. Releasing it here also releases a binding the user can no longer see.
        val orphans = allocated.filter { it !in referenced }
        for (id in orphans) {
            Log.i(TAG, "releasing widget id $id: no workspace refers to it")
            delete(id)
        }
    }

    /** The ids the current workspace config holds, for [reconcile]. */
    fun referencedIds(items: Iterable<WorkspaceItem>): Set<Int> = items.mapNotNullTo(HashSet()) { it.appWidgetId }

    private fun saveIds() {
        prefs.edit().putStringSet(KEY_IDS, allocated.mapTo(HashSet()) { it.toString() }).apply()
    }
}
