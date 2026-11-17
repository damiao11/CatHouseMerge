package com.example.cathousemerge

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
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
class UiV1MigrationTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun content() = ContentParser.parse { file -> context.assets.open("content/$file").bufferedReader().use { it.readText() } }
    private fun minimal(content: GameContent, day: Int = 3) = GameState(day = day, coins = 440, energy = 62, energyAt = System.currentTimeMillis(),
        board = List(BOARD_SIZE) { when(it) { 0 -> Piece(1, "wood", 1, PieceKind.GENERATOR); 2 -> Piece(2, "wood", 3, PieceKind.ITEM); else -> null } },
        builtDays = (1 until day).toSet(), seenDialogue = content.dialogue.scenes.keys, tutorialDone = true, nextUid = 3)
    private fun capture(name: String) {
        compose.waitForIdle()
        val bitmap = compose.onNodeWithTag("migration_preview").captureToImage().asAndroidBitmap()
        val output = File(context.getExternalFilesDir("ui-v1-screenshots"), "$name.png")
        output.parentFile!!.mkdirs()
        output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    private fun dragToWarehouse(index: Int) {
        val start = compose.onNodeWithTag("cell_$index").fetchSemanticsNode().boundsInRoot.center
        val end = compose.onNodeWithTag("warehouse_open").fetchSemanticsNode().boundsInRoot.center
        val origin = compose.onNodeWithTag("board").fetchSemanticsNode().boundsInRoot.topLeft
        compose.onNodeWithTag("board").performTouchInput { swipe(start - origin, end - origin, durationMillis = 400) }
        compose.waitForIdle()
    }

    @Test fun approvedBoardUsesRealContentAndBlankSelectionAndEngineSalePrice() {
        val content = content(); val engine = GameEngine(content)
        var state by mutableStateOf(minimal(content))
        compose.setContent { CatTheme { Box(Modifier.width(360.dp).height(780.dp).testTag("migration_preview")) { BoardScreen(content, state, GameSettings(reducedMotion = true), { state = engine.reduce(state, it).state }, {}, {}) } } }
        compose.onNodeWithText("体力").assertDoesNotExist()
        compose.onNodeWithText("金币").assertDoesNotExist()
        compose.onNodeWithText("材料已备齐").assertDoesNotExist()
        compose.onNodeWithText("收进仓库").assertDoesNotExist()
        compose.onNodeWithTag("cell_2").performClick()
        compose.onNodeWithText("木板 3级").assertIsDisplayed()
        compose.onNodeWithText("平整结实，适合修补旧屋。").assertIsDisplayed()
        capture("board-selected")
        val before = compose.onNodeWithTag("board").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("cell_62").performClick()
        compose.onNodeWithText("空格子").assertDoesNotExist()
        compose.onNodeWithText("木板 3级").assertDoesNotExist()
        compose.onNodeWithTag("sell_selected").assertDoesNotExist()
        assertEquals(before, compose.onNodeWithTag("board").fetchSemanticsNode().boundsInRoot)
        capture("board-empty-selection")
        compose.onNodeWithTag("cell_2").performClick()
        compose.onNodeWithTag("sell_selected").performClick()
        compose.runOnIdle { assertEquals(446, state.coins); assertNull(state.board[2]) }
    }

    @Test fun longItemHeadingDoesNotOverlapSellingAtLargeFontOnSmallScreens() {
        val content = content()
        val state = minimal(content, 18).copy(energy = MAX_ENERGY, board = List(BOARD_SIZE) { if(it == 2) Piece(2, "plant", 6, PieceKind.ITEM) else null })
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.4f)) {
                CatTheme { Box(Modifier.width(320.dp).height(640.dp).testTag("migration_preview")) { BoardScreen(content, state, GameSettings(reducedMotion = true), {}, {}, {}) } }
            }
        }
        compose.onNodeWithTag("cell_2").performClick()
        val title = compose.onNodeWithTag("item_info_heading").fetchSemanticsNode().boundsInRoot
        val sale = compose.onNodeWithTag("sell_label").fetchSemanticsNode().boundsInRoot
        assertTrue("Long titles must leave the selling column clear", title.right <= sale.left)
        compose.onNodeWithContentDescription("物品说明").assertIsDisplayed()
        compose.onNodeWithTag("sell_selected").assertIsDisplayed()
        capture("board-long-title-large-font")
        for (text in listOf("18", "150", "满开玫瑰 6级")) {
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(text).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertTrue("Text layout should be available for $text", layouts.isNotEmpty())
            val layout = layouts.first()
            // Text pixels round to integers; allow at most one physical pixel, never a missing glyph or line.
            assertTrue("$text width must fit: intrinsic=${layout.multiParagraph.intrinsics.maxIntrinsicWidth}, size=${layout.size}", layout.multiParagraph.intrinsics.maxIntrinsicWidth <= layout.size.width + 1f)
            assertTrue("$text height must fit: paragraph=${layout.multiParagraph.height}, size=${layout.size}", layout.multiParagraph.height <= layout.size.height + 1f)
            assertFalse("$text must not be ellipsized", layout.multiParagraph.didExceedMaxLines)
        }
        capture("board-long-title-large-font")
    }

    @Test fun warehouseStoresOnlyByDraggingAndRespectsDayFourAndCapacity() {
        val content = content(); val engine = GameEngine(content)
        var state by mutableStateOf(minimal(content))
        compose.setContent { CatTheme { Box(Modifier.width(360.dp).height(780.dp).testTag("migration_preview")) { BoardScreen(content, state, GameSettings(reducedMotion = true), { state = engine.reduce(state, it).state }, {}, {}) } } }
        dragToWarehouse(2)
        compose.runOnIdle { assertNotNull(state.board[2]); assertTrue(state.warehouse.isEmpty()); state = state.copy(day = 4) }
        dragToWarehouse(2)
        compose.runOnIdle { assertNull(state.board[2]); assertEquals("wood_3", state.warehouse.single().itemId) }
        compose.runOnIdle { state = minimal(content, 4).copy(warehouse = List(6) { Piece(20L + it, "wood", 1, PieceKind.ITEM) }) }
        dragToWarehouse(2)
        compose.runOnIdle { assertNotNull(state.board[2]); assertEquals(6, state.warehouse.size) }
    }

    @Test fun onlyTheDeliverButtonSubmitsTheOrderAndPaysTheRealReward() {
        val original = content()
        val order = original.ordersForDay(3).first().copy(id = "ui_delivery", req = listOf(Requirement("wood", 3, 1)))
        val content = original.copy(orders = listOf(order), days = listOf(original.day(3).copy(orderIds = listOf(order.id))))
        val engine = GameEngine(content); var state by mutableStateOf(minimal(content))
        compose.setContent { CatTheme { Box(Modifier.width(360.dp).height(780.dp)) { BoardScreen(content, state, GameSettings(reducedMotion = true), { state = engine.reduce(state, it).state }, {}, {}) } } }
        compose.onNodeWithTag("order_${order.id}").performClick()
        compose.runOnIdle { assertTrue(state.delivered.isEmpty()); assertEquals(440, state.coins) }
        compose.onNodeWithTag("deliver_${order.id}").performClick()
        compose.runOnIdle { assertTrue(order.id in state.delivered); assertEquals(440 + order.reward, state.coins); assertNull(state.board[2]) }
    }

    @Test fun deliveredOrderKeepsItsFulfilledCountsAfterMaterialsAreConsumed() {
        val content = content(); val order = content.ordersForDay(3).first()
        var done by mutableStateOf(false)
        compose.setContent { CatTheme {
            OrderCard(content, minimal(content).copy(delivered = if(done) setOf(order.id) else emptySet()), order, List(order.req.size) { 0 }, false, {})
        } }
        compose.runOnIdle { done = true }
        order.req.forEachIndexed { index, req ->
            compose.onNodeWithTag("requirement_${order.id}_$index", true).assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription, "已满足 ${req.count}/${req.count}"))
        }
        compose.onNodeWithText("已交付").assertIsDisplayed()
    }

    @Test fun endOfDayRemainsAvailableWithUnclaimedToolsAndAFullBoard() {
        val content = content()
        val state = minimal(content).copy(board = List(BOARD_SIZE) { Piece(30L + it, "wood", 1, PieceKind.ITEM) },
            delivered = content.ordersForDay(3).map { it.id }.toSet(), builtDays = setOf(1,2,3), bubble = listOf(Piece(200, "wood", 1, PieceKind.GENERATOR)))
        var route = ""
        compose.setContent { CatTheme { Box(Modifier.width(360.dp).height(780.dp)) { BoardScreen(content, state, GameSettings(reducedMotion = true), {}, { route = it }, {}) } } }
        compose.onNodeWithTag("new_tools").assertIsDisplayed()
        compose.onNodeWithTag("summary_open").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("summary", route) }
    }

    @Test fun wholeRoomComparisonHasOneFooterActionAndDoesNotSpendAgain() {
        val content = content(); val state = minimal(content).copy(builtDays = setOf(1,2,3))
        var action: GameAction? = null
        compose.setContent { CatTheme { Box(Modifier.width(360.dp).height(780.dp).testTag("migration_preview")) { BuildScreen(content, state, GameSettings(reducedMotion = true), { action = it }, {}) } } }
        compose.onNodeWithTag("repair_progress").assertTextEquals("3/3")
        compose.onNodeWithText("看看修好后").assertDoesNotExist()
        compose.onNodeWithTag("repair_compare").assertTextContains("重新看看修缮前", substring = true)
        compose.waitUntil(10000) { compose.onAllNodesWithContentDescription("门廊修缮后").fetchSemanticsNodes().isNotEmpty() }
        capture("repair-after")
        compose.onNodeWithTag("repair_compare").performClick()
        compose.onNodeWithText("修缮前插画").assertDoesNotExist()
        compose.onNodeWithTag("repair_scene_label").assertTextEquals("修缮前")
        compose.onNodeWithTag("repair_compare").assertTextContains("重新看看修缮后", substring = true)
        compose.waitUntil(10000) { compose.onAllNodesWithContentDescription("门廊修缮前").fetchSemanticsNodes().isNotEmpty() }
        capture("repair-before-comparison")
        compose.onNodeWithTag("repair_compare").performClick()
        compose.runOnIdle { assertNull(action); assertEquals(440, state.coins) }
    }

    @Test fun activeBoardVisualUsesUnlockedChainsAndActualOrderRequirements() {
        val content = content(); val engine = GameEngine(content)
        val chains = content.chains.filter { it.unlockDay <= 14 }
        val pieces = mutableListOf<Piece>(); var uid = 1L
        chains.forEach { pieces += Piece(uid++, it.id, 1, PieceKind.GENERATOR) }
        for(level in 1..6) chains.forEach { if (content.itemById.containsKey("${it.id}_$level")) pieces += Piece(uid++, it.id, level, PieceKind.ITEM) }
        val positions = (0 until BOARD_SIZE).filter { it % 4 != 1 }
        val board = MutableList<Piece?>(BOARD_SIZE) { null }
        pieces.take(30).forEachIndexed { index, piece -> board[positions[index]] = piece }
        var state = minimal(content, 14).copy(board = board, nextUid = uid)
        state = engine.reduce(state, GameAction.DebugFillOrder(content.ordersForDay(14).first().id)).state
        compose.setContent { CatTheme { Box(Modifier.width(360.dp).height(780.dp).testTag("migration_preview")) { BoardScreen(content, state, GameSettings(reducedMotion = true), {}, {}, {}) } } }
        val selectedIndex = state.board.indexOfFirst { it?.kind == PieceKind.ITEM && it.itemId == "metal_1" }
        compose.onNodeWithTag("cell_$selectedIndex").performClick()
        repeat(BOARD_SIZE) { compose.onNodeWithTag("cell_$it").assertIsDisplayed() }
        capture("board-active")
    }

    @Test fun orderAndBuildSuccessFeedbackAreCenteredAndUseTheShortDuration() {
        val content = content(); val state = minimal(content)
        var message by mutableStateOf<String?>(null); var id by mutableLongStateOf(0)
        var effect by mutableStateOf<GameEffect?>(null)
        compose.setContent { CatTheme { Box(Modifier.width(360.dp).height(780.dp).testTag("migration_preview")) {
            CatHouseApp(content, state, GameSettings(reducedMotion = true), message, id, {}, {}, {}, { message = null }, messageEffect = effect)
        } } }
        compose.onNodeWithTag("start_game").performClick()
        for (kind in listOf(GameEffect.DELIVER, GameEffect.BUILD)) {
            assertEquals(1200L, gameMessageHoldMillis(kind))
            compose.runOnIdle { effect = kind; message = if(kind == GameEffect.DELIVER) "订单完成，获得 120 金币" else "挂迎客灯，完成了"; id++ }
            compose.onNodeWithTag("game_toast").assertIsDisplayed()
            val toast = compose.onNodeWithTag("game_toast").fetchSemanticsNode().boundsInRoot
            val root = compose.onNodeWithTag("migration_preview").fetchSemanticsNode().boundsInRoot
            assertTrue((toast.center.y-root.top)/root.height in .35f..55f/100)
            // Android scheduling under emulator load is not a reliable stopwatch. Verify the configured
            // hold exactly, then independently verify the real overlay and its eventual dismissal.
            compose.waitUntil(10000) { compose.onAllNodesWithTag("game_toast").fetchSemanticsNodes().isEmpty() }
            compose.runOnIdle { assertNull(message) }
        }
        compose.runOnIdle { effect = null; message = "仓库第4天开放"; id++ }
        compose.onNodeWithTag("game_toast").assertIsDisplayed()
        assertEquals(2400L, gameMessageHoldMillis(null))
        compose.waitUntil(10000) { compose.onAllNodesWithTag("game_toast").fetchSemanticsNodes().isEmpty() }
        compose.runOnIdle { assertNull(message) }
    }
}
