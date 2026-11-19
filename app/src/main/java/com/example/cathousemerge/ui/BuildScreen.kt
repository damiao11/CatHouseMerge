package com.example.cathousemerge.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cathousemerge.game.*

@Composable
internal fun BuildScreen(content: GameContent, state: GameState, settings: GameSettings, onAction: (GameAction) -> Unit, onNavigate: (String) -> Unit) {
    val today = content.day(state.day)
    val done = state.day in state.builtDays
    val roomDays = content.days.filter { it.room == today.room }
    val repaired = roomDays.count { it.day in state.builtDays }
    val roomDone = roomDays.all { it.day in state.builtDays }
    var showBefore by remember(today.room, roomDone) { mutableStateOf(false) }
    val showAfter = roomDone && !showBefore
    val affordable = state.coins >= today.build.cost
    BoxWithConstraints(Modifier.fillMaxSize().testTag("build_screen")) {
        GameBackground(Modifier.matchParentSize())
        val sceneWidth = maxWidth - 24.dp
        val scale = LocalDensity.current.fontScale
        val sheetMaxHeight = maxHeight * (if (scale > 1.2f) .61f else .51f)
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                GameSquareButton(Icons.AutoMirrored.Outlined.ArrowBack, "返回棋盘", Modifier.width(42.dp).height(48.dp).testTag("build_back")) { onNavigate("board") }
                Column(Modifier.weight(1f)) {
                    Text(roomName(today.room), fontSize = 18.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold)
                    Text("第${state.day}天 · 给旧屋一点新意", fontSize = 10.sp, lineHeight = 15.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                GamePanel(radius = 24.dp) { GameCoin(state.coins, Modifier.padding(horizontal = 9.dp, vertical = 7.dp)) }
            }
            GamePanel(Modifier.fillMaxWidth().weight(1f).testTag("repair_scene"), radius = 20.dp) {
                SceneImage("rooms/room_${today.room}_${if (showAfter) "after" else "before"}.webp", "${roomName(today.room)}修缮${if (showAfter) "后" else "前"}",
                    Modifier.fillMaxSize().testTag(if (showAfter) "repair_scene_after" else "repair_scene_before"), alignment = Alignment.TopCenter)
                Surface(Modifier.align(Alignment.TopStart).padding(9.dp), shape = RoundedCornerShape(8.dp), color = CardPaper.copy(alpha = .96f)) {
                    Text(if (showAfter) "${roomName(today.room)}已修好" else "修缮前", Modifier.padding(horizontal = 8.dp, vertical = 5.dp).testTag("repair_scene_label"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Ink)
                }
                if (today.room == "porch" && today.build.step == 3 && !done) {
                    Canvas(Modifier.align(Alignment.TopStart).offset(x = sceneWidth * .62f, y = sceneWidth * .24f).size(28.dp).testTag("repair_target")
                        .semantics { contentDescription = "当前修缮位置：${today.build.name}" }) {
                        drawCircle(SelectionYellow, style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))))
                    }
                }
                Surface(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(8.dp), shape = RoundedCornerShape(8.dp), color = CardPaper.copy(alpha = .94f)) {
                    Text(if (showAfter) "每个角落，都有了家的样子。" else "老屋，等你慢慢修好。", Modifier.padding(horizontal = 9.dp, vertical = 6.dp), fontSize = 10.sp, lineHeight = 14.sp, color = Muted)
                }
            }
            GamePanel(Modifier.fillMaxWidth().heightIn(max = sheetMaxHeight).testTag("repair_panel"), radius = 20.dp) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${roomName(today.room)}焕新计划", Modifier.weight(1f), fontSize = 18.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("$repaired/${roomDays.size}", Modifier.background(DemandSatisfied, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp).testTag("repair_progress")
                            .semantics { stateDescription = "${roomName(today.room)}已完成${repaired}步，共${roomDays.size}步" }, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, color = Leaf)
                    }
                    LinearProgressIndicator(progress = { repaired.toFloat() / roomDays.size.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth().height(7.dp), color = Color(0xFF93BC56), trackColor = Color(0xFFE9E6CE))
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        roomDays.filter { it.day != state.day }.forEach { step ->
                            val finished = step.day in state.builtDays
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(if (finished) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, if (finished) "已完成" else "待修缮", Modifier.size(16.dp), tint = if (finished) Leaf else Muted)
                                Text(step.build.name, fontSize = 11.sp, lineHeight = 16.sp, color = if (finished) Leaf else Muted, maxLines = 2)
                            }
                        }
                        Surface(Modifier.fillMaxWidth().testTag(if (done) "repair_finished_task" else "repair_current_task"), shape = RoundedCornerShape(12.dp), color = if (done) DemandSatisfied else Color(0xFFFFF0C7)) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                Icon(if (today.room == "porch" && today.build.step == 3) Icons.Outlined.Lightbulb else Icons.Outlined.Handyman, null, Modifier.size(30.dp), tint = DarkWood)
                                Column(Modifier.weight(1f)) {
                                    Text(if (done) "${today.build.name}，完成了" else today.build.name, fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold)
                                    Text(when { roomDone -> "这间修好了，下一段故事等你开启。"; done -> "今天又修好一角，离新家更近了。"; !affordable -> "还差 ${today.build.cost - state.coins} 金币，完成订单后再来。"; else -> "慢慢修好，留住家的温度。" }, fontSize = 11.sp, lineHeight = 17.sp, color = Muted)
                                }
                            }
                        }
                    }
                    GamePrimaryButton(when { done -> "返回合成"; !affordable -> "去合成赚金币"; else -> "修缮 · ${today.build.cost}金币" },
                        { if (done || !affordable) onNavigate("board") else onAction(GameAction.Build) }, Modifier.fillMaxWidth().testTag("build_pay"), done = done)
                    TextButton({ if (roomDone) showBefore = !showBefore else onNavigate("board") }, Modifier.fillMaxWidth().heightIn(min = 36.dp).testTag("repair_compare")) {
                        Text(if (roomDone) { if (showBefore) "重新看看修缮后" else "重新看看修缮前" } else "返回合成，整理材料", fontSize = 12.sp, lineHeight = 17.sp, color = Muted)
                    }
                }
            }
        }
    }
}
