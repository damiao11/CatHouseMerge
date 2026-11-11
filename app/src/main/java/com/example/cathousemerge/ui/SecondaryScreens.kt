package com.example.cathousemerge.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cathousemerge.game.*

@Composable
internal fun WelcomeScreen(state: GameState, onStart: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        SceneImage("scenes/scene_splash.webp", "黄昏里的老屋与猫", Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Ink.copy(alpha = 0.88f)), startY = 300f)))
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 28.dp, vertical = 36.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("猫 屋 合 合", style = MaterialTheme.typography.headlineLarge, color = CardPaper, fontSize = 38.sp)
            Text("一栋旧屋，六只猫。\n把日子，一点点修回来。", style = MaterialTheme.typography.bodyLarge, color = CardPaper.copy(alpha = 0.85f))
            Spacer(Modifier.height(12.dp))
            MainButton(
                if (state.day == 1 && state.delivered.isEmpty() && state.board.all { it == null }) "推开猫屋的门" else "继续第 ${state.day} 天",
                onClick = onStart, modifier = Modifier.fillMaxWidth().testTag("start_game"),
            )
        }
    }
}

@Composable
internal fun BuildScreen(content: GameContent, state: GameState, settings: GameSettings, onAction: (GameAction) -> Unit, onNavigate: (String) -> Unit) {
    val today = content.day(state.day)
    val done = state.day in state.builtDays
    val roomDays = content.days.filter { it.room == today.room }
    val roomDone = roomDays.all { it.day in state.builtDays }
    val reveal = remember(today.room) { Animatable(if (roomDone) 1f else 0f) }
    LaunchedEffect(roomDone, settings.reducedMotion) { reveal.animateTo(if (roomDone) 1f else 0f, tween(if (settings.reducedMotion) 0 else 900)) }
    Box(Modifier.fillMaxSize()) {
        SceneImage("rooms/room_${today.room}_before.webp", "${roomName(today.room)}修缮前", Modifier.fillMaxSize())
        SceneImage("rooms/room_${today.room}_after.webp", "${roomName(today.room)}修缮后", Modifier.fillMaxSize().drawWithContent { clipRect(right = size.width * reveal.value) { this@drawWithContent.drawContent() } })
        Column(Modifier.fillMaxWidth().align(Alignment.TopCenter).background(Ink.copy(alpha = 0.65f))) {
            Row(Modifier.fillMaxWidth().height(56.dp).padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton({ onNavigate("board") }) { Icon(Icons.Outlined.ArrowBack, "返回棋盘", tint = CardPaper) }
                Text(roomName(today.room), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = CardPaper)
                CoinAmount(state.coins, light = true)
            }
        }
        Surface(Modifier.align(Alignment.BottomCenter).fillMaxWidth(), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), color = Paper.copy(alpha = 0.98f)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (roomDone) "这间修好了" else "一天，修好一小步", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    Text("${roomDays.count { it.day in state.builtDays }} / 3", color = DarkWood, style = MaterialTheme.typography.titleMedium)
                }
                roomDays.forEach { day ->
                    val finished = day.day in state.builtDays
                    val active = day.day == state.day
                    Row(Modifier.fillMaxWidth().alpha(if (finished || active) 1f else 0.4f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(if (finished) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, if (finished) "已完成" else null, Modifier.size(20.dp), tint = if (finished) Leaf else DarkWood)
                        Text(day.build.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        if (!finished) CoinAmount(day.build.cost)
                    }
                }
                Spacer(Modifier.height(4.dp))
                MainButton(
                    when {
                        done -> "返回棋盘"
                        state.coins < today.build.cost -> "还差 ${today.build.cost - state.coins} 金币"
                        else -> "修好这一步 · ${today.build.cost} 金币"
                    },
                    onClick = { if (done) onNavigate("board") else onAction(GameAction.Build) },
                    modifier = Modifier.fillMaxWidth().testTag("build_pay"),
                    enabled = done || state.coins >= today.build.cost,
                )
                if (done) Text("这一小角，重新有了家的样子。", style = MaterialTheme.typography.bodySmall, color = Leaf)
            }
        }
    }
}

