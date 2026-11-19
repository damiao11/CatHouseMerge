package com.example.cathousemerge.game

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class IterationFeedbackTest {
    private val content = ContentParser.parse { File("docs/data", it).readText() }
    private val now = 1_000_000L
    private fun item(uid: Long, chain: String = "wood") = Piece(uid, chain, 1, PieceKind.ITEM)
    private fun generator(uid: Long, chain: String = "wood", level: Int = 1) = Piece(uid, chain, level, PieceKind.GENERATOR)
    private fun state(vararg entries: Pair<Int, Piece>): GameState {
        val board = MutableList<Piece?>(BOARD_SIZE) { null }
        entries.forEach { (index, piece) -> board[index] = piece }
        return GameState(board = board, energyAt = now, nextUid = 100)
    }
    private fun order(id: String, vararg requirements: Requirement) = OrderDef(id, 1, "normal", requirements.toList(), 10, 10)
    private fun engine(vararg orders: OrderDef): GameEngine = GameEngine(content.copy(
        orders = orders.toList(), days = listOf(content.day(1).copy(orderIds = orders.map(OrderDef::id))),
    ))

    @Test fun surroundedGeneratorUsesNearestEmptyThenTopThenLeft() {
        val game = GameEngine(content)
        val board = MutableList<Piece?>(BOARD_SIZE) { item(it.toLong() + 1) }
        board[24] = generator(80)
        listOf(10, 22, 26, 38, 62).forEach { board[it] = null }
        val initial = state().copy(board = board)
        val result = game.reduce(initial, GameAction.Spawn(24), now, 0.0)
        assertEquals(GameEffect.SPAWN, result.effect)
        assertEquals(149, result.state.energy)
        assertEquals(100L, result.state.board[10]?.uid)
        assertNull(result.state.board[22])
        val tie = board.toMutableList()
        listOf(10, 22, 26, 38).forEach { tie[it] = item(it.toLong() + 1) }
        tie[8] = null
        tie[12] = null
        assertEquals(100L, game.reduce(initial.copy(board = tie), GameAction.Spawn(24), now, 0.0).state.board[8]?.uid)
    }

    @Test fun anySingleEmptyCellCanReceiveAnItemFromAnyGeneratorPosition() {
        val game = GameEngine(content)
        for (generatorIndex in 0 until BOARD_SIZE) {
            for (empty in 0 until BOARD_SIZE) {
                if (empty == generatorIndex) continue
                val board = MutableList<Piece?>(BOARD_SIZE) { item(it.toLong() + 1) }
                board[generatorIndex] = generator(90)
                board[empty] = null
                val result = game.reduce(state().copy(board = board), GameAction.Spawn(generatorIndex), now, 0.0)
                assertEquals("generator=$generatorIndex empty=$empty", 100L, result.state.board[empty]?.uid)
                assertEquals(149, result.state.energy)
            }
        }
    }

    @Test fun nearestOrderCanMarkAnItemSatisfiedBeforeTheWholeCardIsReady() {
        val far = order("far", Requirement("wood", 1, 3), Requirement("metal", 1, 1))
        val near = order("near", Requirement("wood", 1, 1), Requirement("fabric", 1, 1))
        val game = engine(far, near)
        val board = state(0 to item(1))
        val feedback = game.itemDemandFeedback(board).getValue("wood_1")
        assertEquals("near", feedback.orderId)
        assertTrue(feedback.satisfied)
        assertEquals(1, feedback.orderRemaining)
        assertFalse(game.isOrderReady(board, near))
    }

    @Test fun tiedOrdersFollowDisplayOrderAndDeliveredOrdersAreIgnored() {
        val first = order("first", Requirement("wood", 1, 2))
        val second = order("second", Requirement("wood", 1, 1), Requirement("metal", 1, 1))
        val game = engine(first, second)
        val board = state(0 to item(1))
        assertEquals("first", game.itemDemandFeedback(board).getValue("wood_1").orderId)
        assertFalse(game.itemDemandFeedback(board).getValue("wood_1").satisfied)
        val after = board.copy(delivered = setOf("first"))
        assertEquals("second", game.itemDemandFeedback(after).getValue("wood_1").orderId)
        assertTrue(game.itemDemandFeedback(after).getValue("wood_1").satisfied)
        assertTrue(game.itemDemandFeedback(after.copy(delivered = setOf("first", "second"))).isEmpty())
    }

    @Test fun warehouseAndToolsNeverCountTowardDemandAndDuplicateRequirementsCannotReuseItems() {
        val request = order("duplicate", Requirement("wood", 1, 1), Requirement("wood", 1, 1))
        val game = engine(request)
        val board = state(0 to item(1), 1 to generator(2)).copy(warehouse = listOf(item(3)))
        assertEquals(listOf(1, 0), game.orderProgress(board, request))
        assertEquals(1, game.itemDemandFeedback(board).getValue("wood_1").available)
        assertEquals(2, game.itemDemandFeedback(board).getValue("wood_1").required)
        assertFalse(game.itemDemandFeedback(board).getValue("wood_1").satisfied)
        val full = state(0 to item(1), 1 to item(2))
        assertTrue(game.isOrderReady(full, request))
        assertTrue(game.itemDemandFeedback(full).getValue("wood_1").satisfied)
    }

    @Test fun hintsPreferCurrentRoomThenHighestLevelThenBoardOrder() {
        val board = state(0 to generator(1, "wood", 5), 1 to generator(2, "fabric", 1), 2 to generator(3, "fabric", 2), 3 to generator(4, "fabric", 2)).board
        assertEquals("fabric", GeneratorHints.mainChain(content, 8))
        assertEquals(3L, GeneratorHints.at(board, "fabric", 10, 3_000, 0, emptyMap())?.uid)
        assertEquals(1L, GeneratorHints.at(board, "metal", 10, 3_000, 0, emptyMap())?.uid)
    }

    @Test fun hintsWaitThreeSecondsAlternateWithoutOverlapAndPauseForTenSeconds() {
        val board = state(0 to generator(1)).board
        fun hint(time: Long) = GeneratorHints.at(board, "wood", 1, time, 0, emptyMap())!!
        assertEquals(GeneratorHintPhase.WAITING, hint(2_999).phase)
        assertEquals(GeneratorHintPhase.BADGE, hint(3_000).phase)
        assertEquals(1f, hint(4_999).scale, 0.0001f)
        assertEquals(GeneratorHintPhase.PULSE, hint(5_000).phase)
        assertEquals(1.06f, hint(5_300).scale, 0.0001f)
        assertEquals(1f, hint(5_600).scale, 0.0001f)
        assertEquals(GeneratorHintPhase.BADGE, hint(7_000).phase)
        assertNull(GeneratorHints.at(board, "wood", 1, 9_999, 0, mapOf(1L to 0L)))
        assertNotNull(GeneratorHints.at(board, "wood", 1, 10_000, 0, mapOf(1L to 0L)))
    }

    @Test fun lowEnergyAndReducedMotionNeverPulse() {
        val board = state(0 to generator(1)).board
        val noEnergy = GeneratorHints.at(board, "wood", 0, 5_300, 0, emptyMap())!!
        assertEquals(GeneratorHintPhase.NO_ENERGY, noEnergy.phase)
        assertEquals(1f, noEnergy.scale, 0f)
        val reduced = GeneratorHints.at(board, "wood", 10, 5_300, 0, emptyMap(), reducedMotion = true)!!
        assertEquals(GeneratorHintPhase.BADGE, reduced.phase)
        assertEquals(1f, reduced.scale, 0f)
    }
}
