package com.example.cathousemerge.game

import kotlin.math.min
import kotlin.random.Random

/** Applies one event to an immutable game snapshot. Rendering and persistence stay outside this class. */
class GameEngine(val content: GameContent) {
    fun newGame(now: Long): GameState {
        val initial = GameState(energyAt = now)
        return enterDay(initial, 1, now, grantEnergy = false)
    }

    fun recoverEnergy(state: GameState, now: Long): GameState {
        if (state.energy >= MAX_ENERGY) {
            return state.copy(energy = MAX_ENERGY, energyAt = now)
        }
        if (now <= state.energyAt) return state
        val intervals = (now - state.energyAt) / ENERGY_INTERVAL_MS
        if (intervals == 0L) return state
        val recovered = min(intervals, (MAX_ENERGY - state.energy).toLong()).toInt()
        val energy = state.energy + recovered
        val baseline = if (energy == MAX_ENERGY) now else state.energyAt + intervals * ENERGY_INTERVAL_MS
        return state.copy(energy = energy, energyAt = baseline)
    }

    fun generatorCap(day: Int): Int = if (day <= 3) 1 else ((day - 1) / 6 + 2).coerceIn(2, 5)

    fun orderProgress(state: GameState, order: OrderDef): List<Int> {
        val available = state.board.filterNotNull()
            .filter { it.kind == PieceKind.ITEM }
            .groupingBy(Piece::itemId)
            .eachCount()
            .toMutableMap()
        return order.req.map { requirement ->
            val count = min(available[requirement.itemId] ?: 0, requirement.count)
            available[requirement.itemId] = (available[requirement.itemId] ?: 0) - count
            count
        }
    }

    fun isOrderReady(state: GameState, order: OrderDef): Boolean =
        order.day == state.day && order.id !in state.delivered &&
            orderProgress(state, order).zip(order.req).all { (count, requirement) -> count >= requirement.count }

    fun canAdvance(state: GameState): Boolean =
        state.day in state.builtDays && content.ordersForDay(state.day).all { it.id in state.delivered }

    fun reduce(
        state: GameState,
        action: GameAction,
        now: Long = System.currentTimeMillis(),
        random: Double = Random.nextDouble(),
    ): GameResult {
        val current = recoverEnergy(state, now)
        return when (action) {
            is GameAction.Spawn -> spawn(current, action.index, random)
            is GameAction.Move -> move(current, action.from, action.to)
            is GameAction.TakeBubble -> takeBubble(current, action.uid, action.target)
            is GameAction.Store -> store(current, action.index)
            is GameAction.Retrieve -> retrieve(current, action.uid)
            is GameAction.Deliver -> deliver(current, action.orderId)
            GameAction.Build -> build(current)
            GameAction.NextDay -> nextDay(current, now)
            is GameAction.Sell -> sell(current, action.index)
            GameAction.ExpandWarehouse -> expandWarehouse(current)
            is GameAction.BuyGenerator -> buyGenerator(current, action.chain)
            is GameAction.SeenDialogue -> seeDialogue(current, action.sceneId)
            GameAction.TutorialComplete -> GameResult(current.copy(tutorialDone = true))
            GameAction.DebugEnergy -> GameResult(current.copy(energy = MAX_ENERGY, energyAt = now), "体力已补满")
            GameAction.DebugCoins -> GameResult(
                current.copy(coins = (current.coins.toLong() + 1_000).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()),
                "获得 1000 金币",
            )
            is GameAction.DebugJump -> debugJump(current, action.day, now)
            is GameAction.DebugFillOrder -> debugFillOrder(current, action.orderId)
        }
    }

