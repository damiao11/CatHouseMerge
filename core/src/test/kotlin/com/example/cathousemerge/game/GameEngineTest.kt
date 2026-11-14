package com.example.cathousemerge.game

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {
    private val content = ContentParser.parse { File("docs/data", it).readText() }
    private val engine = GameEngine(content)
    private val now = 1_000_000L

    @Test
    fun newGameOffersTwoToolsWithUniqueIdentifiers() {
        val state = engine.newGame(now)
        assertEquals(BOARD_SIZE, state.board.size)
        assertTrue(state.board.all { it == null })
        assertEquals(2, state.bubble.size)
        assertEquals(2, state.bubble.map(Piece::uid).toSet().size)
        assertTrue(state.bubble.all { it.chain == "wood" && it.level == 1 && it.kind == PieceKind.GENERATOR })
        assertTrue(state.bubble.all { it.uid < state.nextUid })
        assertEquals(MAX_ENERGY, state.energy)
        assertTrue("daju" in state.unlockedCats)
    }

    @Test
    fun energyRecoversEveryTwoMinutesAndKeepsPartialInterval() {
        val state = engine.newGame(now).copy(energy = 100)
        assertEquals(100, engine.recoverEnergy(state, now + ENERGY_INTERVAL_MS - 1).energy)
        val recovered = engine.recoverEnergy(state, now + 2 * ENERGY_INTERVAL_MS + 15_000)
        assertEquals(102, recovered.energy)
        assertEquals(now + 2 * ENERGY_INTERVAL_MS, recovered.energyAt)
        assertEquals(103, engine.recoverEnergy(recovered, now + 3 * ENERGY_INTERVAL_MS).energy)
    }

    @Test
    fun fullEnergyDoesNotBankRecoveryAndClockRollbackAwardsNothing() {
        val initial = engine.newGame(now)
        val full = engine.recoverEnergy(initial, now + 10 * ENERGY_INTERVAL_MS)
        val spent = full.copy(energy = 149)
        assertEquals(149, engine.recoverEnergy(spent, full.energyAt + 1).energy)
        assertEquals(spent, engine.recoverEnergy(spent, now - 1))
        val almostFull = initial.copy(energy = 149)
        val capped = engine.recoverEnergy(almostFull, now + 4 * ENERGY_INTERVAL_MS)
        assertEquals(150, capped.energy)
        assertEquals(now + 4 * ENERGY_INTERVAL_MS, capped.energyAt)
    }

    @Test
    fun spawningUsesNeighborPriorityAndChargesExactlyOneEnergy() {
        val initial = withBoard(0 to generator(1))
        val first = engine.reduce(initial, GameAction.Spawn(0), now, 0.0)
        assertEquals(GameEffect.SPAWN, first.effect)
        assertEquals(149, first.state.energy)
        assertEquals("wood_1", first.state.board[1]?.itemId)
        assertEquals(initial.nextUid + 1, first.state.nextUid)
        val second = engine.reduce(first.state, GameAction.Spawn(0), now, 0.0)
        assertEquals("wood_1", second.state.board[7]?.itemId)
        assertEquals(148, second.state.energy)
    }

    @Test
    fun edgeGeneratorNeverWrapsIntoNextRow() {
        val state = withBoard(6 to generator(1))
        val result = engine.reduce(state, GameAction.Spawn(6), now, 0.0).state
        assertNull(result.board[7])
        assertEquals("wood_1", result.board[13]?.itemId)
    }

    @Test
    fun blockedGeneratorAndInsufficientEnergyKeepResources() {
        val state = withBoard(0 to generator(1), 1 to item(2), 7 to item(3))
        assertEquals(state, engine.reduce(state, GameAction.Spawn(0), now).state)
        val emptyEnergy = withBoard(0 to generator(1)).copy(energy = 0)
        assertEquals(emptyEnergy, engine.reduce(emptyEnergy, GameAction.Spawn(0), now).state)
        assertEquals(state, engine.reduce(state, GameAction.Spawn(-1), now).state)
    }

    @Test
    fun generatorWeightsChooseConfiguredLevelsWithoutDroppingTopItems() {
        val state = withBoard(0 to generator(1, level = 5)).copy(day = 19)
        assertEquals(2, engine.reduce(state, GameAction.Spawn(0), now, 0.1).state.board[1]?.level)
        assertEquals(3, engine.reduce(state, GameAction.Spawn(0), now, 0.5).state.board[1]?.level)
        assertEquals(4, engine.reduce(state, GameAction.Spawn(0), now, 0.99).state.board[1]?.level)
    }

    @Test
    fun matchingItemsMergeAndMovingToEmptyCellPreservesIdentifier() {
        val state = withBoard(0 to item(1, level = 2), 1 to item(2, level = 2))
        val merged = engine.reduce(state, GameAction.Move(0, 1), now)
        assertEquals(GameEffect.MERGE, merged.effect)
        assertNull(merged.state.board[0])
        assertEquals(3, merged.state.board[1]?.level)
        assertEquals(state.nextUid, merged.state.board[1]?.uid)
        assertEquals(state.nextUid + 1, merged.state.nextUid)
        val moved = engine.reduce(merged.state, GameAction.Move(1, 62), now).state
        assertNull(moved.board[1])
        assertEquals(merged.state.board[1], moved.board[62])
    }

    @Test
    fun incompatibleOrMaximumItemsStayInPlace() {
        val incompatible = listOf(
            item(2, chain = "metal"),
            item(2, level = 2),
            generator(2),
        )
        for (other in incompatible) {
            val state = withBoard(0 to item(1), 1 to other)
            assertEquals(state, engine.reduce(state, GameAction.Move(0, 1), now).state)
        }
        val maximum = withBoard(0 to item(1, level = 6), 1 to item(2, level = 6))
        assertEquals(maximum, engine.reduce(maximum, GameAction.Move(0, 1), now).state)
        assertEquals(maximum, engine.reduce(maximum, GameAction.Move(0, -1), now).state)
        assertEquals(maximum, engine.reduce(maximum, GameAction.Move(0, 63), now).state)
    }

    @Test
    fun generatorUpgradesRespectTutorialAndChapterCaps() {
        val initial = withBoard(0 to generator(1), 1 to generator(2))
        assertEquals(initial, engine.reduce(initial, GameAction.Move(0, 1), now).state)
        val fourthDay = initial.copy(day = 4)
        assertEquals(2, engine.reduce(fourthDay, GameAction.Move(0, 1), now).state.board[1]?.level)
        val capped = withBoard(0 to generator(1, level = 2), 1 to generator(2, level = 2)).copy(day = 4)
        assertEquals(capped, engine.reduce(capped, GameAction.Move(0, 1), now).state)
        assertEquals(listOf(1, 2, 3, 4, 5), listOf(1, 4, 7, 13, 19).map(engine::generatorCap))
    }

    @Test
    fun takingToolIsAtomicAndCannotDuplicateIt() {
        val state = engine.newGame(now)
        val tool = state.bubble.first()
        val placed = engine.reduce(state, GameAction.TakeBubble(tool.uid, 0), now).state
        assertEquals(tool, placed.board[0])
        assertEquals(1, placed.bubble.size)
        assertEquals(placed, engine.reduce(placed, GameAction.TakeBubble(tool.uid, 1), now).state)
        val occupied = engine.reduce(placed, GameAction.TakeBubble(placed.bubble.first().uid, 0), now).state
        assertEquals(placed, occupied)
    }

    @Test
    fun deliveryRequiresAllBoardItemsAndPaysOnce() {
        val order = content.ordersForDay(1).first()
        val empty = engine.newGame(now)
        assertFalse(engine.isOrderReady(empty, order))
        assertEquals(empty, engine.reduce(empty, GameAction.Deliver(order.id), now).state)
        val ready = fillOrder(empty, order)
        assertTrue(engine.isOrderReady(ready, order))
        val result = engine.reduce(ready, GameAction.Deliver(order.id), now)
        assertEquals(GameEffect.DELIVER, result.effect)
        assertEquals(order.reward, result.state.coins)
        assertTrue(order.id in result.state.delivered)
        assertTrue(result.state.board.all { it == null })
        assertEquals(result.state, engine.reduce(result.state, GameAction.Deliver(order.id), now).state)
        assertEquals(empty, engine.reduce(empty, GameAction.Deliver("o2_1"), now).state)
    }

    @Test
    fun warehouseItemsDoNotCountForOrders() {
        val order = content.ordersForDay(4).first()
        val state = fillOrder(engine.newGame(now).copy(day = 4), order)
        val index = state.board.indexOfFirst { it != null }
        val stored = engine.reduce(state, GameAction.Store(index), now).state
        assertFalse(engine.isOrderReady(stored, order))
        assertEquals(stored, engine.reduce(stored, GameAction.Deliver(order.id), now).state)
        val restored = engine.reduce(stored, GameAction.Retrieve(stored.warehouse.first().uid), now).state
        assertTrue(engine.isOrderReady(restored, order))
        assertTrue(restored.warehouse.isEmpty())
    }

    @Test
    fun duplicateRequirementsCannotReuseOnePieceTwice() {
        val original = content.ordersForDay(1).first()
        val order = original.copy(req = listOf(Requirement("wood", 2, 1), Requirement("wood", 2, 1)))
        val custom = content.copy(orders = content.orders.map { if (it.id == order.id) order else it })
        val customEngine = GameEngine(custom)
        val state = withBoard(0 to item(1, level = 2))
        assertEquals(listOf(1, 0), customEngine.orderProgress(state, order))
        assertFalse(customEngine.isOrderReady(state, order))
        assertEquals(state, customEngine.reduce(state, GameAction.Deliver(order.id), now).state)
    }

    @Test
    fun warehouseCapacityAndFullBoardFailuresPreservePieces() {
        val piece = item(1)
        val locked = withBoard(0 to piece)
        assertEquals(locked, engine.reduce(locked, GameAction.Store(0), now).state)
        val full = locked.copy(day = 4, warehouse = List(6) { item(10L + it) })
        assertEquals(full, engine.reduce(full, GameAction.Store(0), now).state)
        val fullBoard = full.copy(board = List(BOARD_SIZE) { item(100L + it) })
        assertEquals(fullBoard, engine.reduce(fullBoard, GameAction.Retrieve(fullBoard.warehouse.first().uid), now).state)
        val tool = withBoard(0 to generator(1)).copy(day = 4)
        val storedTool = engine.reduce(tool, GameAction.Store(0), now).state
        assertEquals(PieceKind.GENERATOR, storedTool.warehouse.single().kind)
        assertEquals(tool.board[0], engine.reduce(storedTool, GameAction.Retrieve(1), now).state.board[0])
    }

    @Test
    fun expandingWarehouseUsesEachPriceOnceAndStopsAtTwelve() {
        var state = engine.newGame(now).copy(day = 4, coins = 7_350)
        for (price in GameEngine.WAREHOUSE_PRICES) {
            val previous = state
            state = engine.reduce(state, GameAction.ExpandWarehouse, now).state
            assertEquals(previous.coins - price, state.coins)
            assertEquals(previous.warehouseCapacity + 1, state.warehouseCapacity)
        }
        assertEquals(12, state.warehouseCapacity)
        assertEquals(0, state.coins)
        assertEquals(state, engine.reduce(state, GameAction.ExpandWarehouse, now).state)
        val poor = engine.newGame(now).copy(day = 4, coins = 449)
        assertEquals(poor, engine.reduce(poor, GameAction.ExpandWarehouse, now).state)
    }

    @Test
    fun buyingToolsRequiresUnlockedChainAndFunds() {
        val state = engine.newGame(now).copy(day = 4, coins = 1_980)
        assertEquals(state, engine.reduce(state, GameAction.BuyGenerator("fabric"), now).state)
        val bought = engine.reduce(state, GameAction.BuyGenerator("wood"), now).state
        assertEquals(1_080, bought.coins)
        assertEquals(1, bought.purchasedGenerators)
        assertEquals(state.bubble.size + 1, bought.bubble.size)
        val second = engine.reduce(bought, GameAction.BuyGenerator("metal"), now).state
        assertEquals(0, second.coins)
        assertEquals(2, second.purchasedGenerators)
        assertEquals(second, engine.reduce(second, GameAction.BuyGenerator("wood"), now).state)
        assertTrue(second.bubble.all { it.uid < second.nextUid })
    }

    @Test
    fun sellingPaysItemValueAndCannotSellGenerators() {
        val state = withBoard(0 to item(1, level = 3), 1 to generator(2)).copy(day = 2)
        val sold = engine.reduce(state, GameAction.Sell(0), now).state
        assertEquals((content.itemById.getValue("wood_3").value / 2).coerceAtLeast(1), sold.coins)
        assertNull(sold.board[0])
        assertEquals(sold, engine.reduce(sold, GameAction.Sell(1), now).state)
        assertEquals(sold, engine.reduce(sold, GameAction.Sell(0), now).state)
    }

    @Test
    fun buildingChargesOnceAndAdvancingRequiresOrdersToo() {
        val state = engine.newGame(now).copy(coins = 130)
        assertEquals(state, engine.reduce(state, GameAction.NextDay, now).state)
        val built = engine.reduce(state, GameAction.Build, now).state
        assertEquals(0, built.coins)
        assertTrue(1 in built.builtDays)
        assertEquals(built, engine.reduce(built, GameAction.Build, now).state)
        assertEquals(built, engine.reduce(built, GameAction.NextDay, now).state)
        val ready = built.copy(delivered = content.ordersForDay(1).map(OrderDef::id).toSet(), energy = 100)
        val next = engine.reduce(ready, GameAction.NextDay, now).state
        assertEquals(2, next.day)
        assertEquals(120, next.energy)
        assertEquals(ready.delivered, next.delivered)
        assertFalse(engine.canAdvance(next))
    }

    @Test
    fun roomRewardIsGrantedExactlyOnceAndPreservesPendingTools() {
        val state = engine.newGame(now).copy(day = 3, coins = 220)
        val built = engine.reduce(state, GameAction.Build, now).state
        assertEquals(state.bubble.size + 2, built.bubble.size)
        assertTrue(built.bubble.all { it.chain == "wood" })
        assertEquals(built, engine.reduce(built, GameAction.Build, now).state)
        val ready = built.copy(delivered = content.ordersForDay(3).map(OrderDef::id).toSet())
        val fourthDay = engine.reduce(ready, GameAction.NextDay, now).state
        assertEquals(built.bubble.size + 2, fourthDay.bubble.size)
        assertEquals(2, fourthDay.bubble.count { it.chain == "metal" })
        assertEquals(fourthDay.bubble.size, fourthDay.bubble.map(Piece::uid).toSet().size)
    }

    @Test
    fun seenDialogueIsIdempotentAndFutureScenesStayUnseen() {
        val state = engine.newGame(now)
        val seen = engine.reduce(state, GameAction.SeenDialogue("d1_intro"), now).state
        assertEquals(setOf("d1_intro"), seen.seenDialogue)
        assertEquals(seen, engine.reduce(seen, GameAction.SeenDialogue("d1_intro"), now).state)
        assertEquals(seen, engine.reduce(seen, GameAction.SeenDialogue("d24_intro"), now).state)
        assertEquals(seen, engine.reduce(seen, GameAction.SeenDialogue("missing"), now).state)
    }

    @Test
    fun debugFillOnlySuppliesMissingItemsAndKeepsIdentifiersUnique() {
        val state = engine.newGame(now)
        val order = content.ordersForDay(1).first()
        val filled = engine.reduce(state, GameAction.DebugFillOrder(order.id), now).state
        assertTrue(engine.isOrderReady(filled, order))
        assertEquals(filled, engine.reduce(filled, GameAction.DebugFillOrder(order.id), now).state)
        val all = filled.board.filterNotNull() + filled.bubble
        assertEquals(all.size, all.map(Piece::uid).toSet().size)
        assertTrue(all.all { it.uid < filled.nextUid })
    }

    @Test
    fun allTwentyFourDaysCanBeCompletedThroughNormalSpawnMergeDeliverAndBuildActions() {
        var state = engine.newGame(now)
        var clock = now
        var spawnCount = 0

        fun apply(action: GameAction) {
            state = engine.reduce(state, action, clock, 0.0).state
        }

        fun generatorIndex(chain: String): Int {
            val existing = state.board.indexOfFirst { it?.kind == PieceKind.GENERATOR && it.chain == chain }
            if (existing >= 0) return existing
            val bubble = state.bubble.first { it.chain == chain }
            val target = (0 until BOARD_COLS).first { state.board[it] == null }
            apply(GameAction.TakeBubble(bubble.uid, target))
            return target
        }

        fun craft(chain: String, level: Int): Int {
            if (level > 1) {
                val first = craft(chain, level - 1)
                val second = craft(chain, level - 1)
                apply(GameAction.Move(first, second))
                assertEquals(level, state.board[second]?.level)
                return second
            }
            val tool = generatorIndex(chain)
            if (state.energy == 0) {
                clock += MAX_ENERGY * ENERGY_INTERVAL_MS
                state = engine.recoverEnergy(state, clock)
            }
            val expectedUid = state.nextUid
            apply(GameAction.Spawn(tool))
            spawnCount++
            val produced = state.board.indexOfFirst { it?.uid == expectedUid }
            assertTrue("Generator for $chain must produce a piece", produced >= 0)
            val workspace = (14 until BOARD_SIZE).first { state.board[it] == null }
            apply(GameAction.Move(produced, workspace))
            return workspace
        }

        for (day in 1..24) {
            assertEquals(day, state.day)
            for (order in content.ordersForDay(day)) {
                for (requirement in order.req) {
                    repeat(requirement.count) { craft(requirement.chain, requirement.level) }
                }
                assertTrue("Order ${order.id} must be achievable", engine.isOrderReady(state, order))
                apply(GameAction.Deliver(order.id))
                assertTrue(order.id in state.delivered)
            }
            val beforeBuild = state.coins
            apply(GameAction.Build)
            assertEquals(beforeBuild - content.day(day).build.cost, state.coins)
            assertTrue(engine.canAdvance(state))
            apply(GameAction.NextDay)
        }
        assertTrue(state.completed)
        assertEquals(24, state.day)
        assertEquals(112, state.delivered.size)
        assertEquals(24, state.builtDays.size)
        assertTrue(spawnCount > 1_000)
        assertEquals(content.orders.sumOf(OrderDef::reward) - content.days.sumOf { it.build.cost }, state.coins)
        val completed = state
        apply(GameAction.NextDay)
        assertEquals(completed, state)
        val pieces = state.board.filterNotNull() + state.warehouse + state.bubble
        assertEquals(pieces.size, pieces.map(Piece::uid).toSet().size)
        assertNotEquals(engine.newGame(now), state)
    }

    private fun item(uid: Long, chain: String = "wood", level: Int = 1): Piece =
        Piece(uid, chain, level, PieceKind.ITEM)

    private fun generator(uid: Long, chain: String = "wood", level: Int = 1): Piece =
        Piece(uid, chain, level, PieceKind.GENERATOR)

    private fun withBoard(vararg entries: Pair<Int, Piece>): GameState {
        val board = MutableList<Piece?>(BOARD_SIZE) { null }
        entries.forEach { (index, piece) -> board[index] = piece }
        return engine.newGame(now).copy(board = board, bubble = emptyList(), nextUid = 100)
    }

    private fun fillOrder(state: GameState, order: OrderDef): GameState {
        var uid = state.nextUid
        val board = state.board.toMutableList()
        for (requirement in order.req) {
            repeat(requirement.count) {
                val empty = board.indexOfFirst { it == null }
                board[empty] = item(uid++, requirement.chain, requirement.level)
            }
        }
        return state.copy(board = board, nextUid = uid)
    }
}
