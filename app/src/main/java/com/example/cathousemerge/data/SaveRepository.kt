package com.example.cathousemerge.data

import android.content.Context
import android.util.AtomicFile
import com.example.cathousemerge.game.BOARD_SIZE
import com.example.cathousemerge.game.GameContent
import com.example.cathousemerge.game.GameState
import com.example.cathousemerge.game.MAX_ENERGY
import com.example.cathousemerge.game.PieceKind
import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class LoadedSave(val state: GameState?, val recoveredFromDamage: Boolean = false)

class SaveRepository(context: Context, private val content: GameContent) {
    private val file = AtomicFile(File(context.filesDir, "save.json"))
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Synchronized
    fun load(): LoadedSave {
        if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) {
            if (File(file.baseFile.path + ".new").exists()) {
                archiveDamagedFiles()
                return LoadedSave(null, recoveredFromDamage = true)
            }
            return LoadedSave(null)
        }
        // IO errors keep the original files in place. Only decoded, invalid data starts a new game.
        val text = file.openRead().bufferedReader().use { it.readText() }
        return try {
            val state = json.decodeFromString<GameState>(text)
            validate(state)
            LoadedSave(state)
        } catch (error: Exception) {
            archiveDamagedFiles()
            LoadedSave(null, recoveredFromDamage = true)
        }
    }

    @Synchronized
    fun save(state: GameState) {
        validate(state)
        val stream = file.startWrite()
        try {
            stream.write(json.encodeToString(state).toByteArray(Charsets.UTF_8))
            file.finishWrite(stream)
        } catch (error: Exception) {
            file.failWrite(stream)
            throw error
        }
    }

    @Synchronized
    fun saveLatest(currentState: () -> GameState) {
        save(currentState())
    }

    private fun archiveDamagedFiles() {
        val timestamp = System.currentTimeMillis()
        listOf("", ".bak", ".new").forEachIndexed { index, suffix ->
            val original = File(file.baseFile.path + suffix)
            if (original.exists()) {
                original.copyTo(File(file.baseFile.parentFile, "save_broken_${timestamp}_$index.json"))
            }
        }
        // Delete only after every existing copy has been preserved successfully.
        file.delete()
    }

    private fun validate(state: GameState) {
        require(state.version == 1) { "不支持的存档版本" }
        require(state.day in 1..content.days.size && state.coins >= 0)
        require(state.energy in 0..MAX_ENERGY && state.energyAt >= 0)
        require(state.board.size == BOARD_SIZE && state.warehouseCapacity in 6..12)
        require(state.warehouse.size <= state.warehouseCapacity)
        require(state.delivered.all { id -> content.orderById[id]?.day?.let { it <= state.day } == true })
        require(state.builtDays.all { it in 1..state.day })
        require(state.seenDialogue.all { it in content.dialogue.scenes })
        require(state.unlockedCats.all { id -> content.dialogue.almanac.any { it.cat == id } })
        require(state.purchasedGenerators >= 0)
        require(state.choices.all { (day, sceneId) ->
            day in 1..state.day && content.dialogue.scenes[sceneId]?.let { it.day == day && it.kind == "choice" } == true
        })
        val pieces = state.board.filterNotNull() + state.warehouse + state.bubble
        require(pieces.map { it.uid }.distinct().size == pieces.size)
        require(pieces.all { piece ->
            val chain = content.chainById[piece.chain]
            piece.uid > 0 && chain != null && chain.unlockDay <= state.day &&
                if (piece.kind == PieceKind.GENERATOR) piece.level in 1..5
                else piece.itemId in content.itemById
        })
        require(state.bubble.all { it.kind == PieceKind.GENERATOR })
        require(state.nextUid > (pieces.maxOfOrNull { it.uid } ?: 0))
        require(!state.completed || (state.day == content.days.size && state.day in state.builtDays &&
            content.ordersForDay(state.day).all { it.id in state.delivered }))
    }
}
