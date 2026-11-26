package com.example.cathousemerge

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
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
class OrderAndCoverRegressionTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun content() = ContentParser.parse { context.assets.open("content/$it").bufferedReader().use { it.readText() } }
    private fun capture(tag: String, name: String) {
        val bitmap = compose.onNodeWithTag(tag, true).captureToImage().asAndroidBitmap()
        val file = File(context.getExternalFilesDir("ui-review-20261126"), "$name.png")
        file.parentFile!!.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun ordersUseAllSixActualCatPortraits() {
        val content = content()
        val orders = listOf(1, 7, 10, 13, 16, 19).map { content.ordersForDay(it).first() }
        compose.setContent {
            CatTheme {
                Column(Modifier.width(360.dp).testTag("cat_orders"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    orders.forEach { order -> OrderCard(content, GameState(), order, List(order.req.size) { 0 }, false) {} }
                }
            }
        }
        orders.forEach { order ->
            compose.onNodeWithTag("order_avatar_${order.id}", true)
                .assertContentDescriptionEquals("${catName(catForDay(order.day))}的订单").assertIsDisplayed()
        }
        capture("cat_orders", "six-real-cat-orders")
    }

    @Test fun itemIconsAreProminentAndSeparateFromActions() = checkCardProportions(1f)
    @Test fun twoItemOrdersRemainReadableAtLargeFont() = checkCardProportions(1.4f)

    private fun checkCardProportions(fontScale: Float) {
        val content = content()
        val orders = listOf(content.orders.first { it.req.size == 1 }, content.orders.first { it.req.size == 2 })
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                CatTheme {
                    Column(Modifier.width(340.dp).testTag("order_proportions"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        orders.forEach { order -> OrderCard(content, GameState(), order, List(order.req.size) { 0 }, false) {} }
                    }
                }
            }
        }
        orders.forEach { order ->
            val avatar = compose.onNodeWithTag("order_avatar_${order.id}", true).fetchSemanticsNode().boundsInRoot
            val items = compose.onNodeWithTag("order_items_${order.id}", true).fetchSemanticsNode().boundsInRoot
            val action = compose.onNodeWithTag("deliver_${order.id}", true).fetchSemanticsNode().boundsInRoot
            assertTrue(avatar.right <= items.left)
            assertTrue(items.right <= action.left)
            assertTrue("Materials need most of the central space", items.width >= avatar.width * 2)
            order.req.forEachIndexed { index, req ->
                val icon = compose.onNodeWithTag("order_item_${order.id}_$index", true).fetchSemanticsNode().boundsInRoot
                val requirement = compose.onNodeWithTag("requirement_${order.id}_$index", true)
                requirement.assertIsDisplayed().assertContentDescriptionEquals("${content.itemById.getValue(req.itemId).name} 0/${req.count}")
                assertTrue("Material should be comparable in size to the cat portrait", icon.width >= avatar.width * 0.75f)
                assertTrue(icon.right <= action.left)
                val layouts = mutableListOf<TextLayoutResult>()
                compose.onNodeWithTag("order_count_${order.id}_$index", true)
                    .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                assertTrue(layouts.isNotEmpty())
                layouts.forEach { layout ->
                    assertTrue("The full count must remain visible", layout.multiParagraph.height <= layout.size.height + 1f)
                    assertFalse("The count must not wrap or truncate", layout.multiParagraph.didExceedMaxLines)
                }
            }
        }
        capture("order_proportions", if (fontScale == 1f) "orders-normal" else "orders-large-font")
    }

    @Test fun holdingAnOrderSwipeNeverRestartsGeneratorHintsOrSpawnsItems() {
        val content = content()
        val engine = GameEngine(content)
        val board = MutableList<Piece?>(BOARD_SIZE) { null }
        board[0] = Piece(1, "wood", 1, PieceKind.GENERATOR)
        board[8] = Piece(2, "wood", 1, PieceKind.GENERATOR)
        var state by mutableStateOf(GameState(board = board, energyAt = System.currentTimeMillis(), nextUid = 3, tutorialDone = true))
        compose.setContent { CatTheme { BoardScreen(content, state, GameSettings(), { state = engine.reduce(state, it).state }, {}, {}) } }
        compose.waitUntil(6_000) { compose.onAllNodesWithTag("generator_energy_badge", true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("orders").performTouchInput {
            down(center)
            moveTo(center - Offset(80f, 0f))
        }
        compose.onAllNodesWithTag("generator_energy_badge", true).assertCountEquals(0)
        val duringTouch = compose.onNodeWithTag("board").captureToImage().asAndroidBitmap()
        Thread.sleep(3_400)
        compose.onAllNodesWithTag("generator_energy_badge", true).assertCountEquals(0)
        val afterIdleThreshold = compose.onNodeWithTag("board").captureToImage().asAndroidBitmap()
        assertTrue("The board must not flash or pulse while an order gesture is held", duringTouch.sameAs(afterIdleThreshold))
        duringTouch.recycle()
        afterIdleThreshold.recycle()
        capture("board", "board-paused-during-order-drag")
        compose.onNodeWithTag("orders").performTouchInput { up() }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(board, state.board); assertEquals(MAX_ENERGY, state.energy) }
        compose.waitUntil(7_000) { compose.onAllNodesWithTag("generator_energy_badge", true).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithTag("generator_energy_badge", true).assertCountEquals(1)
    }
}