@Composable
internal fun SummaryScreen(content: GameContent, state: GameState, onAction: (GameAction) -> Unit, onNavigate: (String) -> Unit) {
    val today = content.day(state.day)
    val orders = content.ordersForDay(state.day)
    val scene = when (state.day) { 6 -> "door"; 12 -> "lights"; 18 -> "garden"; 24 -> "ending"; else -> null }
    Box(Modifier.fillMaxSize()) {
        SceneImage(if (scene == null) "rooms/room_${today.room}_${if (state.day % 3 == 0) "after" else "before"}.webp" else "scenes/scene_$scene.webp", "今天的猫屋", Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Ink.copy(alpha = 0.68f)))
        Column(Modifier.fillMaxSize().padding(28.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
            Icon(Icons.Outlined.WbTwilight, null, Modifier.size(40.dp), tint = Wood)
            Spacer(Modifier.height(20.dp))
            Text(if (state.completed) "灯亮了，猫也都在。" else "第 ${state.day} 天，收工。", style = MaterialTheme.typography.headlineMedium, color = CardPaper)
            Spacer(Modifier.height(12.dp))
            Text(if (state.day == 24) "旧屋重新成了家。\n那些被留下的日子，也终于拼在了一起。" else "今天的努力，已经留在这栋房子里。", style = MaterialTheme.typography.bodyLarge, color = CardPaper.copy(alpha = 0.8f))
            Spacer(Modifier.height(40.dp))
            SummaryRow("完成订单", "${orders.count { it.id in state.delivered }} / ${orders.size}")
            SummaryRow("订单收入", "${orders.filter { it.id in state.delivered }.sumOf(OrderDef::reward)} 金币")
            SummaryRow("今天的修缮", "${roomName(today.room)} · 第 ${today.build.step} 步")
            Spacer(Modifier.height(40.dp))
            if (!state.completed) {
                val ready = GameEngine(content).canAdvance(state)
                MainButton(
                    if (state.day == 24) "留住家的样子" else "迎接第 ${state.day + 1} 天",
                    onClick = { onAction(GameAction.NextDay); if (state.day < 24) onNavigate("board") },
                    modifier = Modifier.fillMaxWidth().testTag("next_day"), enabled = ready,
                )
                if (!ready) Text("完成今天的订单和建造后，就可以迎接明天。", Modifier.padding(top = 12.dp), color = CardPaper, style = MaterialTheme.typography.bodySmall)
            } else {
                MainButton("看看陪伴你的猫", onClick = { onNavigate("almanac") }, modifier = Modifier.fillMaxWidth())
            }
            TextButton({ onNavigate("board") }, Modifier.fillMaxWidth()) { Text("回到棋盘", color = CardPaper) }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = CardPaper.copy(alpha = 0.7f))
        Text(value, style = MaterialTheme.typography.titleMedium, color = CardPaper)
    }
}

