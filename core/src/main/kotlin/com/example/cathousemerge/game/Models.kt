package com.example.cathousemerge.game

import kotlinx.serialization.Serializable

const val BOARD_COLS = 7
const val BOARD_ROWS = 9
const val BOARD_SIZE = BOARD_COLS * BOARD_ROWS
const val MAX_ENERGY = 150
const val ENERGY_INTERVAL_MS = 120_000L

@Serializable
data class ItemDef(
    val id: String,
    val chain: String,
    val level: Int,
    val name: String,
    val value: Int,
    val asset: String,
    val energy: Int = 1 shl (level - 1),
)

@Serializable
data class ChainDef(
    val id: String,
    val name: String,
    val generator: String,
    val unlockDay: Int,
    val itemIds: List<String>,
)

@Serializable
data class Requirement(val chain: String, val level: Int, val count: Int) {
    val itemId: String get() = "${chain}_$level"
}

@Serializable
data class OrderDef(
    val id: String,
    val day: Int,
    val type: String,
    val req: List<Requirement>,
    val value: Int,
    val reward: Int,
    val equiv: Int = 0,
)

@Serializable
data class BuildDef(val step: Int, val name: String, val cost: Int)

@Serializable
data class DayDef(
    val day: Int,
    val room: String,
    val newGenerator: String? = null,
    val orderIds: List<String>,
    val build: BuildDef,
    val dialogue: Map<String, String>,
)

@Serializable
data class DialogueLine(val who: String, val text: String)

@Serializable
data class DialogueScene(val day: Int, val kind: String, val lines: List<DialogueLine>)

@Serializable
data class AlmanacEntry(val cat: String, val title: String, val text: String)

@Serializable
data class Letter(val title: String, val text: String)

@Serializable
data class DialogueData(
    val version: Int,
    val scenes: Map<String, DialogueScene>,
    val almanac: List<AlmanacEntry>,
    val letters: List<Letter>,
)

data class DropWeight(val level: Int, val probability: Double)

data class GameContent(
    val chains: List<ChainDef>,
    val items: List<ItemDef>,
    val orders: List<OrderDef>,
    val days: List<DayDef>,
    val dialogue: DialogueData,
    val dropTables: Map<Int, List<DropWeight>>,
) {
    val itemById = items.associateBy(ItemDef::id)
    val chainById = chains.associateBy(ChainDef::id)
    val orderById = orders.associateBy(OrderDef::id)
    fun day(number: Int): DayDef = days.first { it.day == number }
    fun ordersForDay(number: Int): List<OrderDef> = day(number).orderIds.map(orderById::getValue)
}

@Serializable
enum class PieceKind { ITEM, GENERATOR }

@Serializable
data class Piece(val uid: Long, val chain: String, val level: Int, val kind: PieceKind) {
    val itemId: String get() = "${chain}_$level"
    val asset: String get() = if (kind == PieceKind.GENERATOR) "gen_${chain}_$level" else "ic_${chain}_$level"
}

@Serializable
data class GameState(
    val version: Int = 1,
    val day: Int = 1,
    val coins: Int = 0,
    val energy: Int = MAX_ENERGY,
    val energyAt: Long = 0,
    val board: List<Piece?> = List(BOARD_SIZE) { null },
    val warehouse: List<Piece> = emptyList(),
    val warehouseCapacity: Int = 6,
    val bubble: List<Piece> = emptyList(),
    val delivered: Set<String> = emptySet(),
    val builtDays: Set<Int> = emptySet(),
    val seenDialogue: Set<String> = emptySet(),
    val unlockedCats: Set<String> = emptySet(),
    val purchasedGenerators: Int = 0,
    val nextUid: Long = 1,
    val completed: Boolean = false,
    val tutorialDone: Boolean = false,
    val choices: Map<Int, String> = emptyMap(),
)

@Serializable
data class GameSettings(
    val soundVolume: Float = 0.7f,
    val haptics: Boolean = true,
    val reducedMotion: Boolean = false,
)

sealed interface GameAction {
    data class Spawn(val index: Int) : GameAction
    data class Move(val from: Int, val to: Int) : GameAction
    data class TakeBubble(val uid: Long, val target: Int? = null) : GameAction
    data class Store(val index: Int) : GameAction
    data class Retrieve(val uid: Long) : GameAction
    data class Deliver(val orderId: String) : GameAction
    data object Build : GameAction
    data object NextDay : GameAction
    data class Sell(val index: Int) : GameAction
    data object ExpandWarehouse : GameAction
    data class BuyGenerator(val chain: String) : GameAction
    data class SeenDialogue(val sceneId: String) : GameAction
    data class Choose(val sceneId: String) : GameAction
    data object TutorialComplete : GameAction
    data object DebugEnergy : GameAction
    data object DebugCoins : GameAction
    data class DebugJump(val day: Int) : GameAction
    data class DebugFillOrder(val orderId: String) : GameAction
}

enum class GameEffect { SPAWN, MERGE, DELIVER, BUILD, DAY }

data class GameResult(
    val state: GameState,
    val message: String? = null,
    val effect: GameEffect? = null,
)