    private fun spawn(state: GameState, index: Int, random: Double): GameResult {
        val generator = state.board.getOrNull(index)
        if (generator?.kind != PieceKind.GENERATOR) return GameResult(state)
        if (state.energy <= 0) return GameResult(state, "体力不足，稍后再来")
        val column = index % BOARD_COLS
        val row = index / BOARD_COLS
        val neighbors = listOf(
            if (column + 1 < BOARD_COLS) index + 1 else -1,
            if (row + 1 < BOARD_ROWS) index + BOARD_COLS else -1,
            if (column > 0) index - 1 else -1,
            if (row > 0) index - BOARD_COLS else -1,
        )
        val empty = neighbors.firstOrNull { it in state.board.indices && state.board[it] == null }
            ?: return GameResult(state, "先腾个位置，工具旁边需要空格")
        val weights = content.dropTables[generator.level] ?: return GameResult(state, "工具产出配置缺失")
        var remaining = if (random.isFinite()) random.coerceIn(0.0, Math.nextDown(1.0)) else 0.0
        var level = weights.last().level
        for (weight in weights) {
            if (remaining < weight.probability) {
                level = weight.level
                break
            }
            remaining -= weight.probability
        }
        if ("${generator.chain}_$level" !in content.itemById) return GameResult(state, "物品配置缺失")
        val board = state.board.toMutableList()
        board[empty] = Piece(state.nextUid, generator.chain, level, PieceKind.ITEM)
        return GameResult(
            state.copy(board = board, energy = state.energy - 1, nextUid = state.nextUid + 1),
            effect = GameEffect.SPAWN,
        )
    }

    private fun move(state: GameState, from: Int, to: Int): GameResult {
        if (from !in state.board.indices || to !in state.board.indices || from == to) return GameResult(state)
        val source = state.board[from] ?: return GameResult(state)
        val destination = state.board[to]
        val board = state.board.toMutableList()
        if (destination == null) {
            board[from] = null
            board[to] = source
            return GameResult(state.copy(board = board))
        }
        if (source.chain != destination.chain || source.kind != destination.kind || source.level != destination.level) {
            return GameResult(state, "同类、同级的物品才能合成")
        }
        if (source.kind == PieceKind.GENERATOR && source.level >= generatorCap(state.day)) {
            val message = if (state.day <= 3) "第 4 天解锁工具升级" else "本章工具已到顶"
            return GameResult(state, message)
        }
        val nextLevel = source.level + 1
        if (source.kind == PieceKind.ITEM && "${source.chain}_$nextLevel" !in content.itemById) {
            return GameResult(state, "已经是这条合成链的最高级")
        }
        board[from] = null
        board[to] = Piece(state.nextUid, source.chain, nextLevel, source.kind)
        return GameResult(
            state.copy(board = board, nextUid = state.nextUid + 1),
            effect = GameEffect.MERGE,
        )
    }

    private fun takeBubble(state: GameState, uid: Long, target: Int?): GameResult {
        val piece = state.bubble.firstOrNull { it.uid == uid } ?: return GameResult(state)
        val empty = target ?: state.board.indexOfFirst { it == null }
        if (empty !in state.board.indices || state.board[empty] != null) return GameResult(state, "这里放不下，先腾个空格")
        val board = state.board.toMutableList()
        board[empty] = piece
        return GameResult(state.copy(board = board, bubble = state.bubble.filterNot { it.uid == uid }))
    }

    private fun store(state: GameState, index: Int): GameResult {
        if (state.day < 4) return GameResult(state, "第 4 天开放仓库")
        val piece = state.board.getOrNull(index) ?: return GameResult(state)
        if (state.warehouse.size >= state.warehouseCapacity) return GameResult(state, "仓库满了，先取出一件物品")
        val board = state.board.toMutableList()
        board[index] = null
        return GameResult(state.copy(board = board, warehouse = state.warehouse + piece), "已收进仓库")
    }

    private fun retrieve(state: GameState, uid: Long): GameResult {
        if (state.day < 4) return GameResult(state, "第 4 天开放仓库")
        val piece = state.warehouse.firstOrNull { it.uid == uid } ?: return GameResult(state)
        val empty = state.board.indexOfFirst { it == null }
        if (empty == -1) return GameResult(state, "棋盘满了，先腾个位置")
        val board = state.board.toMutableList()
        board[empty] = piece
        return GameResult(state.copy(board = board, warehouse = state.warehouse.filterNot { it.uid == uid }))
    }

