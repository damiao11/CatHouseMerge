package com.example.cathousemerge.ui

import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cathousemerge.R
import com.example.cathousemerge.game.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

@Composable
internal fun BoardScreen(
    content: GameContent,
    state: GameState,
    settings: GameSettings,
    onAction: (GameAction) -> Unit,
    onNavigate: (String) -> Unit,
    onDebug: () -> Unit,
) {
    val engine = remember(content) { GameEngine(content) }
    var selected by remember(state.day) { mutableStateOf<Int?>(null) }
    var warehouseRect by remember { mutableStateOf(Rect.Zero) }
    var boardRect by remember { mutableStateOf(Rect.Zero) }
    var screenOrigin by remember { mutableStateOf(Offset.Zero) }
    val dragPreview = remember { mutableStateOf<DragPreview?>(null) }
    val demands = remember(state.board, state.delivered, state.day, content) { engine.itemDemandFeedback(state) }
    var lastInteractionAt by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    var generatorTaps by remember { mutableStateOf(emptyMap<Long, Long>()) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    LaunchedEffect(state.board, state.tutorialDone) {
        if (!state.tutorialDone && state.board.any { it?.kind == PieceKind.ITEM && it.level > 1 }) {
            onAction(GameAction.TutorialComplete)
        }
    }
    val piece = selected?.let { state.board.getOrNull(it) }
    val today = content.day(state.day)
    val remaining = content.ordersForDay(state.day).count { it.id !in state.delivered }
    val countdown = ((ENERGY_INTERVAL_MS - (now - state.energyAt).coerceAtLeast(0) % ENERGY_INTERVAL_MS) / 1000).toInt()

    Box(Modifier.fillMaxSize().background(Paper).onGloballyPositioned { screenOrigin = it.boundsInRoot().topLeft }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.changes.any { it.pressed || it.previousPressed }) lastInteractionAt = SystemClock.uptimeMillis()
                }
            }
        }) {
        JournalPaper(Modifier.matchParentSize())
        Column(Modifier.fillMaxSize()) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val compactHeader = maxWidth < 360.dp || LocalDensity.current.fontScale > 1.2f
                Column {
                    Row(Modifier.fillMaxWidth().heightIn(min = 68.dp).padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.testTag("day_stamp").pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                val released = withTimeoutOrNull(3000) { waitForUpOrCancellation(); true }
                                if (released == null) onDebug()
                            }
                        }) {
                            Text("第 ${state.day} 天", style = MaterialTheme.typography.titleLarge, fontSize = 22.sp)
                            Text(roomName(today.room), style = MaterialTheme.typography.labelMedium, color = JournalWoodInk)
                        }
                        Spacer(Modifier.weight(1f))
                        if (!compactHeader) {
                            JournalEnergy(state.energy, countdown)
                            Spacer(Modifier.width(4.dp))
                            JournalCoins(state.coins)
                        }
                        IconButton({ onNavigate("almanac") }, Modifier.size(44.dp).testTag("almanac_open")) { Icon(Icons.Outlined.Pets, "猫图鉴", Modifier.size(22.dp), tint = Ink) }
                        IconButton({ onNavigate("settings") }, Modifier.size(44.dp).testTag("settings_open")) { Icon(Icons.Outlined.Settings, "设置", Modifier.size(22.dp), tint = Ink) }
                    }
                    if (compactHeader) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                            JournalEnergy(state.energy, countdown)
                            JournalCoins(state.coins)
                        }
                    }
                }
            }

            if (remaining == 0 && state.bubble.isEmpty()) {
                Surface(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp).testTag("orders_completed"),
                    shape = RoundedCornerShape(18.dp), color = Color(0xFFEAECD5),
                ) {
                    Row(Modifier.heightIn(min = 38.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CheckCircle, null, Modifier.size(20.dp), tint = Leaf)
                        Text("今日订单已完成", Modifier.weight(1f).padding(start = 8.dp), style = MaterialTheme.typography.titleSmall, color = Leaf)
                        val total = content.ordersForDay(state.day).size
                        Text("$total/$total", style = MaterialTheme.typography.titleSmall, color = Leaf)
                    }
                }
            } else {
                LazyRow(
                    Modifier.fillMaxWidth().height(112.dp).testTag("orders"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (state.bubble.isNotEmpty()) item(key = "tools") {
                        NewToolCard(state.bubble, boardRect, onAction) { dragPreview.value = it }
                    }
                    items(content.ordersForDay(state.day), key = OrderDef::id) { order ->
                        OrderCard(content, state, order, engine.orderProgress(state, order), engine.isOrderReady(state, order)) {
                            onAction(GameAction.Deliver(order.id))
                        }
                    }
                }
            }

            val hint = when {
                !state.tutorialDone && state.day == 1 && state.board.none { it?.kind == PieceKind.GENERATOR } -> "点上方的新工具，把木工台放到棋盘上"
                !state.tutorialDone && state.day == 1 && state.board.count { it?.kind == PieceKind.ITEM } < 2 -> "点木工台产出材料，每次消耗 1 点体力"
                piece?.kind == PieceKind.GENERATOR -> "点按产出材料 · 同级工具可合成升级"
                piece != null -> "同级合成，或交给需要它的猫"
                else -> "拖动相同材料进行合成"
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 3.dp).heightIn(min = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Outlined.TouchApp, null, Modifier.size(14.dp), tint = JournalWoodInk)
                Text(hint, Modifier.padding(start = 6.dp), style = MaterialTheme.typography.bodySmall, color = JournalWoodInk, maxLines = 2)
            }

            MergeBoard(
                content, state.board, selected, engine.generatorCap(state.day), settings.reducedMotion,
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                demands = demands, mainChain = GeneratorHints.mainChain(content, state.day), energy = state.energy,
                lastInteractionAt = lastInteractionAt, generatorTaps = generatorTaps,
                onBounds = { boardRect = it },
                onDragPreview = { dragPreview.value = it },
                onTap = { index ->
                    selected = if (selected == index && state.board[index]?.kind != PieceKind.GENERATOR) null else index
                    lastInteractionAt = SystemClock.uptimeMillis()
                    state.board[index]?.takeIf { it.kind == PieceKind.GENERATOR }?.let { generator ->
                        generatorTaps = generatorTaps + (generator.uid to lastInteractionAt)
                        onAction(GameAction.Spawn(index))
                    }
                },
                onDrop = { from, to, rootPosition ->
                    if (warehouseRect.contains(rootPosition)) {
                        onAction(GameAction.Store(from))
                        selected = null
                    } else if (to != null && from != to) {
                        onAction(GameAction.Move(from, to))
                        selected = to
                    }
                },
            )

            Surface(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp), color = CardPaper.copy(alpha = 0.86f),
            ) {
                Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    val roomDays = content.days.filter { it.room == today.room }
                    val repaired = roomDays.count { it.day in state.builtDays }
                    Row(Modifier.fillMaxWidth().heightIn(min = 54.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Handyman, null, Modifier.size(28.dp), tint = JournalWoodInk)
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${roomName(today.room)}修复", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                                Text("$repaired/${roomDays.size}", style = MaterialTheme.typography.labelLarge, color = JournalWoodInk)
                            }
                            Text(
                                when {
                                    state.completed -> "每一个角落，都有了家的样子"
                                    state.day in state.builtDays -> "今天的修缮完成了"
                                    remaining > 0 -> "今天还剩 $remaining 条订单"
                                    else -> "再修好一处，完成今天的${roomName(today.room)}"
                                },
                                style = MaterialTheme.typography.bodySmall, color = JournalMuted, maxLines = 2,
                            )
                        }
                        Button(
                            { onNavigate(if (state.completed) "summary" else "build") },
                            Modifier.heightIn(min = 44.dp).testTag("build_open"),
                            shape = CircleShape, contentPadding = PaddingValues(horizontal = 18.dp),
                        ) { Text(if (state.completed) "看新家" else if (state.day in state.builtDays) "看进度" else "去修复") }
                    }
                    Surface(
                        onClick = { onNavigate("warehouse") },
                        modifier = Modifier.fillMaxWidth().testTag("warehouse_open").onGloballyPositioned { warehouseRect = it.boundsInRoot() },
                        shape = RoundedCornerShape(14.dp), color = Color(0xFFF0E5D1),
                    ) {
                        Row(Modifier.heightIn(min = 40.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Inventory2, "仓库", Modifier.size(18.dp), tint = JournalMuted)
                            Text("仓库", Modifier.weight(1f).padding(start = 8.dp), style = MaterialTheme.typography.labelLarge, color = Ink)
                            if (state.day < 4) Icon(Icons.Outlined.Lock, null, Modifier.size(14.dp), tint = JournalMuted)
                            Text(if (state.day < 4) "D4 开放" else "${state.warehouse.size}/${state.warehouseCapacity}", Modifier.padding(start = 5.dp), style = MaterialTheme.typography.labelMedium, color = JournalMuted)
                        }
                    }
                    if (piece != null) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            if (piece.kind == PieceKind.ITEM) SmallAction(Icons.Outlined.MonetizationOn, "卖出 ${(content.itemById[piece.itemId]?.value?.div(2) ?: 1).coerceAtLeast(1)}") {
                                selected?.let { onAction(GameAction.Sell(it)) }; selected = null
                            }
                            SmallAction(Icons.Outlined.Inventory2, "收进仓库") { selected?.let { onAction(GameAction.Store(it)) }; selected = null }
                        }
                    } else if (engine.canAdvance(state)) {
                        TextButton({ onNavigate("summary") }, Modifier.fillMaxWidth().testTag("summary_open")) {
                            Text(if (state.day == 24) "看看家的新模样" else "结束这一天")
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.padding(start = 8.dp).size(16.dp))
                        }
                    }
                }
            }
        }
        DragPreviewOverlay(dragPreview, screenOrigin)
    }
}

