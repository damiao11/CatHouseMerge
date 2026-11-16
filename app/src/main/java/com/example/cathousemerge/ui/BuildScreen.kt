package com.example.cathousemerge.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.example.cathousemerge.R
import com.example.cathousemerge.game.*

@Composable
internal fun BuildScreen(content: GameContent, state: GameState, settings: GameSettings, onAction: (GameAction) -> Unit, onNavigate: (String) -> Unit) {
    val today = content.day(state.day)
    val done = state.day in state.builtDays
    val roomDays = content.days.filter { it.room == today.room }
    val repaired = roomDays.count { it.day in state.builtDays }
    val roomDone = roomDays.all { it.day in state.builtDays }
    val reveal = remember(today.room) { Animatable(if (roomDone) 1f else 0f) }
    LaunchedEffect(roomDone, settings.reducedMotion) {
        reveal.animateTo(if (roomDone) 1f else 0f, tween(if (settings.reducedMotion) 0 else 900))
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(Paper).testTag("build_screen")) {
        val sheetMaxHeight = maxHeight * 0.55f
        // Scene has its own viewport: the task sheet no longer hides the entire porch floor.
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.68f).testTag("repair_scene")) {
            SceneImage("rooms/room_${today.room}_before.webp", "${roomName(today.room)}修缮前", Modifier.fillMaxSize(), alignment = Alignment.TopCenter)
            SceneImage("rooms/room_${today.room}_after.webp", "${roomName(today.room)}修缮后",
                Modifier.fillMaxSize().drawWithContent { clipRect(right = size.width * reveal.value) { this@drawWithContent.drawContent() } }, alignment = Alignment.TopCenter)
            if (today.room == "kitchen" && state.choices[14] == "d14_choice_green") {
                Box(Modifier.fillMaxSize().background(Color(0xFF7FA05A).copy(alpha = 0.12f)))
            }
        }
        if (today.room == "porch" && today.build.step == 3 && !done) {
            // Anchor to the source artwork's empty hook; this is a task marker, not a fake repaired scene.
            Canvas(Modifier.offset(x = maxWidth * (720f / 1080f) - 18.dp, y = maxWidth * (330f / 1080f) - 18.dp)
                .size(36.dp).testTag("repair_target").semantics { contentDescription = "当前修缮位置：${today.build.name}" }) {
                drawCircle(Color(0xFFE2A343), style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))))
            }
        }
        Row(Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Surface(onClick = { onNavigate("board") }, modifier = Modifier.testTag("build_back"), shape = CircleShape, color = CardPaper.copy(alpha = 0.96f)) {
                Row(Modifier.heightIn(min = 44.dp).padding(start = 12.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回棋盘", Modifier.size(20.dp), tint = Ink)
                    Text(roomName(today.room), Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleMedium)
                }
            }
            JournalCoins(state.coins)
        }
        Surface(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().heightIn(max = sheetMaxHeight),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), color = Paper,
        ) {
            Box {
                JournalPaper(Modifier.matchParentSize())
                Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (roomDone) "这间修好了" else "今日修缮", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                        Text("$repaired/${roomDays.size}", Modifier.testTag("repair_progress"), color = JournalWoodInk, style = MaterialTheme.typography.titleMedium)
                    }
                    LinearProgressIndicator(
                        progress = { repaired.toFloat() / roomDays.size.coerceAtLeast(1) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).testTag("repair_progress_bar").semantics { stateDescription = "已修复 $repaired/${roomDays.size}" },
                        color = Leaf, trackColor = Color(0xFFE8D9BD), gapSize = 0.dp, drawStopIndicator = {},
                    )
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        roomDays.filter { it.day != today.day || done }.forEach { day ->
                            val finished = day.day in state.builtDays
                            Row(Modifier.fillMaxWidth().heightIn(min = 26.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(if (finished) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, if (finished) "已完成" else "待修缮", Modifier.size(20.dp), tint = if (finished) Leaf else JournalMuted)
                                Text(day.build.name, style = MaterialTheme.typography.bodyMedium, color = if (finished) Ink else JournalMuted)
                            }
                        }
                        if (!done) {
                            Surface(Modifier.fillMaxWidth().testTag("repair_current_task"), shape = RoundedCornerShape(14.dp), color = Color(0xFFF0E1C8)) {
                                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Image(painterResource(R.drawable.ui_repair_brush), null, Modifier.size(width = 58.dp, height = 48.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(today.build.name, style = MaterialTheme.typography.titleMedium)
                                        Text(if (today.room == "porch" && today.build.step == 3) "让门廊重新亮起来" else "慢慢修好，留住家的温度", style = MaterialTheme.typography.bodySmall, color = JournalMuted)
                                    }
                                }
                            }
                        }
                    }
                    val affordable = state.coins >= today.build.cost
                    MainButton(
                        when {
                            done -> "返回棋盘"
                            !affordable -> "还差 ${today.build.cost - state.coins} 金币 · 去合成"
                            else -> "修好这一步 · ${today.build.cost} 金币"
                        },
                        onClick = { if (done || !affordable) onNavigate("board") else onAction(GameAction.Build) },
                        modifier = Modifier.fillMaxWidth().testTag("build_pay"),
                    )
                    if (done) Text("这一小角，重新有了家的样子。", style = MaterialTheme.typography.bodySmall, color = Leaf)
                }
            }
        }
    }
}
