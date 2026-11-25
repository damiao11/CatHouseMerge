package com.example.cathousemerge.game

/** The board follows the nearest-to-completion order, not the sum of every order. */
data class ItemDemandFeedback(
    val orderId: String,
    val available: Int,
    val required: Int,
    val orderRemaining: Int,
) {
    val satisfied: Boolean get() = available >= required
}

internal fun GameState.boardItemCounts(): Map<String, Int> =
    board.asSequence().filterNotNull().filter { it.kind == PieceKind.ITEM }
        .groupingBy(Piece::itemId).eachCount()

internal fun OrderDef.progress(available: Map<String, Int>): List<Int> {
    val remaining = available.toMutableMap()
    return req.map { requirement ->
        val count = remaining[requirement.itemId] ?: 0
        val used = minOf(count, requirement.count)
        remaining[requirement.itemId] = count - used
        used
    }
}

fun GameEngine.itemDemandFeedback(state: GameState): Map<String, ItemDemandFeedback> {
    val available = state.boardItemCounts()
    return buildMap {
        for (order in content.ordersForDay(state.day)) {
            if (order.id in state.delivered) continue
            val progress = order.progress(available)
            val remaining = order.req.indices.sumOf { order.req[it].count - progress[it] }
            for ((itemId, requirements) in order.req.groupBy(Requirement::itemId)) {
                // Strict comparison keeps display order when two orders are equally close.
                val previous = get(itemId)
                if (previous == null || remaining < previous.orderRemaining) {
                    val required = requirements.sumOf(Requirement::count)
                    put(itemId, ItemDemandFeedback(order.id, (available[itemId] ?: 0).coerceAtMost(required), required, remaining))
                }
            }
        }
    }
}
