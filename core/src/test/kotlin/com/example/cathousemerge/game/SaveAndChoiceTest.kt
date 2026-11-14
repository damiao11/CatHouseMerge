package com.example.cathousemerge.game

import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class SaveAndChoiceTest {
    private val content = ContentParser.parse { File("docs/data", it).readText() }
    private val engine = GameEngine(content)
    private val now = 1_000_000L

    @Test
    fun choicePersistsAndOnlyTheSelectedBranchRemainsPlayable() {
        val state = engine.reduce(engine.newGame(now), GameAction.DebugJump(14), now).state
        val built = engine.reduce(state, GameAction.Build, now).state
        val selected = engine.reduce(built, GameAction.Choose("d14_choice_green"), now).state
        assertEquals("d14_choice_green", selected.choices[14])
        assertTrue("d14_choice_warm" in selected.seenDialogue)
        assertFalse("d14_choice_green" in selected.seenDialogue)
        assertEquals(selected, engine.reduce(selected, GameAction.Choose("d14_choice_warm"), now).state)
        val restored = Json.decodeFromString<GameState>(Json.encodeToString(selected))
        assertEquals(selected, restored)
        assertEquals(selected.board, restored.board)
        assertEquals(selected.bubble, restored.bubble)
    }

    @Test
    fun choicesMustBelongToTodayAndFollowTheBuildStep() {
        val state = engine.newGame(now)
        assertEquals(state, engine.reduce(state, GameAction.Choose("d14_choice_green"), now).state)
        val fourteenth = engine.reduce(state, GameAction.DebugJump(14), now).state
        assertEquals(fourteenth, engine.reduce(fourteenth, GameAction.Choose("d14_choice_green"), now).state)
    }

    @Test
    fun existingVersionOneSavesDefaultToNoChoices() {
        val original = engine.newGame(now)
        val encoded = Json.encodeToString(original)
        assertFalse(encoded.contains("choices"))
        assertEquals(emptyMap<Int, String>(), Json.decodeFromString<GameState>(encoded).choices)
    }
}
