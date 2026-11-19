package com.example.cathousemerge

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
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
class IterationUiTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun content() = ContentParser.parse { name -> context.assets.open("content/$name").bufferedReader().use { it.readText() } }
    private fun capture(name: String) {
        compose.waitForIdle()
        // Capture the component surface, not the test activity's overlaid system bars.
        val node = if (name.contains("welcome")) compose.onNodeWithTag("welcome", useUnmergedTree = true) else compose.onRoot()
        val screenshot = node.captureToImage().asAndroidBitmap()
        val output = File(context.getExternalFilesDir("iteration-screenshots"), "$name.png")
        output.parentFile!!.mkdirs()
        output.outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        screenshot.recycle()
    }

    @Test fun coverShowsTheNewTitleNoteAndVersionAndStartsTheGame() {
        var started = false
        compose.setContent { CatTheme { WelcomeScreen(GameState(), true) { started = true } } }
        compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription("一只猫独自守着旧屋").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("welcome_title").assertExists()
        compose.onNodeWithTag("welcome_note").assertTextContains("一个旧屋，六只猫。", substring = true)
        compose.onNodeWithText("第 1 天 · v${BuildConfig.VERSION_NAME}").assertExists()
        capture("v${BuildConfig.VERSION_NAME}-welcome")
        compose.onNodeWithTag("start_game").performClick()
        compose.runOnIdle { assertTrue(started) }
    }

    @Test fun compactCoverWithLargerFontKeepsTheNoteAboveTheAction() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.3f)) {
                CatTheme { Box(Modifier.width(320.dp).height(568.dp)) { WelcomeScreen(GameState(), true) {} } }
            }
        }
        compose.onNodeWithTag("start_game").assertIsDisplayed()
        val note = compose.onNodeWithTag("welcome_note").fetchSemanticsNode().boundsInRoot
        val button = compose.onNodeWithTag("start_game").fetchSemanticsNode().boundsInRoot
        assertTrue("The note must never overlap the start button", note.bottom <= button.top)
        capture("v${BuildConfig.VERSION_NAME}-welcome-compact")
    }

    @Test fun orderAndBoardFeedbackChangeTogetherAndClearAfterDelivery() {
        val original = content()
        val order = OrderDef("feedback", 1, "normal", listOf(Requirement("wood", 1, 2)), 10, 10)
        val content = original.copy(orders = listOf(order), days = listOf(original.day(1).copy(orderIds = listOf(order.id))))
        val engine = GameEngine(content)
        val board = MutableList<Piece?>(BOARD_SIZE) { null }
        board[0] = Piece(1, "wood", 1, PieceKind.GENERATOR)
        board[1] = Piece(2, "wood", 1, PieceKind.ITEM)
        var state by mutableStateOf(GameState(board = board, energyAt = System.currentTimeMillis(), nextUid = 3, tutorialDone = true))
        compose.setContent { CatTheme { BoardScreen(content, state, GameSettings(reducedMotion = true), { state = engine.reduce(state, it).state }, {}, {}) } }
        compose.onNodeWithTag("requirement_feedback_0", true).assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "未满足 1/2"))
        compose.onNodeWithTag("cell_1").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "需求仍欠缺，订单 feedback，1/2"))
        capture("v${BuildConfig.VERSION_NAME}-board-demand-missing")
        compose.onNodeWithTag("cell_0").performClick()
        compose.onNodeWithTag("requirement_feedback_0", true).assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "已满足 2/2"))
        compose.onNodeWithTag("order_feedback").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "可交付"))
        compose.onNodeWithTag("cell_1").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "需求已满足，订单 feedback，2/2"))
        capture("v${BuildConfig.VERSION_NAME}-board-ready")
        compose.onNodeWithTag("deliver_feedback").performClick()
        compose.runOnIdle { assertTrue("feedback" in state.delivered); assertNull(state.board[1]) }
        compose.onNodeWithTag("orders_completed").assertExists()
    }

    @Test fun lowEnergyShowsOneSlashedBadgeAndNoActiveLightningPrompt() {
        val content = content()
        val board = MutableList<Piece?>(BOARD_SIZE) { null }
        board[0] = Piece(1, "wood", 1, PieceKind.GENERATOR)
        board[8] = Piece(2, "wood", 1, PieceKind.GENERATOR)
        val state = GameState(board = board, energy = 0, energyAt = System.currentTimeMillis(), nextUid = 3, tutorialDone = true)
        compose.setContent { CatTheme { BoardScreen(content, state, GameSettings(), {}, {}, {}) } }
        compose.waitUntil(6_000) { compose.onAllNodesWithTag("generator_no_energy", true).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithTag("generator_no_energy", true).assertCountEquals(1)
        compose.onAllNodesWithTag("generator_energy_badge", true).assertCountEquals(0)
        capture("v${BuildConfig.VERSION_NAME}-generator-no-energy")
    }

    @Test fun dialogueButtonRevealsThenAdvancesAndLastPageFinishes() {
        val first = "先听完这一句话，再继续修好猫屋。"
        val last = "这就是最后一句。"
        var finished = false
        compose.setContent { CatTheme { DialogueOverlay("button-test", DialogueScene(1, "intro", listOf(DialogueLine("daju", first), DialogueLine("daju", last))), "porch", false, false) { finished = true } } }
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("dialogue_advance", true).performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("dialogue_text", true).assertTextEquals(first)
        compose.onNodeWithTag("dialogue_advance", true).performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("dialogue_advance", true).performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("dialogue_text", true).assertTextEquals(last)
        compose.onNodeWithContentDescription("结束对话", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("dialogue_advance", true).performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.runOnIdle { assertTrue(finished) }
    }

    @Test fun narrowLargeFontDialoguePaginatesWithoutDroppingAnyCharacters() {
        val text = "猫屋的每一个角落，都有慢慢修好的理由。".repeat(6)
        var finished = false
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f)) {
                CatTheme { Box(Modifier.width(320.dp).height(640.dp)) {
                    DialogueOverlay("large-font", DialogueScene(1, "intro", listOf(DialogueLine("daju", text))), "porch", false, true) { finished = true }
                } }
            }
        }
        val read = StringBuilder()
        for (i in 0 until 40) {
            if (finished) break
            val shown = compose.onNodeWithTag("dialogue_text", true).fetchSemanticsNode().config[SemanticsProperties.Text].single().text
            assertTrue(shown.isNotEmpty())
            read.append(shown)
            compose.onNodeWithTag("dialogue_advance", true).performClick()
        }
        compose.runOnIdle { assertTrue(finished); assertEquals(text, read.toString()) }
    }

    @Test fun dialogueUsesTheCurrentRoomAndCatPortrait() {
        compose.setContent { CatTheme { DialogueOverlay("visual", DialogueScene(1, "intro", listOf(DialogueLine("daju", "回来就好。先把门廊修好，雨天也能坐着喝茶。"))), "porch", false, true) {} } }
        compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription("当前房间", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("dialogue_avatar", true).assertExists()
        compose.onNodeWithTag("dialogue_name", true).assertExists()
        capture("v${BuildConfig.VERSION_NAME}-dialogue")
    }
}
