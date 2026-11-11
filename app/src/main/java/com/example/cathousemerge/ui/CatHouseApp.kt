package com.example.cathousemerge.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.cathousemerge.game.*
import kotlinx.coroutines.delay

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
) {
    CatTheme {
        var screen by rememberSaveable { mutableStateOf("welcome") }
        var started by rememberSaveable { mutableStateOf(false) }
        var showDebug by remember { mutableStateOf(false) }
        var dialogueId by remember { mutableStateOf<String?>(null) }
        val snackbar = remember { SnackbarHostState() }
        val latestOnAction by rememberUpdatedState(onAction)
        val latestMessageShown by rememberUpdatedState(onMessageShown)
        LaunchedEffect(messageId) {
            if (!message.isNullOrBlank()) {
                snackbar.showSnackbar(message, duration = SnackbarDuration.Short)
                latestMessageShown()
            }
        }
        LaunchedEffect(started, state.day, state.delivered, state.builtDays, state.seenDialogue) {
            if (!started) { dialogueId = null; return@LaunchedEffect }
            val today = content.day(state.day)
            val intro = today.dialogue["intro"]?.takeIf { it !in state.seenDialogue }
            val catRequestDone = content.ordersForDay(state.day).any { it.type == "cat" && it.id in state.delivered }
            val catScene = content.dialogue.scenes.entries.firstOrNull {
                it.value.day == state.day && it.value.kind == "cat" && it.key !in state.seenDialogue && catRequestDone
            }?.key
            val built = state.day in state.builtDays
            val choice = content.dialogue.scenes.entries.firstOrNull {
                it.value.day == state.day && it.value.kind == "choice" && it.key !in state.seenDialogue && built
            }?.key
            val allDone = content.ordersForDay(state.day).all { it.id in state.delivered } && built
            val outro = today.dialogue["outro"]?.takeIf { allDone && it !in state.seenDialogue }
            val next = intro ?: catScene ?: choice ?: outro
            if (next != null && built && intro == null && catScene == null) delay(if (settings.reducedMotion) 0 else 950)
            dialogueId = next
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
                        "welcome" -> WelcomeScreen(state) { started = true; screen = if (state.completed) "summary" else "board" }
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
                        else -> BoardScreen(content, state, settings, onAction, { screen = it }, { showDebug = true })
                    }
                }
                dialogueId?.let { id ->
                    content.dialogue.scenes[id]?.let { scene ->
                        DialogueOverlay(id, scene, settings.reducedMotion) {
                            latestOnAction(GameAction.SeenDialogue(id))
                            dialogueId = null
                        }
                    }
                }
                SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp)) { data ->
                    Snackbar(data, containerColor = Ink, contentColor = CardPaper, shape = RoundedCornerShape(16.dp))
                }
                if (showDebug) DebugPanel(content, state, onAction) { showDebug = false }
            }
        }
    }
}

@Composable
private fun DialogueOverlay(id: String, scene: DialogueScene, reducedMotion: Boolean, onFinish: () -> Unit) {
    val pages = remember(id) { scene.lines.flatMap { line -> line.text.chunked(32).map { line.copy(text = it) } } }
    var page by remember(id) { mutableIntStateOf(0) }
    val line = pages.getOrNull(page) ?: return
    var visible by remember(id, page) { mutableIntStateOf(if (reducedMotion) line.text.length else 0) }
    LaunchedEffect(id, page, reducedMotion) {
        if (reducedMotion) visible = line.text.length
        else while (visible < line.text.length) { delay(33); visible++ }
    }
    val advance = {
        if (visible < line.text.length) visible = line.text.length
        else if (page < pages.lastIndex) page++
        else onFinish()
    }
    Box(Modifier.fillMaxSize().background(Ink.copy(alpha = 0.45f)).clickable(onClick = advance).testTag("dialogue_next")) {
        TextButton(onFinish, Modifier.align(Alignment.TopEnd).padding(12.dp).testTag("dialogue_skip")) { Text("略过这段", color = CardPaper) }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 20.dp, vertical = 28.dp)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (line.who != "narrator" && line.who != "player") CatAvatar(line.who, Modifier.size(72.dp))
                Surface(color = Paper, shape = RoundedCornerShape(12.dp)) {
                    Text(catName(line.who), Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.titleSmall, color = DarkWood)
                }
            }
            Spacer(Modifier.height(10.dp))
            Surface(Modifier.fillMaxWidth(), color = CardPaper, shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text(line.text.take(visible), Modifier.fillMaxWidth().heightIn(min = 58.dp), style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(12.dp))
                    Text(if (visible < line.text.length) "点一下，看完这句" else if (page == pages.lastIndex) "点一下，继续修缮" else "点一下，继续听", Modifier.align(Alignment.End), style = MaterialTheme.typography.bodySmall, color = Muted)
                }
            }
        }
    }
}