    private fun deliver(state: GameState, orderId: String): GameResult {
        val order = content.orderById[orderId] ?: return GameResult(state)
        if (order.day != state.day || order.id in state.delivered) return GameResult(state)
        val progress = orderProgress(state, order)
        val missingIndex = order.req.indices.firstOrNull { progress[it] < order.req[it].count }
        if (missingIndex != null) {
            val requirement = order.req[missingIndex]
            val name = content.itemById[requirement.itemId]?.name ?: requirement.itemId
            return GameResult(state, "还差 ${requirement.count - progress[missingIndex]} 个$name")
        }
        val board = state.board.toMutableList()
        for (requirement in order.req) {
            var remaining = requirement.count
            for (index in board.indices) {
                val piece = board[index]
                if (remaining > 0 && piece?.kind == PieceKind.ITEM && piece.itemId == requirement.itemId) {
                    board[index] = null
                    remaining--
                }
            }
        }
        return GameResult(
            state.copy(
                board = board,
                delivered = state.delivered + order.id,
                coins = (state.coins.toLong() + order.reward).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            ),
            "订单完成，获得 ${order.reward} 金币",
            GameEffect.DELIVER,
        )
    }

    private fun build(state: GameState): GameResult {
        if (state.day in state.builtDays) return GameResult(state)
        val day = content.day(state.day)
        if (state.coins < day.build.cost) return GameResult(state, "还差 ${day.build.cost - state.coins} 金币")
        var result = state.copy(coins = state.coins - day.build.cost, builtDays = state.builtDays + state.day)
        if (day.build.step == 3) {
            val chain = content.chains.filter { it.unlockDay <= day.day }.maxByOrNull { it.unlockDay }
            if (chain != null) result = grantGenerators(result, chain.id, 2)
        }
        return GameResult(result, "${day.build.name}，完成了", GameEffect.BUILD)
    }

    private fun nextDay(state: GameState, now: Long): GameResult {
        if (state.completed) return GameResult(state)
        if (!canAdvance(state)) return GameResult(state, "完成今天的全部订单和修缮后，再迎接明天")
        if (state.day == content.days.maxOf(DayDef::day)) {
            return GameResult(state.copy(completed = true), "旧屋修好了，猫也有了家", GameEffect.DAY)
        }
        return GameResult(enterDay(state, state.day + 1, now, grantEnergy = true), effect = GameEffect.DAY)
    }

    private fun enterDay(state: GameState, day: Int, now: Long, grantEnergy: Boolean): GameState {
        val definition = content.day(day)
        val energy = if (grantEnergy) min(MAX_ENERGY, state.energy + 20) else state.energy
        val intro = definition.dialogue["intro"]?.let(content.dialogue.scenes::get)
        var next = state.copy(
            day = day,
            energy = energy,
            energyAt = if (energy == MAX_ENERGY) now else state.energyAt,
            unlockedCats = state.unlockedCats + speakers(intro),
        )
        val chain = content.chains.firstOrNull { it.generator == definition.newGenerator }
        if (chain != null) next = grantGenerators(next, chain.id, 2)
        return next
    }

    private fun grantGenerators(state: GameState, chain: String, count: Int): GameState {
        val tools = List(count) { Piece(state.nextUid + it, chain, 1, PieceKind.GENERATOR) }
        return state.copy(bubble = state.bubble + tools, nextUid = state.nextUid + count)
    }

    private fun sell(state: GameState, index: Int): GameResult {
        if (state.day < 2) return GameResult(state, "第 2 天解锁卖出")
        val piece = state.board.getOrNull(index) ?: return GameResult(state)
        if (piece.kind == PieceKind.GENERATOR) return GameResult(state, "工具需要留着，可以收进仓库")
        val definition = content.itemById[piece.itemId] ?: return GameResult(state)
        val price = (definition.value / 2).coerceAtLeast(1)
        val board = state.board.toMutableList()
        board[index] = null
        return GameResult(
            state.copy(board = board, coins = (state.coins.toLong() + price).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()),
            "卖出${definition.name}，获得 $price 金币",
        )
    }

