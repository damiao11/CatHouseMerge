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

fun GameEngine.itemDemandFeedback(state: GameState): Map<String, ItemDemandFeedback> {
    val available = state.board.filterNotNull().filter { it.kind == PieceKind.ITEM }
        .groupingBy(Piece::itemId).eachCount()
    val active = content.ordersForDay(state.day).filter { it.id !in state.delivered }
    // Kotlin's stable sort retains the left-to-right order when remaining counts tie.
    val prioritized = active.map { order ->
        val progress = orderProgress(state, order)
        order to order.req.indices.sumOf { order.req[it].count - progress[it] }
    }.sortedBy { it.second }
    return buildMap {
        for ((order, remaining) in prioritized) {
            val requirements = order.req.groupBy(Requirement::itemId)
            for ((itemId, requirementsForItem) in requirements) {
                val required = requirementsForItem.sumOf(Requirement::count)
                if (itemId !in this) {
                    put(itemId, ItemDemandFeedback(order.id, (available[itemId] ?: 0).coerceAtMost(required), required, remaining))
                }
            }
        }
    }
}
