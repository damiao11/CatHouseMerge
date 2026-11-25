package com.example.cathousemerge.game

import kotlin.math.PI
import kotlin.math.cos

/** Monotonic milliseconds supplied by the UI make idle timing independently testable. */
enum class GeneratorHintPhase { WAITING, BADGE, PULSE, NO_ENERGY }

data class GeneratorHint(val uid: Long, val phase: GeneratorHintPhase, val scale: Float = 1f, val badgeAlpha: Float = 1f)

object GeneratorHints {
    const val IDLE_MS = 3_000L
    const val COOLDOWN_MS = 10_000L
    const val CYCLE_MS = 4_000L

    fun mainChain(content: GameContent, day: Int): String? {
        val room = content.day(day).room
        return content.days.firstOrNull { it.room == room && it.newGenerator != null }
            ?.newGenerator?.removePrefix("gen_")
    }

    fun at(
        board: List<Piece?>,
        mainChain: String?,
        energy: Int,
        now: Long,
        lastInteractionAt: Long,
        tappedAt: Map<Long, Long>,
        reducedMotion: Boolean = false,
    ): GeneratorHint? {
        val candidate = board.asSequence().filterNotNull().filter { piece ->
            piece.kind == PieceKind.GENERATOR && tappedAt[piece.uid]?.let { now - it >= COOLDOWN_MS } != false
        }.maxWithOrNull(compareBy<Piece> { it.chain == mainChain }.thenBy { it.level })
            ?: return null
        val idle = (now - lastInteractionAt).coerceAtLeast(0)
        if (idle < IDLE_MS) {
            // A single, barely perceptible idle breath; never combined with the badge.
            val breath = if (reducedMotion || energy <= 0) 1f else 1f + (0.01 * (1 - cos(2 * PI * idle / CYCLE_MS))).toFloat()
            return GeneratorHint(candidate.uid, GeneratorHintPhase.WAITING, breath)
        }
        if (energy <= 0) return GeneratorHint(candidate.uid, GeneratorHintPhase.NO_ENERGY)
        if (reducedMotion) return GeneratorHint(candidate.uid, GeneratorHintPhase.BADGE)
        val cycle = (idle - IDLE_MS) % CYCLE_MS
        if (cycle < 2_000) {
            val alpha = if (cycle > 1_800) (2_000 - cycle) / 200f else 1f
            return GeneratorHint(candidate.uid, GeneratorHintPhase.BADGE, badgeAlpha = alpha)
        }
        val pulseTime = cycle - 2_000
        val scale = if (pulseTime <= 600) 1f + (0.03 * (1 - cos(2 * PI * pulseTime / 600))).toFloat() else 1f
        return GeneratorHint(candidate.uid, GeneratorHintPhase.PULSE, scale)
    }
}
