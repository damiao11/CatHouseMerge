package com.example.cathousemerge

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.cathousemerge.game.GameAction
import com.example.cathousemerge.game.GameSettings
import com.example.cathousemerge.game.PieceKind
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameFlowTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun model(): GameViewModel = ViewModelProvider(compose.activity)[GameViewModel::class.java]

    private fun skipDialogue() {
        compose.waitForIdle()
        repeat(5) {
            if (compose.onAllNodesWithTag("dialogue_skip").fetchSemanticsNodes().isNotEmpty()) {
                compose.onNodeWithTag("dialogue_skip").performClick()
                compose.waitForIdle()
            }
        }
    }

    private fun dragCell(from: Int, to: Int) {
        val start = compose.onNodeWithTag("cell_$from").fetchSemanticsNode().boundsInRoot.center
        val end = compose.onNodeWithTag("cell_$to").fetchSemanticsNode().boundsInRoot.center
        val origin = compose.onNodeWithTag("board").fetchSemanticsNode().boundsInRoot.topLeft
        compose.onNodeWithTag("board").performTouchInput {
            swipe(start - origin, end - origin, durationMillis = 400)
        }
        compose.waitForIdle()
    }

    @Test
    fun normalGesturesDayProgressionAndAtomicSaveRestore() {
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("start_game").fetchSemanticsNodes().isNotEmpty() }
        compose.runOnIdle {
            model().reset()
            model().changeSettings(GameSettings(soundVolume = 0f, haptics = false, reducedMotion = true))
        }
        compose.onNodeWithTag("start_game").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("board", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        skipDialogue()

        // Exercise actual pointer input for generation, moving, and merging.
        compose.onNodeWithTag("bubble_1", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("cell_0").performClick()
        dragCell(1, 2)
        compose.onNodeWithTag("cell_0").performClick()
        dragCell(1, 2)
        compose.runOnIdle {
            val state = model().ui.value.game!!
            assertEquals(PieceKind.ITEM, state.board[2]?.kind)
            assertEquals(2, state.board[2]?.level)
            assertEquals(148, state.energy)
        }

        // Supply the remaining materials through the debug reducer, then use the normal order cards.
        for (id in listOf("o1_1", "o1_2", "o1_3")) {
            compose.runOnIdle { model().dispatch(GameAction.DebugFillOrder(id)) }
            compose.onNodeWithTag("orders").performScrollToNode(hasTestTag("order_$id"))
            compose.onNodeWithTag("order_$id").performClick()
            skipDialogue()
        }
        compose.runOnIdle { assertEquals(180, model().ui.value.game!!.coins) }
        compose.onNodeWithTag("build_open").performClick()
        compose.onNodeWithTag("build_pay").performClick()
        compose.runOnIdle { assertTrue(1 in model().ui.value.game!!.builtDays) }
        skipDialogue()
        compose.onNodeWithTag("build_pay").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("board").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("summary_open").performClick()
        compose.onNodeWithTag("next_day").performClick()
        skipDialogue()
        compose.runOnIdle {
            assertEquals(2, model().ui.value.game!!.day)
            assertEquals(50, model().ui.value.game!!.coins)
            model().flushSave()
        }

        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.runOnIdle {
            val state = model().ui.value.game!!
            assertEquals(2, state.day)
            assertEquals(50, state.coins)
            assertEquals(3, state.delivered.size)
            assertTrue(1 in state.builtDays)
        }

        // Read from disk through a separate repository, independently of retained ViewModel state.
        compose.runOnIdle {
            val ui = model().ui.value
            val restored = com.example.cathousemerge.data.SaveRepository(compose.activity, ui.content!!).load().state!!
            assertEquals(ui.game, restored)
        }
    }

    @Test
    fun storyChoiceAndWarehouseUseTheirNormalControls() {
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("start_game").fetchSemanticsNodes().isNotEmpty() }
        compose.runOnIdle {
            model().reset()
            model().changeSettings(GameSettings(soundVolume = 0f, haptics = false, reducedMotion = true))
        }
        compose.onNodeWithTag("start_game").performClick()
        skipDialogue()
        compose.runOnIdle { model().dispatch(GameAction.DebugJump(14)) }
        skipDialogue()
        compose.runOnIdle { model().dispatch(GameAction.Build) }
        compose.onNodeWithTag("choice_d14_choice_green").performClick()
        skipDialogue()
        compose.runOnIdle {
            assertEquals("d14_choice_green", model().ui.value.game!!.choices[14])
            assertTrue("d14_choice_warm" in model().ui.value.game!!.seenDialogue)
        }
        compose.onNodeWithTag("cell_0").performClick()
        compose.onNodeWithText("收进仓库").performClick()
        compose.runOnIdle { assertEquals(1, model().ui.value.game!!.warehouse.size) }
        compose.onNodeWithTag("warehouse_open").performClick()
        compose.onNodeWithTag("warehouse_slot_0").performClick()
        compose.onNodeWithTag("warehouse_expand").performClick()
        compose.runOnIdle {
            val state = model().ui.value.game!!
            assertTrue(state.warehouse.isEmpty())
            assertEquals(PieceKind.GENERATOR, state.board[0]?.kind)
            assertEquals(7, state.warehouseCapacity)
            model().flushSave()
            val restored = com.example.cathousemerge.data.SaveRepository(compose.activity, model().ui.value.content!!).load().state!!
            assertEquals(state, restored)
        }
    }
}
