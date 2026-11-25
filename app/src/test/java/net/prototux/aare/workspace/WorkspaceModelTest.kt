package net.prototux.aare.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceModelTest {

    // ---------------------------------------------------------------- cyclic switching

    @Test
    fun cyclicIndexWrapsBothWays() {
        assertEquals(0, cyclicIndex(3, 3))
        assertEquals(2, cyclicIndex(-1, 3))
        assertEquals(0, cyclicIndex(0, 3))
        assertEquals(0, cyclicIndex(0, 0))
    }

    @Test
    fun steppingForwardFromTheLastGoesToTheFirst() {
        assertEquals(1, cyclicStep(0, 3, 1))
        assertEquals(2, cyclicStep(1, 3, 1))
        assertEquals(0, cyclicStep(2, 3, 1))
    }

    @Test
    fun steppingBackFromTheFirstGoesToTheLast() {
        assertEquals(2, cyclicStep(0, 3, -1))
        assertEquals(1, cyclicStep(2, 3, -1))
        assertEquals(0, cyclicStep(0, 1, -1))
    }

    @Test
    fun shortestDeltaTakesTheRingNotTheList() {
        assertEquals(-1, cyclicDelta(0, 2, 3)) // 0 -> 2 is one step back, not two forward
        assertEquals(1, cyclicDelta(2, 0, 3))
        assertEquals(0, cyclicDelta(1, 1, 3))
    }

    @Test
    fun virtualPagesMapBackToWorkspacesAndStayNearTheCurrentOne() {
        val center = VIRTUAL_PAGE_CENTER
        assertEquals(0, realIndexFor(virtualPageFor(0, 3), 3))
        assertEquals(2, realIndexFor(virtualPageFor(2, 3), 3))
        val current = virtualPageFor(0, 3)
        val toLast = nearestVirtualPage(2, current, 3)
        assertEquals(2, realIndexFor(toLast, 3))
        assertEquals(-1, toLast - current) // the shortest way back from the first workspace
    }


    // ---------------------------------------------------------------- live app panes

    @Test
    fun paneRequiresAnAppAndRoundTrips() {
        val pane = WorkspaceItem.pane("com.example.maps", WorkspaceRect(0, 0, 6, 6))
        val ws = Workspace.create("Panes", WorkspaceLayout.SPLIT_50).withAddedItem(pane)!!
        val config = WorkspaceConfig(listOf(ws), defaultId = ws.id)
        val decoded = WorkspaceJson.decode(WorkspaceJson.encode(config))!!
        val item = decoded.workspaces.single().items.single()
        assertEquals(WorkspaceItemKind.PANE, item.kind)
        assertEquals("com.example.maps", item.appId)
        assertNull(decoded.workspaces.single().item(item.id)?.widgetId)
    }

    @Test
    fun aPaneWithoutAnAppIsDroppedAndDuplicatePaneAppsCollapse() {
        val json = """{"workspaces":[{"id":"w","name":"W","items":[
            {"kind":"pane","col":0,"row":0,"colSpan":5,"rowSpan":3},
            {"kind":"pane","app":"com.example","col":0,"row":0,"colSpan":5,"rowSpan":3},
            {"kind":"pane","app":"com.example","col":5,"row":0,"colSpan":5,"rowSpan":3},
            {"kind":"pane","app":"com.other","col":0,"row":3,"colSpan":5,"rowSpan":3}]}]}"""
        val ws = WorkspaceJson.decode(json)!!.workspaces.single()
        assertEquals(2, ws.items.size)
        assertEquals(setOf("com.example", "com.other"), ws.items.mapNotNull { it.appId }.toSet())
        assertFalse(ws.items[0].rect.overlaps(ws.items[1].rect))
    }

    // ---------------------------------------------------------------- layouts

    @Test
    fun everyPresetFillsTheGridWithoutOverlaps() {
        for (layout in listOf(WorkspaceLayout.FULL, WorkspaceLayout.SPLIT_50, WorkspaceLayout.SPLIT_70_30,
            WorkspaceLayout.SPLIT_30_70, WorkspaceLayout.THREE, WorkspaceLayout.FOUR)) {
            for (axis in WorkspaceAxis.entries) {
                val rects = layout.slots(axis)
                assertEquals("$layout/$axis", layout.slots(axis).size, layout.slots(axis).distinct().size)
                rects.forEach { r ->
                    assertTrue("$layout/$axis $r inside", r.isInsideGrid())
                    assertTrue("$layout/$axis $r in grid", r.right <= WORKSPACE_COLUMNS && r.bottom <= WORKSPACE_ROWS)
                }
                rects.forEachIndexed { i, a ->
                    rects.drop(i + 1).forEach { b -> assertFalse("$layout/$axis: $a overlaps $b", a.overlaps(b)) }
                }
                val area = rects.sumOf { it.colSpan * it.rowSpan }
                assertEquals("$layout/$axis fills the grid", WORKSPACE_COLUMNS * WORKSPACE_ROWS, area)
            }
        }
    }

    @Test
    fun seventyThirtyIsReallySeventyPercentWide() {
        val (big, small) = WorkspaceLayout.SPLIT_70_30.slots(WorkspaceAxis.HORIZONTAL)
        assertEquals(7, big.colSpan)
        assertEquals(3, small.colSpan)
        val (top, bottom) = WorkspaceLayout.SPLIT_70_30.slots(WorkspaceAxis.VERTICAL)
        assertEquals(4, top.rowSpan)
        assertEquals(2, bottom.rowSpan)
    }

    @Test
    fun portraitTurnKeepsTheSeventyPercentSideAndTheArea() {
        val rects = WorkspaceLayout.SPLIT_70_30.slots(WorkspaceAxis.HORIZONTAL).map { it.portraitRect() }
        rects.forEach {
            assertTrue("$it inside portrait 6×10", it.col + it.colSpan <= WORKSPACE_ROWS && it.row + it.rowSpan <= WORKSPACE_COLUMNS)
        }
        assertEquals(WORKSPACE_COLUMNS * WORKSPACE_ROWS, rects.sumOf { it.colSpan * it.rowSpan })
        // Landscape 70% wide becomes portrait 70% tall: one item 6×7 cells, the other 6×3.
        assertEquals(listOf(42, 18), rects.map { it.colSpan * it.rowSpan })
    }

    // ---------------------------------------------------------------- items and presets

    @Test
    fun addingAnItemTakesTheNextPresetSlot() {
        val ws = Workspace.create("Home", WorkspaceLayout.SPLIT_70_30)
        val withMap = ws.withAddedItem(WorkspaceItem.widget("map", WorkspaceRect(0, 0, 1, 1)))!!
        assertEquals(WorkspaceRect(0, 0, 7, 6), withMap.items.single().rect)
        val withTwo = withMap.withAddedItem(WorkspaceItem.widget("media", WorkspaceRect(0, 0, 1, 1)))!!
        assertEquals(WorkspaceRect(7, 0, 3, 6), withTwo.items.last().rect)
        // A third item doesn't fit the two-slot preset: it goes to free grid space if there is any.
        val three = withTwo.withAddedItem(WorkspaceItem.widget("weather", WorkspaceRect(0, 0, 1, 1)))
        assertNull(three) // both items fill the whole grid
    }

    @Test
    fun addingBeyondThePresetUsesFreeCellsInACustomLayout() {
        val ws = Workspace.create("Home", WorkspaceLayout.FULL)
            .withAddedItem(WorkspaceItem.widget("map", WorkspaceRect(0, 0, 1, 1)))!!
        val custom = ws.copy(layout = WorkspaceLayout.CUSTOM, items = listOf(ws.items.first().copy(rect = WorkspaceRect(0, 0, 5, 3))))
        val added = custom.withAddedItem(WorkspaceItem.widget("media", WorkspaceRect(0, 0, 1, 1)))!!
        assertEquals(WorkspaceLayout.CUSTOM, added.layout)
        assertFalse(added.items.first().rect.overlaps(added.items.last().rect))
    }

    @Test
    fun layoutChangeIsRefusedWhenThereAreMoreItemsThanSlots() {
        val ws = Workspace(
            id = "w", name = "W", layout = WorkspaceLayout.CUSTOM,
            items = listOf(
                WorkspaceItem("a", WorkspaceItemKind.WIDGET, WorkspaceRect(0, 0, 5, 3), widgetId = "map"),
                WorkspaceItem("b", WorkspaceItemKind.WIDGET, WorkspaceRect(5, 0, 5, 3), widgetId = "media"),
                WorkspaceItem("c", WorkspaceItemKind.WIDGET, WorkspaceRect(0, 3, 5, 3), widgetId = "weather"),
            ),
        )
        assertNull(ws.withLayout(WorkspaceLayout.FULL))
        assertNull(ws.withLayout(WorkspaceLayout.SPLIT_50))
        assertNotNull(ws.withLayout(WorkspaceLayout.THREE))
    }

    @Test
    fun removingAnItemReflowsAPresetAndKeepsACustomGridCells() {
        val preset = Workspace(
            id = "w", name = "W", layout = WorkspaceLayout.SPLIT_70_30,
            items = listOf(
                WorkspaceItem("a", WorkspaceItemKind.WIDGET, WorkspaceRect(0, 0, 7, 6), widgetId = "map"),
                WorkspaceItem("b", WorkspaceItemKind.WIDGET, WorkspaceRect(7, 0, 3, 6), widgetId = "media"),
            ),
        )
        val reflowed = preset.withRemovedItem("a")
        assertEquals(WorkspaceRect(0, 0, 7, 6), reflowed.items.single().rect)

        val custom = preset.copy(layout = WorkspaceLayout.CUSTOM, items = listOf(
            preset.items[0].copy(rect = WorkspaceRect(0, 0, 4, 3)),
            preset.items[1].copy(rect = WorkspaceRect(5, 4, 2, 2)),
        ))
        assertEquals(WorkspaceRect(5, 4, 2, 2), custom.withRemovedItem("a").items.single().rect)
    }

    @Test
    fun movingOrResizingAnItemRejectsOverlapAndSwitchesToCustom() {
        val ws = Workspace(
            id = "w", name = "W", layout = WorkspaceLayout.SPLIT_50,
            items = listOf(
                WorkspaceItem("a", WorkspaceItemKind.WIDGET, WorkspaceRect(0, 0, 5, 6), widgetId = "map"),
                WorkspaceItem("b", WorkspaceItemKind.WIDGET, WorkspaceRect(5, 0, 5, 6), widgetId = "media"),
            ),
        )
        assertNull(ws.withItemRect("a", WorkspaceRect(5, 0, 5, 6)))
        val moved = ws.withItemRect("b", WorkspaceRect(5, 0, 5, 3))!!
        assertEquals(WorkspaceLayout.CUSTOM, moved.layout)
        // Overlapping is refused; a smaller move into free cells is accepted.
        val resized = ws.withItemRect("b", WorkspaceRect(5, 0, 2, 6))!!
        assertEquals(2, resized.items.last().rect.colSpan)
    }

    // ---------------------------------------------------------------- config operations

    private fun sampleConfig(): WorkspaceConfig {
        val a = Workspace.create("City", WorkspaceLayout.SPLIT_70_30)
        val b = Workspace.create("Highway", WorkspaceLayout.FULL)
        val c = Workspace.create("Car", WorkspaceLayout.FULL)
        return WorkspaceConfig(listOf(a, b, c), defaultId = b.id, lastId = a.id)
    }

    @Test
    fun startWorkspacePrefersTheLastThenTheDefault() {
        val config = sampleConfig()
        assertEquals("City", config.startWorkspace()?.name)
        assertEquals("Highway", config.copy(lastId = null).startWorkspace()?.name)
        assertEquals("Highway", config.copy(lastId = "gone").startWorkspace()?.name)
    }

    @Test
    fun removingTheActiveOrDefaultWorkspaceMovesToANeighbour() {
        val config = sampleConfig()
        val city = config.workspaces[0].id
        val cityAsDefault = config.withDefault(city).withLast(city)
        val next = cityAsDefault.without(city)!!
        assertEquals("Highway", next.defaultWorkspace()?.name)
        assertEquals("Highway", next.startWorkspace()?.name)
        assertEquals(2, next.workspaces.size)
    }

    @Test
    fun theLastWorkspaceCannotBeRemoved() {
        val one = WorkspaceConfig(listOf(Workspace.create("Only")))
        assertNull(one.without(one.workspaces.single().id))
    }

    @Test
    fun movingAWorkspaceReordersWithoutTouchingOthers() {
        val config = sampleConfig()
        val moved = config.withMoved(config.workspaces[2].id, -2)
        assertEquals(listOf("Car", "City", "Highway"), moved.workspaces.map { it.name })
        assertEquals(config.workspaces[2].id, moved.workspaces[0].id)
    }

    // ---------------------------------------------------------------- JSON and migration

    @Test
    fun jsonRoundTripKeepsEverything() {
        val base = sampleConfig()
        val config = base.withLast(base.workspaces[2].id)
        val decoded = WorkspaceJson.decode(WorkspaceJson.encode(config))!!
        assertEquals(config, decoded)
    }

    @Test
    fun decodeAcceptsAMinimalOrPartlyBrokenDocument() {
        val minimal = """{"workspaces":[{"id":"w1","name":"One","items":[]}]}"""
        val decoded = WorkspaceJson.decode(minimal)!!
        assertEquals(1, decoded.workspaces.size)
        assertEquals("w1", decoded.defaultId)
        assertEquals(WorkspaceLayout.FULL, decoded.workspaces.single().layout)

        val brokenItems = """{"version":1,"workspaces":[{"id":"w1","name":"One","layout":"split50","items":[
            {"kind":"widget","widget":"map","col":0,"row":0,"colSpan":5,"rowSpan":6},
            {"kind":"widget","widget":"map","col":5,"row":0,"colSpan":5,"rowSpan":6},
            {"kind":"app","app":"com.example","col":5,"row":0,"colSpan":5,"rowSpan":6},
            {"kind":"nonsense","col":0,"row":0}]}]}"""
        val ws = WorkspaceJson.decode(brokenItems)!!.workspaces.single()
        // The duplicate map is dropped, the app is moved off the map to free cells, the unknown kind is dropped.
        assertEquals(2, ws.items.size)
        assertEquals(setOf("map", "com.example"), ws.items.map { it.widgetId ?: it.appId }.toSet())
        assertFalse(ws.items[0].rect.overlaps(ws.items[1].rect))
    }

    @Test
    fun decodeOfGarbageIsNull() {
        assertNull(WorkspaceJson.decode(null))
        assertNull(WorkspaceJson.decode(""))
        assertNull(WorkspaceJson.decode("not json"))
        assertNull(WorkspaceJson.decode("""{"workspaces":[]}"""))
    }

    @Test
    fun closeAppsWhenHiddenRoundTrips() {
        val ws = Workspace.create("Leaving").copy(closeAppsWhenHidden = true)
        val decoded = WorkspaceJson.decode(WorkspaceJson.encode(WorkspaceConfig(listOf(ws))))!!
        assertTrue(decoded.workspaces.single().closeAppsWhenHidden)
    }

    @Test
    fun oldDashboardBecomesTheFirstWorkspace() {
        val migrated = migrateDashboard(listOf("map", "media", "calendar", "notification"))
        assertEquals(1, migrated.workspaces.size)
        val ws = migrated.workspaces.single()
        assertEquals(WorkspaceLayout.FOUR, ws.layout)
        assertEquals(listOf("map", "media", "calendar", "notification"), ws.items.map { it.widgetId })
        assertEquals(migrated.workspaces.single().id, migrated.defaultId)
        assertEquals(migrated.workspaces.single().id, migrated.lastId)
        ws.items.forEach { assertTrue(it.rect.isInsideGrid()) }
    }

    @Test
    fun migratingAllSevenOldCardsKeepsEveryOne() {
        val cards = listOf("map", "media", "calendar", "notification", "weather", "home", "car")
        val migrated = migrateDashboard(cards)
        val ws = migrated.workspaces.single()
        assertEquals(WorkspaceLayout.CUSTOM, ws.layout)
        assertEquals(cards, ws.items.map { it.widgetId })
        assertEquals(cards.size, ws.items.size)
        ws.items.forEach { assertTrue("$it inside", it.rect.isInsideGrid()) }
        ws.items.forEachIndexed { i, a ->
            ws.items.drop(i + 1).forEach { b -> assertFalse("$a overlaps $b", a.rect.overlaps(b.rect)) }
        }
    }

    @Test
    fun defaultConfigHasAUsableMapAndMediaSplit() {
        val config = WorkspaceConfig.default()
        val ws = config.workspaces.single()
        assertEquals(WorkspaceLayout.SPLIT_70_30, ws.layout)
        assertEquals(listOf("map", "media"), ws.items.map { it.widgetId })
        assertEquals(7, ws.items.first().rect.colSpan)
        assertNotNull(config.startWorkspace())
    }

    @Test
    fun duplicateWorkspaceNeedsFreshIdsForTheCopy() {
        val original = Workspace.create("City", WorkspaceLayout.SPLIT_50)
            .withAddedItem(WorkspaceItem.widget("map", WorkspaceRect(0, 0, 1, 1)))!!
        val copy = original.copy(id = "copy", name = "City 2", items = original.items.map { it.copy(id = it.id + "-copy") })
        assertEquals(original.items.size, copy.items.size)
        assertNotEquals(original.id, copy.id)
        assertTrue(original.items.map { it.id }.intersect(copy.items.map { it.id }.toSet()).isEmpty())
    }

    @Test
    fun gridHelpersPlaceAroundOccupiedCells() {
        val occupied = listOf(WorkspaceRect(0, 0, 10, 3))
        assertEquals(WorkspaceRect(0, 3, 5, 3), firstFreeRect(occupied, 5, 3))
        assertNull(firstFreeRect(listOf(WorkspaceRect.full()), 1, 1))
    }
}