@Composable
private fun NewToolCard(bubble: List<Piece>, boardRect: Rect, onAction: (GameAction) -> Unit, onDragPreview: (DragPreview?) -> Unit) {
    val latestBoardRect by rememberUpdatedState(boardRect)
    val latestAction by rememberUpdatedState(onAction)
    val latestPreview by rememberUpdatedState(onDragPreview)
    Surface(Modifier.width(180.dp).height(96.dp).testTag("new_tools"), shape = RoundedCornerShape(16.dp), color = Color(0xFFF0E1C9), border = androidx.compose.foundation.BorderStroke(1.dp, Wood)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text("新工具 · 点取或拖入", style = MaterialTheme.typography.labelMedium, color = JournalWoodInk)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                bubble.take(2).forEach { piece ->
                    var origin by remember(piece.uid) { mutableStateOf(Offset.Zero) }
                    var pickedUp by remember(piece.uid) { mutableStateOf(false) }
                    Box(Modifier.size(52.dp).testTag("bubble_${piece.uid}").onGloballyPositioned { origin = it.boundsInRoot().topLeft }.pointerInput(piece.uid) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            val start = down.position
                            var moved = false
                            try {
                                while (true) {
                                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                    // Canceled gestures arrive consumed and must never become a tap or drop.
                                    if (change.isConsumed) break
                                    val position = change.position
                                    if ((position - start).getDistance() > 12.dp.toPx()) moved = true
                                    if (moved) {
                                        pickedUp = true
                                        change.consume()
                                        latestPreview(DragPreview(piece, origin + position, 52.dp))
                                    }
                                    if (!change.pressed) {
                                        if (!moved) latestAction(GameAction.TakeBubble(piece.uid))
                                        else {
                                            val root = origin + position
                                            val bounds = latestBoardRect
                                            if (bounds.contains(root) && bounds.width > 0) {
                                                val col = ((root.x - bounds.left) / (bounds.width / BOARD_COLS)).toInt()
                                                val row = ((root.y - bounds.top) / (bounds.height / BOARD_ROWS)).toInt()
                                                latestAction(GameAction.TakeBubble(piece.uid, row * BOARD_COLS + col))
                                            }
                                        }
                                        break
                                    }
                                }
                            } finally { pickedUp = false; latestPreview(null) }
                        }
                    }.semantics { contentDescription = "领取${piece.chain}工具"; onClick { onAction(GameAction.TakeBubble(piece.uid)); true } }, contentAlignment = Alignment.Center) {
                        GameImage(piece.asset, null, Modifier.fillMaxSize().alpha(if (pickedUp) 0.2f else 1f))
                    }
                }
                if (bubble.size > 2) Text("+${bubble.size - 2}", Modifier.align(Alignment.CenterVertically), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun MergeBoard(
    content: GameContent,
    board: List<Piece?>,
    selected: Int?,
    generatorCap: Int,
    reducedMotion: Boolean,
    modifier: Modifier,
    demands: Map<String, ItemDemandFeedback>,
    mainChain: String?,
    energy: Int,
    lastInteractionAt: Long,
    generatorTaps: Map<Long, Long>,
    onBounds: (Rect) -> Unit,
    onDragPreview: (DragPreview?) -> Unit,
    onTap: (Int) -> Unit,
    onDrop: (Int, Int?, Offset) -> Unit,
) {
    var dragFrom by remember { mutableStateOf<Int?>(null) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val currentBoard by rememberUpdatedState(board)
    val currentTap by rememberUpdatedState(onTap)
    val currentDrop by rememberUpdatedState(onDrop)
    val currentPreview by rememberUpdatedState(onDragPreview)
    val hint by produceState<GeneratorHint?>(null, board, mainChain, energy, lastInteractionAt, generatorTaps, reducedMotion) {
        if (board.none { it?.kind == PieceKind.GENERATOR }) { value = null; return@produceState }
        while (true) {
            val time = SystemClock.uptimeMillis()
            val next = GeneratorHints.at(board, mainChain, energy, time, lastInteractionAt, generatorTaps, reducedMotion)
            value = next
            val cycle = (time - lastInteractionAt - GeneratorHints.IDLE_MS).coerceAtLeast(0) % GeneratorHints.CYCLE_MS
            // Animate the 600ms pulse at frame cadence. Steady badges do not recompose the board.
            delay(if (next?.phase == GeneratorHintPhase.PULSE && cycle < 2_600) 16 else 100)
        }
    }
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val rim = 10.dp
        val cell = minOf(56.dp, (maxWidth - rim * 2) / BOARD_COLS, (maxHeight - rim * 2) / BOARD_ROWS).coerceAtLeast(1.dp)
        val cellPx = with(LocalDensity.current) { cell.toPx() }
        val dragPiece = dragFrom?.let { board.getOrNull(it) }
        Box(Modifier.size(cell * BOARD_COLS + rim * 2, cell * BOARD_ROWS + rim * 2)
            .clip(RoundedCornerShape(16.dp)).background(Color(0xFFE9CDA6)).testTag("workshop_tray"), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.ui_workshop_frame), null, Modifier.matchParentSize(), contentScale = ContentScale.FillBounds)
            Box(
                Modifier.size(cell * BOARD_COLS, cell * BOARD_ROWS)
                    .onGloballyPositioned { val bounds = it.boundsInRoot(); origin = bounds.topLeft; onBounds(bounds) }
                    .testTag("board")
                    .pointerInput(cellPx) {
                        fun hit(position: Offset): Int? {
                            if (position.x < 0 || position.y < 0 || position.x >= cellPx * BOARD_COLS || position.y >= cellPx * BOARD_ROWS) return null
                            return (position.y / cellPx).toInt() * BOARD_COLS + (position.x / cellPx).toInt()
                        }
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            val from = hit(down.position) ?: return@awaitEachGesture
                            val start = down.position
                            var dragging = false
                            try {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (change.isConsumed) break
                                    if (!dragging && currentBoard.getOrNull(from) != null && (change.position - start).getDistance() >= 8.dp.toPx()) {
                                        dragging = true
                                        dragFrom = from
                                    }
                                    if (dragging) {
                                        change.consume()
                                        currentBoard.getOrNull(from)?.let { currentPreview(DragPreview(it, origin + change.position, cell)) }
                                    }
                                    if (!change.pressed) {
                                        if (dragging) currentDrop(from, hit(change.position), origin + change.position)
                                        else currentTap(from)
                                        break
                                    }
                                }
                            } finally { dragFrom = null; currentPreview(null) }
                        }
                    },
            ) {
                val canMerge = dragPiece?.let {
                    val maxLevel = if (it.kind == PieceKind.GENERATOR) generatorCap else content.chainById[it.chain]?.itemIds?.size ?: it.level
                    it.level < maxLevel
                } == true
                board.forEachIndexed { index, piece ->
                    val same = canMerge && piece != null && piece.kind == dragPiece.kind && piece.chain == dragPiece.chain && piece.level == dragPiece.level && index != dragFrom
                    val demand = piece?.takeIf { it.kind == PieceKind.ITEM }?.let { demands[it.itemId] }
                    val tint = if (index == selected) Wood else if (same) Leaf else if (demand?.satisfied == true) Leaf else if (demand != null) DemandPendingInk else Line
                    Box(
                        Modifier.offset(x = cell * (index % BOARD_COLS), y = cell * (index / BOARD_COLS)).size(cell).padding(1.dp)
                            .clip(RoundedCornerShape(7.dp)).background(if (piece?.kind == PieceKind.GENERATOR) Color(0xFFEEDDC2) else if ((index + index / BOARD_COLS) % 2 == 0) Color(0xFFF2E4CC) else Color(0xFFF7EBD9))
                            .border(if (same || index == selected || demand != null) 2.dp else 0.8.dp, tint, RoundedCornerShape(7.dp))
                            .testTag("cell_$index")
                            .semantics {
                                contentDescription = piece?.let { "${pieceName(it, content)} ${it.level}级 第${index + 1}格" } ?: "空格 ${index + 1}"
                                stateDescription = when {
                                    piece?.kind == PieceKind.GENERATOR -> if (energy <= 0) "发射器，体力不足，暂不能产出" else "发射器，点击消耗 1 点体力"
                                    demand != null -> "${if (demand.satisfied) "需求已满足" else "需求仍欠缺"}，订单 ${demand.orderId}，${demand.available}/${demand.required}"
                                    else -> "没有当前订单需求"
                                }
                                onClick { onTap(index); true }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (piece != null) {
                            key(piece.uid, piece.level) {
                                var entered by remember { mutableStateOf(reducedMotion) }
                                LaunchedEffect(Unit) { entered = true }
                                val scale by animateFloatAsState(if (entered) 1f else 0.72f, tween(if (reducedMotion) 0 else 180), label = "pieceAppear")
                                val activeHint = hint?.takeIf { it.uid == piece.uid }
                                val cueScale = activeHint?.scale ?: 1f
                                GameImage(piece.asset, null, Modifier.fillMaxSize().padding(if (piece.kind == PieceKind.ITEM) 2.dp else 0.dp)
                                    .graphicsLayer { scaleX = scale * cueScale; scaleY = scale * cueScale; shape = RoundedCornerShape(7.dp); shadowElevation = ((cueScale - 1f) / 0.06f).coerceIn(0f, 1f) * 2.dp.toPx() }
                                    .alpha(if (dragFrom == index) 0.2f else 1f))
                                if (piece.kind == PieceKind.GENERATOR && activeHint != null && dragFrom != index &&
                                    (activeHint.phase == GeneratorHintPhase.BADGE || activeHint.phase == GeneratorHintPhase.NO_ENERGY)) {
                                    GeneratorEnergyBadge(activeHint.phase == GeneratorHintPhase.NO_ENERGY,
                                        Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp).size(cell * 0.4f).alpha(activeHint.badgeAlpha))
                                }
                                if (demand != null) {
                                    Icon(if (demand.satisfied) Icons.Outlined.CheckCircle else Icons.Outlined.Schedule, null,
                                        Modifier.align(Alignment.BottomEnd).padding(1.dp).size(11.dp).background(CardPaper, CircleShape),
                                        tint = if (demand.satisfied) Leaf else DemandPendingInk)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class DragPreview(val piece: Piece, val position: Offset, val size: Dp)

@Composable
private fun DragPreviewOverlay(preview: State<DragPreview?>, screenOrigin: Offset) {
    val drag = preview.value ?: return
    val halfSize = with(LocalDensity.current) { drag.size.toPx() / 2 }
    GameImage(
        drag.piece.asset,
        null,
        Modifier.offset { IntOffset((drag.position.x - screenOrigin.x - halfSize).roundToInt(), (drag.position.y - screenOrigin.y - halfSize).roundToInt()) }
            .size(drag.size).graphicsLayer { scaleX = 1.15f; scaleY = 1.15f },
    )
}
