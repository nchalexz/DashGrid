package net.prototux.aare.workspace

import android.util.Log
import net.prototux.aare.Settings
import net.prototux.aare.ui.car.DashCard

/**
 * Reads and writes the workspace configuration through [Settings]' one `workspaces` string.
 *
 * The first load on an install that predates workspaces migrates AARE's single-dashboard card
 * order ([Settings.dashboardCards]) into the first workspace, so an update keeps what the driver
 * had. A config that can't be parsed is left in place under a backup key and a fresh default is
 * used, so a bug (or a hand-edited file) can't leave the car screen empty without a trace.
 *
 * Main thread, like the rest of the runtime.
 */
class WorkspaceStore(
    private val settings: Settings,
    /** The first workspace's name when the old dashboard is migrated (localized by the caller). */
    private val migratedName: String = "Home",
    /** The preinstalled VPM cockpit workspace's name (localized by the caller). */
    private val carPresetName: String = "Car",
) {

    /** The saved configuration, migrated from the old dashboard on first use. */
    fun load(): WorkspaceConfig {
        val raw = settings.workspacesJson
        val decoded = WorkspaceJson.decode(raw)
        if (decoded != null) {
            val preset = decoded.withCarPreset(carPresetName)
            val migrated = preset.withVpmCockpit()
            if (migrated != decoded) {
                save(migrated)
                if (preset != decoded) Log.i(TAG, "added the preinstalled Car workspace (${VPM_COCKPIT_WIDGET_ID})")
                if (migrated != preset) Log.i(TAG, "moved the legacy OBD cockpit to the VPM cockpit")
            }
            return migrated
        }
        if (!raw.isNullOrBlank()) {
            Log.w(TAG, "saved workspaces can't be read (${raw.length} chars); keeping a copy under workspaces_broken")
            settings.workspacesBrokenJson = raw
        }
        val migrated = migrateOldDashboard().withCarPreset(carPresetName).withVpmCockpit()
        save(migrated)
        return migrated
    }

    /** Writes [config]; [WorkspaceConfig.version] goes with it. */
    fun save(config: WorkspaceConfig) {
        settings.workspacesJson = WorkspaceJson.encode(config)
    }

    /** [load], changed by [transform] and saved when [transform] produces a different config. */
    fun update(transform: (WorkspaceConfig) -> WorkspaceConfig): WorkspaceConfig {
        val current = load()
        val next = transform(current)
        if (next != current) save(next)
        return next
    }

    /** Remembers [id] as the workspace the next connection should show; no-op when it isn't one. */
    fun setLast(id: String?) {
        val config = load()
        val next = config.withLast(id)
        if (next != config) save(next)
    }

    /**
     * The workspace config for an install that only knew the single dashboard: its cards, in the
     * order the user chose, become widgets of the first workspace. Unknown card ids (a widget
     * removed in a later version) are dropped by [DashCard.parse].
     */
    private fun migrateOldDashboard(): WorkspaceConfig {
        val cards = DashCard.parse(settings.dashboardCards).map { it.id }
        val migrated = migrateDashboard(cards, migratedName)
        Log.i(TAG, "migrated the old dashboard (${cards.joinToString(",")}) into the first workspace")
        return migrated
    }

    private companion object {
        const val TAG = "WorkspaceStore"
    }
}