@Composable
internal fun WarehouseScreen(content: GameContent, state: GameState, onAction: (GameAction) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        PageHeader("仓库", onBack) { Text("${state.warehouse.size} / ${state.warehouseCapacity}", style = MaterialTheme.typography.titleSmall) }
        if (state.day < 4) {
            Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Outlined.Inventory2, null, Modifier.size(56.dp), tint = DarkWood)
                Text("第 4 天，打开储物间", Modifier.padding(top = 24.dp), style = MaterialTheme.typography.titleLarge)
                Text("修好门廊后，会有 6 个免费格子。\n物品和工具都能先放在这里。", Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyMedium, color = Muted)
            }
            return
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("点存放的物品取回棋盘。\n交订单时，要先把需要的物品取出来。", style = MaterialTheme.typography.bodyMedium, color = Muted)
            repeat(3) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(4) { col ->
                        val index = row * 4 + col
                        val piece = state.warehouse.getOrNull(index)
                        val unlocked = index < state.warehouseCapacity
                        Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(14.dp)).background(if (unlocked) CardPaper else Color(0xFFF0E8DA)).border(1.dp, Line, RoundedCornerShape(14.dp)).clickable(enabled = piece != null || index == state.warehouseCapacity) {
                            if (piece != null) onAction(GameAction.Retrieve(piece.uid)) else onAction(GameAction.ExpandWarehouse)
                        }.testTag("warehouse_slot_$index"), contentAlignment = Alignment.Center) {
                            if (piece != null) GameImage(piece.asset, "取出${pieceName(piece, content)}", Modifier.fillMaxSize().padding(8.dp))
                            else if (!unlocked) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Outlined.Lock, "未解锁", Modifier.size(18.dp), tint = Muted)
                                    if (index == state.warehouseCapacity) Text(warehousePrice(state.warehouseCapacity).toString(), style = MaterialTheme.typography.labelMedium, color = Muted)
                                }
                            }
                        }
                    }
                }
            }
            if (state.warehouseCapacity < 12) MainButton("再添一格 · ${warehousePrice(state.warehouseCapacity)} 金币", onClick = { onAction(GameAction.ExpandWarehouse) }, modifier = Modifier.fillMaxWidth().testTag("warehouse_expand"), enabled = state.coins >= warehousePrice(state.warehouseCapacity))
            else Text("每件东西，都有了自己的位置。", style = MaterialTheme.typography.bodyMedium, color = Leaf)
            HorizontalDivider(color = Line)
            Text("添置工具", style = MaterialTheme.typography.titleLarge)
            Text("新工具进入订单行最前面的气泡。\n两个同级工具可以合成升级。", style = MaterialTheme.typography.bodySmall, color = Muted)
            content.chains.filter { it.unlockDay <= state.day }.forEach { chain ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    GameImage("gen_${chain.id}_1", null, Modifier.size(48.dp))
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text("${chain.name}工具台", style = MaterialTheme.typography.titleSmall)
                        Text("1 级", style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                    OutlinedButton({ onAction(GameAction.BuyGenerator(chain.id)) }, enabled = state.coins >= 900 + state.purchasedGenerators * 180) {
                        Text((900 + state.purchasedGenerators * 180).toString())
                    }
                }
            }
        }
    }
}

private fun warehousePrice(capacity: Int): Int = listOf(450, 630, 885, 1235, 1730, 2420).getOrElse(capacity - 6) { 0 }

@Composable
internal fun AlmanacScreen(content: GameContent, state: GameState, onBack: () -> Unit) {
    var selectedCat by remember { mutableStateOf<String?>(null) }
    var letter by remember { mutableStateOf<Int?>(null) }
    val known = state.unlockedCats
    Column(Modifier.fillMaxSize()) {
        PageHeader("住在这里的猫", onBack) { Text("${known.size} / 6", style = MaterialTheme.typography.titleSmall) }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("慢慢靠近，慢慢熟悉。", style = MaterialTheme.typography.bodyMedium, color = Muted)
            listOf("daju", "huahua", "tuanzi", "meiqiu", "ali", "huzi").chunked(3).forEach { cats ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    cats.forEach { cat ->
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable { selectedCat = cat }.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            CatAvatar(cat, Modifier.fillMaxWidth().aspectRatio(1f).alpha(if (cat in known) 1f else 0.25f))
                            Text(if (cat in known) catName(cat) else "还没遇见", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleSmall, color = if (cat in known) Ink else Muted)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Line)
            Text("外婆留下的信", style = MaterialTheme.typography.titleLarge)
            content.dialogue.letters.forEachIndexed { index, item ->
                val unlocked = state.day >= listOf(6, 12, 21)[index] && (state.day > listOf(6, 12, 21)[index] || state.day in state.builtDays)
                ListItem(
                    headlineContent = { Text(if (unlocked) item.title else "一封还没找到的信") },
                    leadingContent = { Icon(if (unlocked) Icons.Outlined.MailOutline else Icons.Outlined.Lock, null, tint = DarkWood) },
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(enabled = unlocked) { letter = index },
                    colors = ListItemDefaults.colors(containerColor = CardPaper),
                )
            }
        }
    }
    selectedCat?.let { cat ->
        val entry = content.dialogue.almanac.firstOrNull { it.cat == cat }
        val requestDone = content.orders.any { it.type == "cat" && catForDay(it.day) == cat && it.id in state.delivered }
        AlertDialog(
            onDismissRequest = { selectedCat = null },
            icon = { CatAvatar(cat, Modifier.size(112.dp)) },
            title = { Text(catName(cat), style = MaterialTheme.typography.titleLarge) },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) {
                if (requestDone || state.completed) {
                    entry?.let { Text(it.title, style = MaterialTheme.typography.titleSmall); Spacer(Modifier.height(12.dp)); Text(it.text, style = MaterialTheme.typography.bodyMedium) }
                } else Text(if (cat in known) "完成它的请求后，就能读到它的故事。" else "修好更多房间，也许它就会愿意靠近。")
            } },
            confirmButton = { TextButton({ selectedCat = null }) { Text("慢慢来") } },
        )
    }
    letter?.let { index ->
        val item = content.dialogue.letters[index]
        AlertDialog(onDismissRequest = { letter = null }, title = { Text(item.title, style = MaterialTheme.typography.titleLarge) }, text = { Text(item.text, Modifier.verticalScroll(rememberScrollState()), style = MaterialTheme.typography.bodyLarge) }, confirmButton = { TextButton({ letter = null }) { Text("收好这封信") } })
    }
}

