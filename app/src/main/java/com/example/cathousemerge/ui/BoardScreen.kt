package com.example.cathousemerge.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var showEnergy by remember { mutableStateOf(false) }
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

    Box(Modifier.fillMaxSize().background(Paper).onGloballyPositioned { screenOrigin = it.boundsInRoot().topLeft }) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.testTag("day_stamp").pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        val released = withTimeoutOrNull(3000) { waitForUpOrCancellation(); true }
                        if (released == null) onDebug()
                    }
                }) {
                    Text("第 ${state.day} 天", style = MaterialTheme.typography.titleLarge, fontSize = 20.sp)
                    Text(roomName(today.room), style = MaterialTheme.typography.labelMedium, color = DarkWood)
                }
                Spacer(Modifier.weight(1f))
                Box {
                    TextButton(onClick = { showEnergy = true }, contentPadding = PaddingValues(horizontal = 6.dp)) {
                        Icon(Icons.Outlined.Bolt, "体力", Modifier.size(18.dp), tint = DarkWood)
                        Column {
                            Text(state.energy.toString(), style = MaterialTheme.typography.titleSmall, color = Ink)
                            Text(if (state.energy >= MAX_ENERGY) "已满" else "%02d:%02d".format(countdown / 60, countdown % 60), style = MaterialTheme.typography.labelMedium, fontSize = 10.sp, color = Muted)
                        }
                    }
                    DropdownMenu(showEnergy, { showEnergy = false }) {
                        Text("体力 ${state.energy} / $MAX_ENERGY", Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                        Text("每 2 分钟恢复 1 点", Modifier.padding(horizontal = 20.dp, vertical = 8.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
                CoinAmount(state.coins)
                IconButton({ onNavigate("almanac") }, Modifier.size(44.dp).testTag("almanac_open")) { Icon(Icons.Outlined.Pets, "猫图鉴", Modifier.size(22.dp)) }
                IconButton({ onNavigate("settings") }, Modifier.size(44.dp).testTag("settings_open")) { Icon(Icons.Outlined.Settings, "设置", Modifier.size(22.dp)) }
            }

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

            if (!state.tutorialDone && state.day == 1) {
                val hint = when {
                    state.board.none { it?.kind == PieceKind.GENERATOR } -> "点上方的新工具，把木工台放到棋盘上"
                    state.board.count { it?.kind == PieceKind.ITEM } < 2 -> "点木工台产出材料，每次消耗 1 点体力"
                    else -> "拖动两个一样的材料，合成更高一级"
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp).heightIn(min = 28.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.TouchApp, null, Modifier.size(16.dp), tint = DarkWood)
                    Spacer(Modifier.width(6.dp))
                    Text(hint, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = DarkWood)
                }
            }

            MergeBoard(
                content, state.board, selected, engine.generatorCap(state.day), settings.reducedMotion,
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                onBounds = { boardRect = it },
                onDragPreview = { dragPreview.value = it },
                onTap = { index ->
                    selected = if (selected == index && state.board[index]?.kind != PieceKind.GENERATOR) null else index
                    if (state.board[index]?.kind == PieceKind.GENERATOR) onAction(GameAction.Spawn(index))
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

            Column(Modifier.fillMaxWidth().background(Color(0xFFF2E7D4)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton({ onNavigate(if (state.completed) "summary" else "build") }, Modifier.heightIn(min = 56.dp).testTag("build_open"), shape = RoundedCornerShape(18.dp), contentPadding = PaddingValues(horizontal = 14.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.Handyman, null, Modifier.size(20.dp))
                            Text("建造", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Column(Modifier.weight(1f).heightIn(min = 56.dp), verticalArrangement = Arrangement.Center) {
                        Text(piece?.let { "${pieceName(it, content)} · ${it.level}级" } ?: "把旧日子，慢慢修回来", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            when {
                                piece?.kind == PieceKind.GENERATOR -> "点按产出 · 同级工具可合成升级"
                                piece != null -> "同级合成，或交给需要它的猫"
                                remaining > 0 -> "今天还剩 $remaining 条订单"
                                state.day !in state.builtDays -> "订单已完成，去修好今天这一角"
                                else -> "今天的修缮完成了"
                            },
                            style = MaterialTheme.typography.bodySmall, color = Muted, maxLines = 2,
                        )
                    }
                    OutlinedButton({ onNavigate("warehouse") }, Modifier.testTag("warehouse_open").onGloballyPositioned { warehouseRect = it.boundsInRoot() }.heightIn(min = 56.dp), shape = RoundedCornerShape(18.dp), contentPadding = PaddingValues(horizontal = 10.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.Inventory2, "仓库", Modifier.size(20.dp))
                            Text(if (state.day < 4) "D4开放" else "仓库", style = MaterialTheme.typography.labelMedium, fontSize = 10.sp)
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.CenterEnd) {
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
                            Icon(Icons.Outlined.ArrowForward, null, Modifier.padding(start = 8.dp).size(16.dp))
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
            Text("新工具 · 点取或拖入", style = MaterialTheme.typography.labelMedium, color = DarkWood)
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
private fun OrderCard(content: GameContent, state: GameState, order: OrderDef, progress: List<Int>, ready: Boolean, onClick: () -> Unit) {
    val done = order.id in state.delivered
    val border by animateColorAsState(if (ready && !done) Leaf else Line, label = "orderBorder")
    Surface(
        onClick = onClick,
        modifier = Modifier.width(180.dp).height(96.dp).testTag("order_${order.id}"),
        shape = RoundedCornerShape(16.dp), color = if (done) Color(0xFFF0EEE4) else CardPaper,
        border = androidx.compose.foundation.BorderStroke(if (ready && !done) 2.dp else 1.dp, border),
    ) {
        if (done) {
            Row(Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.CheckCircle, "已完成", tint = Leaf)
                Column { Text("已经交付", style = MaterialTheme.typography.titleSmall); Text("谢谢你呀。", style = MaterialTheme.typography.bodySmall, color = Muted) }
            }
        } else {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CatAvatar(catForDay(order.day), Modifier.size(36.dp))
                    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.MonetizationOn, null, Modifier.size(12.dp), tint = DarkWood)
                        Text(order.reward.toString(), style = MaterialTheme.typography.labelMedium, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.width(8.dp))
                order.req.forEachIndexed { index, req ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        GameImage(content.itemById[req.itemId]?.asset ?: "ic_${req.chain}_${req.level}", content.itemById[req.itemId]?.name, Modifier.size(42.dp))
                        Text("${(progress.getOrNull(index) ?: 0).coerceAtMost(req.count)}/${req.count}", style = MaterialTheme.typography.labelMedium, color = if ((progress.getOrNull(index) ?: 0) >= req.count) Leaf else Ink)
                    }
                }
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
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val cell = minOf(44.dp, maxWidth / BOARD_COLS, maxHeight / BOARD_ROWS)
        val cellPx = with(LocalDensity.current) { cell.toPx() }
        val dragPiece = dragFrom?.let { board.getOrNull(it) }
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
                val same = canMerge && dragPiece != null && piece != null && piece.kind == dragPiece.kind && piece.chain == dragPiece.chain && piece.level == dragPiece.level && index != dragFrom
                val tint = if (index == selected) Wood else if (same) Leaf else Line
                Box(
                    Modifier.offset(x = cell * (index % BOARD_COLS), y = cell * (index / BOARD_COLS)).size(cell).padding(1.dp)
                        .clip(RoundedCornerShape(7.dp)).background(if ((index + index / BOARD_COLS) % 2 == 0) Color(0xFFF3E9D8) else Color(0xFFF7EFE0))
                        .border(if (same || index == selected) 2.dp else 0.5.dp, tint, RoundedCornerShape(7.dp))
                        .testTag("cell_$index")
                        .semantics {
                            contentDescription = piece?.let { "${pieceName(it, content)} ${it.level}级 第${index + 1}格" } ?: "空格 ${index + 1}"
                            onClick { onTap(index); true }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (piece != null) {
                        key(piece.uid, piece.level) {
                            var entered by remember { mutableStateOf(reducedMotion) }
                            LaunchedEffect(Unit) { entered = true }
                            val scale by animateFloatAsState(if (entered) 1f else 0.72f, tween(if (reducedMotion) 0 else 180), label = "pieceAppear")
                            GameImage(piece.asset, null, Modifier.fillMaxSize().padding(if (piece.kind == PieceKind.ITEM) 2.dp else 0.dp).graphicsLayer { scaleX = scale; scaleY = scale }.alpha(if (dragFrom == index) 0.2f else 1f))
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
