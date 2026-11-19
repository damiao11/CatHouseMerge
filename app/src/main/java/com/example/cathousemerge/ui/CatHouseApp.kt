package com.example.cathousemerge.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cathousemerge.BuildConfig
import com.example.cathousemerge.game.*
import kotlinx.coroutines.delay

// Success feedback is deliberately brief; informational/error messages remain longer.
internal fun gameMessageHoldMillis(effect: GameEffect?): Long =
    if (effect == GameEffect.DELIVER || effect == GameEffect.BUILD) 1200L else 2400L

@Composable
fun CatHouseApp(
    content: GameContent,
    state: GameState,
    settings: GameSettings,
    message: String?,
    messageId: Long,
    onAction: (GameAction) -> Unit,
    onSettings: (GameSettings) -> Unit,
    onReset: () -> Unit,
    onMessageShown: () -> Unit,
    messageEffect: GameEffect? = null,
) {
    CatTheme {
        var screen by rememberSaveable { mutableStateOf("welcome") }
        var started by rememberSaveable { mutableStateOf(false) }
        var showDebug by remember { mutableStateOf(false) }
        var dialogueId by remember { mutableStateOf<String?>(null) }
        var choicePending by remember { mutableStateOf(false) }
        var toastVisible by remember { mutableStateOf(false) }
        var toastText by remember { mutableStateOf("") }
        val latestOnAction by rememberUpdatedState(onAction)
        val latestMessageShown by rememberUpdatedState(onMessageShown)
        LaunchedEffect(messageId) {
            if (!message.isNullOrBlank()) {
                toastText = message
                toastVisible = true
                delay(gameMessageHoldMillis(messageEffect))
                toastVisible = false
                delay(if (settings.reducedMotion) 0 else 220)
                latestMessageShown()
            } else toastVisible = false
        }
        LaunchedEffect(started, state.day, state.delivered, state.builtDays, state.seenDialogue, state.choices) {
            if (!started) { dialogueId = null; choicePending = false; return@LaunchedEffect }
            val today = content.day(state.day)
            val intro = today.dialogue["intro"]?.takeIf { it !in state.seenDialogue }
            val catRequestDone = content.ordersForDay(state.day).any { it.type == "cat" && it.id in state.delivered }
            val catScene = content.dialogue.scenes.entries.firstOrNull {
                it.value.day == state.day && it.value.kind == "cat" && it.key !in state.seenDialogue && catRequestDone
            }?.key
            val built = state.day in state.builtDays
            val options = content.dialogue.scenes.filterValues { it.day == state.day && it.kind == "choice" }
            val needsChoice = built && options.isNotEmpty() && state.day !in state.choices
            val choice = state.choices[state.day]?.takeIf { built && it !in state.seenDialogue }
            val allDone = content.ordersForDay(state.day).all { it.id in state.delivered } && built
            val outro = today.dialogue["outro"]?.takeIf { allDone && !needsChoice && it !in state.seenDialogue }
            val next = intro ?: catScene ?: choice ?: outro
            if (next != null && built && intro == null && catScene == null) delay(if (settings.reducedMotion) 0 else 950)
            dialogueId = next
            choicePending = needsChoice && intro == null && catScene == null
        }
        BackHandler(enabled = started && (screen != "board" || dialogueId != null || showDebug)) {
            when {
                showDebug -> showDebug = false
                dialogueId != null -> { dialogueId?.let { onAction(GameAction.SeenDialogue(it)) }; dialogueId = null }
                else -> screen = "board"
            }
        }
        Surface(Modifier.fillMaxSize(), color = Paper) {
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                AnimatedContent(
                    targetState = screen,
                    transitionSpec = { fadeIn(tween(if (settings.reducedMotion) 0 else 180)) togetherWith fadeOut(tween(if (settings.reducedMotion) 0 else 120)) },
                    label = "screen",
                ) { route ->
                    when (route) {
                        "welcome" -> WelcomeScreen(state, settings.reducedMotion) { started = true; screen = if (state.completed) "summary" else "board" }
                        "build" -> BuildScreen(content, state, settings, onAction) { screen = it }
                        "warehouse" -> WarehouseScreen(content, state, onAction) { screen = "board" }
                        "almanac" -> AlmanacScreen(content, state) { screen = "board" }
                        "settings" -> SettingsScreen(settings, onSettings, {
                            dialogueId = null
                            started = false
                            screen = "welcome"
                            onReset()
                        }) { screen = "board" }
                        "summary" -> SummaryScreen(content, state, onAction) { screen = it }
                        else -> BoardScreen(content, state, settings, onAction, { screen = it }, { if (BuildConfig.DEBUG) showDebug = true })
                    }
                }
                dialogueId?.let { id ->
                    content.dialogue.scenes[id]?.let { scene ->
                        val room = content.day(state.day).room
                        val roomRepaired = content.days.filter { it.room == room }.all { it.day in state.builtDays }
                        DialogueOverlay(id, scene, room, roomRepaired, settings.reducedMotion) {
                            latestOnAction(GameAction.SeenDialogue(id))
                            dialogueId = null
                        }
                    }
                }
                if (choicePending && dialogueId == null) {
                    val options = content.dialogue.scenes.filterValues { it.day == state.day && it.kind == "choice" }
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text(if (state.day == 14) "厨房，刷成什么颜色？" else "你要走了吗？") },
                        text = { Text(if (state.day == 14) "煤球把刷子推到你面前，等你拿主意。" else "虎子抬起头，望着门口的你。") },
                        confirmButton = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                options.forEach { (id, _) ->
                                    TextButton({ onAction(GameAction.Choose(id)) }, Modifier.testTag("choice_$id")) {
                                        Text(when {
                                            id.endsWith("warm") -> "米白"
                                            id.endsWith("green") -> "豆绿"
                                            id.endsWith("stay") -> "留下来"
                                            else -> "再想想"
                                        })
                                    }
                                }
                            }
                        },
                    )
                }
                AnimatedVisibility(toastVisible,
                    Modifier.align(Alignment.Center).offset(y = (-32).dp).widthIn(max = 300.dp).fillMaxWidth().padding(horizontal = 16.dp),
                    enter = fadeIn(tween(if (settings.reducedMotion) 0 else 100)), exit = fadeOut(tween(if (settings.reducedMotion) 0 else 220))) {
                    // Passive feedback deliberately has no click/pointer handler, so it never blocks the game.
                    GamePanel(Modifier.testTag("game_toast").semantics { liveRegion = LiveRegionMode.Polite }) {
                        Text(toastText, Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), color = Ink,
                            style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    }
                }
                if (showDebug) DebugPanel(content, state, onAction) { showDebug = false }
            }
        }
    }
}