@Composable
internal fun SettingsScreen(settings: GameSettings, onSettings: (GameSettings) -> Unit, onReset: () -> Unit, onBack: () -> Unit) {
    var confirmReset by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        PageHeader("设置", onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Text("声音", style = MaterialTheme.typography.titleLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("音效音量")
                Text("${(settings.soundVolume * 100).toInt()}%", color = Muted)
            }
            Slider(settings.soundVolume, { onSettings(settings.copy(soundVolume = it)) }, Modifier.testTag("sound_volume"))
            HorizontalDivider(color = Line)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("触感反馈", style = MaterialTheme.typography.titleMedium); Text("合成与交付时轻轻提醒", style = MaterialTheme.typography.bodySmall, color = Muted) }
                Switch(settings.haptics, { onSettings(settings.copy(haptics = it)) }, Modifier.testTag("haptics_toggle"))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("减少动态效果", style = MaterialTheme.typography.titleMedium); Text("缩短转场，关闭打字机效果", style = MaterialTheme.typography.bodySmall, color = Muted) }
                Switch(settings.reducedMotion, { onSettings(settings.copy(reducedMotion = it)) }, Modifier.testTag("motion_toggle"))
            }
            HorizontalDivider(color = Line)
            Text("进度会自动保存在这台设备上。", style = MaterialTheme.typography.bodyMedium, color = Muted)
            OutlinedButton({ confirmReset = true }, Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("reset_game")) {
                Icon(Icons.Outlined.RestartAlt, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp)); Text("重新开始")
            }
            Spacer(Modifier.height(16.dp))
            Text("猫屋合合", style = MaterialTheme.typography.titleLarge)
            Text("合成一件家具，修好一段时光。\n单机游玩 · 24 天的旧屋故事\nv0.1.0", style = MaterialTheme.typography.bodySmall, color = Muted)
        }
    }
    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text("从第一天重新开始？") },
        text = { Text("当前的棋盘、订单和修缮进度会被清除。") },
        confirmButton = { TextButton({ confirmReset = false; onReset() }, Modifier.testTag("confirm_reset")) { Text("重新开始", color = ErrorInk) } },
        dismissButton = { TextButton({ confirmReset = false }) { Text("继续住在这里") } },
    )
}

@Composable
internal fun DebugPanel(content: GameContent, state: GameState, onAction: (GameAction) -> Unit, onDismiss: () -> Unit) {
    var day by remember(state.day) { mutableFloatStateOf(state.day.toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("开发调试", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ onAction(GameAction.DebugEnergy) }, Modifier.weight(1f).testTag("debug_energy")) { Text("恢复体力") }
                    OutlinedButton({ onAction(GameAction.DebugCoins) }, Modifier.weight(1f).testTag("debug_coins")) { Text("加金币") }
                }
                Text("跳到第 ${day.toInt()} 天", style = MaterialTheme.typography.titleSmall)
                Slider(day, { day = it }, valueRange = 1f..24f, steps = 22)
                OutlinedButton({ onAction(GameAction.DebugJump(day.toInt())); onDismiss() }, Modifier.fillMaxWidth()) { Text("跳转并初始化棋盘") }
                Text("为当前订单补齐物品", style = MaterialTheme.typography.titleSmall)
                content.ordersForDay(state.day).filter { it.id !in state.delivered }.forEach { order ->
                    TextButton({ onAction(GameAction.DebugFillOrder(order.id)); onDismiss() }, Modifier.fillMaxWidth().testTag("debug_fill_${order.id}")) { Text("补齐 ${order.id}") }
                }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("回到猫屋") } },
    )
}
