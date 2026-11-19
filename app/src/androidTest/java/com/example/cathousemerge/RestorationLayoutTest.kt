package com.example.cathousemerge

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cathousemerge.game.*
import com.example.cathousemerge.ui.*
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RestorationLayoutTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun content() = ContentParser.parse { path -> context.assets.open("content/$path").bufferedReader().use { it.readText() } }
    private fun previewState(content: GameContent, coins: Int = 440): GameState {
        val board = MutableList<Piece?>(BOARD_SIZE) { null }
        board[0] = Piece(1, "wood", 1, PieceKind.GENERATOR)
        board[2] = Piece(2, "wood", 3, PieceKind.ITEM)
        board[5] = Piece(3, "wood", 5, PieceKind.ITEM)
        board[6] = Piece(4, "wood", 1, PieceKind.GENERATOR)
        board[10] = Piece(5, "wood", 2, PieceKind.ITEM)
        return GameState(day = 3, coins = coins, energy = 150, energyAt = System.currentTimeMillis(), board = board,
            delivered = content.ordersForDay(3).map { it.id }.toSet(), builtDays = setOf(1, 2), tutorialDone = true, nextUid = 6)
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val file = File(context.getExternalFilesDir("restoration-screenshots"), "$name.png")
        file.parentFile!!.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun boardGeometryStaysFixedAcrossSelectionDeliveryAndOrderCollapse() {
        assertBoardGeometryStable(fontScale = 1f)
    }

    @Test fun largeFontBoardGeometryStaysFixedAcrossSelectionDeliveryAndOrderCollapse() {
        assertBoardGeometryStable(fontScale = 1.3f)
    }

    private fun assertBoardGeometryStable(fontScale: Float) {
        val original = content()
        val order = OrderDef("stable", 1, "normal", listOf(Requirement("wood", 1, 1)), 10, 10)
        val content = original.copy(orders = listOf(order), days = listOf(original.day(1).copy(orderIds = listOf(order.id))))
        val engine = GameEngine(content)
        val board = MutableList<Piece?>(BOARD_SIZE) { null }
        board[0] = Piece(1, "wood", 1, PieceKind.GENERATOR)
        board[1] = Piece(2, "wood", 1, PieceKind.ITEM)
        var state by mutableStateOf(GameState(board = board, energyAt = System.currentTimeMillis(), nextUid = 3, tutorialDone = true))
        var route = ""
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                CatTheme { Box(Modifier.width(320.dp).height(640.dp)) {
                    BoardScreen(content, state, GameSettings(reducedMotion = true), { state = engine.reduce(state, it).state }, { route = it }, {})
                } }
            }
        }
        val tags = listOf("board", "workshop_tray", "cell_0", "cell_62", "board_footer", "warehouse_open")
        val initial = tags.associateWith { compose.onNodeWithTag(it).fetchSemanticsNode().boundsInRoot }
        fun assertUnchanged(phase: String) {
            tags.forEach { tag ->
                val before = initial.getValue(tag)
                val after = compose.onNodeWithTag(tag).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
                assertEquals("$phase: $tag left", before.left, after.left, 0.5f)
                assertEquals("$phase: $tag top", before.top, after.top, 0.5f)
                assertEquals("$phase: $tag width", before.width, after.width, 0.5f)
                assertEquals("$phase: $tag height", before.height, after.height, 0.5f)
            }
        }
        compose.onNodeWithTag("cell_1").performClick()
        assertUnchanged("item selected")
        compose.onNodeWithTag("deliver_stable").performClick()
        compose.runOnIdle { assertNull(state.board[1]); assertTrue("stable" in state.delivered) }
        compose.onNodeWithTag("orders_completed").assertIsDisplayed()
        assertUnchanged("selected item delivered and orders collapsed")
        compose.onNodeWithTag("cell_0").performClick()
        assertUnchanged("generator selected and material spawned")
        compose.onNodeWithTag("cell_1").performClick()
        assertUnchanged("spawned item selected")
        compose.onNodeWithTag("cell_1").performClick()
        assertUnchanged("selection cleared")
        compose.runOnIdle { state = state.copy(bubble = listOf(Piece(100, "wood", 1, PieceKind.GENERATOR))) }
        compose.onNodeWithTag("bubble_100", true).performClick()
        compose.onNodeWithTag("orders_completed").assertIsDisplayed()
        assertUnchanged("new tool claimed and orders collapsed again")
        compose.runOnIdle { state = state.copy(builtDays = setOf(1)) }
        compose.onNodeWithTag("summary_open").assertIsDisplayed()
        assertUnchanged("end-of-day action shown")
        compose.onNodeWithTag("summary_open").performClick()
        compose.runOnIdle { assertEquals("summary", route) }
    }

    @Test fun completedOrdersCollapseAndAll63CellsAndNavigationStayUsable() {
        val content = content()
        var route = ""
        compose.setContent { CatTheme { BoardScreen(content, previewState(content), GameSettings(reducedMotion = true), {}, { route = it }, {}) } }
        compose.onNodeWithTag("orders_completed").assertIsDisplayed()
        compose.onNodeWithText("今日订单已完成").assertIsDisplayed()
        content.ordersForDay(3).forEach { compose.onNodeWithTag("order_${it.id}").assertDoesNotExist() }
        repeat(BOARD_SIZE) { compose.onNodeWithTag("cell_$it").assertIsDisplayed() }
        val first = compose.onNodeWithTag("cell_0").fetchSemanticsNode().boundsInRoot
        val last = compose.onNodeWithTag("cell_62").fetchSemanticsNode().boundsInRoot
        val footer = compose.onNodeWithTag("build_open").fetchSemanticsNode().boundsInRoot
        assertEquals(first.width, first.height, 1f)
        assertTrue(last.bottom <= footer.top)
        capture("board-completed")
        compose.onNodeWithTag("build_open").performClick()
        compose.runOnIdle { assertEquals("build", route) }
        compose.onNodeWithTag("warehouse_open").performClick()
        compose.runOnIdle { assertEquals("warehouse", route) }
    }

    @Test fun activeOrdersAndUnclaimedToolsKeepTheirRealActions() {
        val content = content()
        val state = previewState(content).copy(delivered = emptySet(), bubble = listOf(Piece(6, "wood", 1, PieceKind.GENERATOR)))
        var action: GameAction? = null
        compose.setContent { CatTheme { BoardScreen(content, state, GameSettings(reducedMotion = true), { action = it }, {}, {}) } }
        compose.onNodeWithTag("orders_completed").assertDoesNotExist()
        compose.onNodeWithTag("orders").assertIsDisplayed()
        compose.onNodeWithTag("new_tools").assertIsDisplayed()
        compose.onNodeWithTag("bubble_6", true).performClick()
        compose.runOnIdle { assertEquals(GameAction.TakeBubble(6), action) }
        capture("board-active-orders")
    }

    @Test fun narrowLargeFontBoardKeepsEveryCellAndBottomActionsVisible() {
        val content = content()
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.3f)) {
                CatTheme { Box(Modifier.width(320.dp).height(640.dp)) {
                    BoardScreen(content, previewState(content), GameSettings(reducedMotion = true), {}, {}, {})
                } }
            }
        }
        compose.onNodeWithTag("cell_0").assertIsDisplayed()
        compose.onNodeWithTag("cell_62").assertIsDisplayed()
        compose.onNodeWithTag("settings_open").assertIsDisplayed()
        compose.onNodeWithTag("build_open").assertIsDisplayed()
        compose.onNodeWithTag("warehouse_open").assertIsDisplayed()
        capture("board-compact-large-font")
    }

    @Test fun repairUsesRealProgressAndOneCostAndDispatchesBuild() {
        val content = content()
        var action: GameAction? = null
        compose.setContent { CatTheme { BuildScreen(content, previewState(content), GameSettings(reducedMotion = true), { action = it }, {}) } }
        compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("门廊修缮前").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("repair_progress").assertTextEquals("2/3")
        compose.onNodeWithTag("repair_current_task").assertIsDisplayed()
        compose.onNodeWithTag("repair_target").assertIsDisplayed()
        compose.onAllNodesWithText("220", substring = true).assertCountEquals(1)
        compose.onNodeWithTag("build_back").assertIsDisplayed()
        capture("repair-affordable")
        compose.onNodeWithTag("build_pay").performClick()
        compose.runOnIdle { assertEquals(GameAction.Build, action) }
    }

    @Test fun insufficientCoinsReturnToMergeWithoutDispatchingBuild() {
        val content = content()
        var action: GameAction? = null
        var route = ""
        compose.setContent { CatTheme { BuildScreen(content, previewState(content, coins = 20), GameSettings(reducedMotion = true), { action = it }, { route = it }) } }
        compose.onNodeWithText("还差 200 金币，完成订单后再来。").assertIsDisplayed()
        capture("repair-insufficient-coins")
        compose.onNodeWithTag("build_pay").performClick()
        compose.runOnIdle { assertNull(action); assertEquals("board", route) }
    }

    @Test fun compactLargeFontRepairKeepsThePrimaryActionPinned() {
        val content = content()
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.4f)) {
                CatTheme { Box(Modifier.width(320.dp).height(568.dp)) {
                    BuildScreen(content, previewState(content), GameSettings(reducedMotion = true), {}, {})
                } }
            }
        }
        compose.onNodeWithTag("build_pay").assertIsDisplayed()
        compose.onNodeWithTag("repair_current_task").performScrollTo().assertIsDisplayed()
        capture("repair-compact-large-font")
    }

    @Test fun completedRoomShowsFullProgressAndReturnAction() {
        val content = content()
        var route = ""
        val state = previewState(content).copy(builtDays = setOf(1, 2, 3))
        compose.setContent { CatTheme { BuildScreen(content, state, GameSettings(reducedMotion = true), {}, { route = it }) } }
        compose.onNodeWithText("这间修好了，下一段故事等你开启。").assertIsDisplayed()
        compose.onNodeWithTag("repair_progress").assertTextEquals("3/3")
        compose.onNodeWithTag("repair_current_task").assertDoesNotExist()
        capture("repair-completed")
        compose.onNodeWithTag("build_pay").performClick()
        compose.runOnIdle { assertEquals("board", route) }
    }
}