    private fun expandWarehouse(state: GameState): GameResult {
        if (state.day < 4) return GameResult(state, "第 4 天开放仓库")
        val price = WAREHOUSE_PRICES.getOrNull(state.warehouseCapacity - 6)
            ?: return GameResult(state, "仓库已经扩到 12 格")
        if (state.coins < price) return GameResult(state, "还差 ${price - state.coins} 金币")
        return GameResult(state.copy(coins = state.coins - price, warehouseCapacity = state.warehouseCapacity + 1), "仓库增加了 1 格")
    }

    private fun buyGenerator(state: GameState, chainId: String): GameResult {
        if (state.day < 4) return GameResult(state, "第 4 天解锁工具购买")
        val chain = content.chainById[chainId] ?: return GameResult(state)
        if (chain.unlockDay > state.day) return GameResult(state, "这台工具还没有解锁")
        val price = 900L + state.purchasedGenerators * 180L
        if (state.coins < price) return GameResult(state, "还差 ${price - state.coins} 金币")
        val next = state.copy(coins = (state.coins - price).toInt(), purchasedGenerators = state.purchasedGenerators + 1)
        return GameResult(grantGenerators(next, chainId, 1), "新工具已放进订单行的气泡")
    }

    private fun seeDialogue(state: GameState, sceneId: String): GameResult {
        val scene = content.dialogue.scenes[sceneId] ?: return GameResult(state)
        if (scene.day > state.day) return GameResult(state)
        return GameResult(state.copy(seenDialogue = state.seenDialogue + sceneId, unlockedCats = state.unlockedCats + speakers(scene)))
    }

    private fun speakers(scene: DialogueScene?): Set<String> =
        scene?.lines?.map(DialogueLine::who)?.filter { it != "narrator" && it != "player" }?.toSet().orEmpty()

    private fun debugJump(state: GameState, day: Int, now: Long): GameResult {
        if (content.days.none { it.day == day }) return GameResult(state, "没有这一天")
        var uid = state.nextUid
        val generators = content.chains.filter { it.unlockDay <= day }.map {
            Piece(uid++, it.id, min(3, generatorCap(day)), PieceKind.GENERATOR)
        }
        val board = MutableList<Piece?>(BOARD_SIZE) { null }
        generators.forEachIndexed { index, piece -> board[index * BOARD_COLS] = piece }
        val cats = content.dialogue.scenes.values.filter { it.day <= day }.flatMap { speakers(it) }.toSet()
        return GameResult(
            state.copy(
                day = day,
                coins = maxOf(10_000, state.coins),
                energy = MAX_ENERGY,
                energyAt = now,
                board = board,
                warehouse = emptyList(),
                bubble = emptyList(),
                delivered = content.orders.filter { it.day < day }.map(OrderDef::id).toSet(),
                builtDays = content.days.filter { it.day < day }.map(DayDef::day).toSet(),
                seenDialogue = emptySet(),
                unlockedCats = cats,
                nextUid = uid,
                completed = false,
                tutorialDone = true,
            ),
            "已跳到第 $day 天",
        )
    }

    private fun debugFillOrder(state: GameState, orderId: String): GameResult {
        val order = content.orderById[orderId] ?: return GameResult(state)
        if (order.day != state.day || orderId in state.delivered) return GameResult(state)
        val progress = orderProgress(state, order)
        val missing = order.req.flatMapIndexed { index, requirement ->
            List(requirement.count - progress[index]) { requirement }
        }
        if (missing.any { it.itemId !in content.itemById }) return GameResult(state, "订单含有未定义的物品")
        val empty = state.board.indices.filter { state.board[it] == null }
        if (empty.size < missing.size) return GameResult(state, "棋盘空格不足，先腾个位置")
        val board = state.board.toMutableList()
        missing.forEachIndexed { index, requirement ->
            board[empty[index]] = Piece(state.nextUid + index, requirement.chain, requirement.level, PieceKind.ITEM)
        }
        return GameResult(state.copy(board = board, nextUid = state.nextUid + missing.size), "订单物品已补齐")
    }

    companion object {
        val WAREHOUSE_PRICES = listOf(450, 630, 885, 1235, 1730, 2420)
    }
}
