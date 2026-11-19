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
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
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
    content: GameContent, state: GameState, settings: GameSettings,
    onAction: (GameAction) -> Unit, onNavigate: (String) -> Unit, onDebug: () -> Unit,
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
    var showInfo by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    LaunchedEffect(state.board, state.tutorialDone) {
        if (!state.tutorialDone && state.board.any { it?.kind == PieceKind.ITEM && it.level > 1 }) onAction(GameAction.TutorialComplete)
    }
    val piece = selected?.let { state.board.getOrNull(it) }
    val remaining = content.ordersForDay(state.day).count { it.id !in state.delivered }
    val countdown = ((ENERGY_INTERVAL_MS - (now - state.energyAt).coerceAtLeast(0) % ENERGY_INTERVAL_MS) / 1000).toInt()
    BoxWithConstraints(Modifier.fillMaxSize().onGloballyPositioned { screenOrigin = it.boundsInRoot().topLeft }
        .pointerInput(Unit) { awaitPointerEventScope { while (true) { val event = awaitPointerEvent(PointerEventPass.Initial); if (event.changes.any { it.pressed || it.previousPressed }) lastInteractionAt = SystemClock.uptimeMillis() } } }) {
        GameBackground(Modifier.matchParentSize())
        val fontScale = LocalDensity.current.fontScale
        val headerHeight = 60.dp * (fontScale * .9f).coerceAtLeast(1f)
        val dayStampHeight = (38.dp * fontScale + 12.dp).coerceAtLeast(54.dp)
        val footerHeight = (if (maxHeight < 660.dp && fontScale <= 1.15f) 80.dp else 94.dp) * (fontScale * .88f).coerceAtLeast(1f)
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth().height(headerHeight), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GamePanel(Modifier.width(50.dp).height(dayStampHeight).testTag("day_stamp")
                    .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false); val released = withTimeoutOrNull(3000) { waitForUpOrCancellation(); true }; if (released == null) onDebug() } }
                    .semantics { contentDescription = "第${state.day}天" }, color = Wood, radius = 12.dp) {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("天", fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Box(Modifier.fillMaxWidth().padding(horizontal = 3.dp).background(CardPaper, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                            Text(state.day.toString(), fontSize = minOf(23f, 28f / fontScale).sp, lineHeight = 26.sp, fontWeight = FontWeight.Black, color = Ink, maxLines = 1)
                        }
                    }
                }
                BoardEnergy(state.energy, countdown, Modifier.weight(1.15f))
                BoardCoins(state.coins, Modifier.weight(1f))
                GameSquareButton(Icons.Outlined.Pets, "猫图鉴", Modifier.width(40.dp).fillMaxHeight().testTag("almanac_open")) { onNavigate("almanac") }
                GameSquareButton(Icons.Outlined.Settings, "设置", Modifier.width(40.dp).fillMaxHeight().testTag("settings_open")) { onNavigate("settings") }
            }
            // These slots stay fixed when a selection, reward, or end-of-day action appears.
            Box(Modifier.fillMaxWidth().height(120.dp).testTag("order_slot"), contentAlignment = Alignment.Center) {
                if (remaining == 0 && state.bubble.isEmpty()) {
                    GamePanel(Modifier.fillMaxWidth().testTag("orders_completed"), color = DemandSatisfied) {
                        Row(Modifier.heightIn(min = 54.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Outlined.CheckCircle, null, Modifier.size(20.dp), tint = Leaf)
                            Text("今日订单已完成", Modifier.weight(1f), fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, color = Leaf, maxLines = 2)
                            if (engine.canAdvance(state)) {
                                TextButton({ onNavigate("summary") }, Modifier.testTag("summary_open")) { Text(if (state.day == 24) "看看新家" else "结束这一天", fontSize = 12.sp, color = Leaf) }
                            } else Text("${content.ordersForDay(state.day).size}/${content.ordersForDay(state.day).size}", fontSize = 12.sp, color = Leaf)
                        }
                    }
                } else {
                    LazyRow(Modifier.fillMaxSize().testTag("orders"), contentPadding = PaddingValues(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (state.bubble.isNotEmpty()) item(key = "tools") { NewToolCard(state.bubble, boardRect, onAction) { dragPreview.value = it } }
                        if (engine.canAdvance(state)) item(key = "advance") {
                            GamePanel(Modifier.width(156.dp).height(104.dp).testTag("summary_open").clickable { onNavigate("summary") }, color = DemandSatisfied) {
                                Column(Modifier.align(Alignment.Center).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Outlined.CheckCircle, null, Modifier.size(24.dp), tint = Leaf)
                                    Text(if (state.day == 24) "看看新家" else "结束这一天", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Leaf)
                                    Text("工具可以稍后再领", fontSize = 10.sp, color = Muted)
                                }
                            }
                        }
                        items(content.ordersForDay(state.day), key = OrderDef::id) { order ->
                            OrderCard(content, state, order, engine.orderProgress(state, order), engine.isOrderReady(state, order)) { onAction(GameAction.Deliver(order.id)) }
                        }
                    }
                }
            }
            MergeBoard(content, state.board, selected, engine.generatorCap(state.day), settings.reducedMotion,
                Modifier.weight(1f).fillMaxWidth(), demands, GeneratorHints.mainChain(content, state.day), state.energy, lastInteractionAt, generatorTaps,
                onBounds = { boardRect = it }, onDragPreview = { dragPreview.value = it },
                onAccessibleStore = { index -> onAction(GameAction.Store(index)); if (state.day >= 4 && state.warehouse.size < state.warehouseCapacity) selected = null },
                onTap = { index ->
                    selected = if (selected == index && state.board[index]?.kind == PieceKind.ITEM) null else index
                    lastInteractionAt = SystemClock.uptimeMillis()
                    state.board[index]?.takeIf { it.kind == PieceKind.GENERATOR }?.let { generator -> generatorTaps = generatorTaps + (generator.uid to lastInteractionAt); onAction(GameAction.Spawn(index)) }
                }, onDrop = { from, to, rootPosition ->
                    if (warehouseRect.contains(rootPosition)) {
                        onAction(GameAction.Store(from))
                        selected = if (state.day >= 4 && state.warehouse.size < state.warehouseCapacity) null else from
                    } else if (to != null && from != to) { onAction(GameAction.Move(from, to)); selected = to }
                })
            Row(Modifier.fillMaxWidth().height(footerHeight).testTag("board_footer"), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FooterEntrance(Icons.Outlined.Home, "修缮", Modifier.width(48.dp).fillMaxHeight().testTag("build_open")) { onNavigate(if (state.completed) "summary" else "build") }
                BoxWithConstraints(Modifier.weight(1f).fillMaxHeight().testTag("board_action_slot")) {
                    val infoTitleWidth = (maxWidth - if (piece?.kind == PieceKind.ITEM) 54.dp else 0.dp).coerceAtLeast(0.dp)
                    GamePanel(Modifier.fillMaxSize().testTag("item_info")) {
                        if (piece != null) Text(itemDescription(piece, content), Modifier.fillMaxSize().padding(start = 9.dp, end = if (piece.kind == PieceKind.ITEM) 55.dp else 9.dp, top = 33.dp, bottom = 7.dp),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 20.sp), color = Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    if (piece != null) {
                        val headingText = "${pieceName(piece, content)} ${piece.level}级"
                        val headingEm = headingText.fold(0f) { sum, ch -> sum + when { ch.isWhitespace() -> .35f; ch.code < 128 -> .62f; else -> 1f } }
                        val headingSize = minOf(13f, (infoTitleWidth.value - 38f) / (headingEm * fontScale)).coerceAtLeast(9f)
                        Row(Modifier.align(Alignment.TopStart).widthIn(max = infoTitleWidth).testTag("item_info_heading")
                            .background(DarkWood, RoundedCornerShape(topStart = 12.dp, bottomEnd = 8.dp)).padding(start = 2.dp, top = 2.dp, end = 2.dp, bottom = 3.dp)
                            .background(Wood, RoundedCornerShape(topStart = 10.dp, bottomEnd = 6.dp)).padding(start = 6.dp, end = 3.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(headingText, Modifier.weight(1f, fill = false), fontSize = headingSize.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Outlined.Info, "物品说明", Modifier.padding(start = 3.dp).size(20.dp).background(Color(0xFF66BADD), RoundedCornerShape(4.dp)).clickable { showInfo = true }, tint = CardPaper)
                        }
                        if (piece.kind == PieceKind.ITEM) {
                            val price = (content.itemById[piece.itemId]?.value?.div(2) ?: 1).coerceAtLeast(1)
                            Column(Modifier.align(Alignment.CenterEnd).padding(end = 5.dp).width(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("出售", Modifier.testTag("sell_label"), fontSize = 10.sp, lineHeight = 14.sp, color = Muted, fontWeight = FontWeight.Bold)
                                Box(Modifier.size(48.dp).testTag("sell_selected").clickable { selected?.let { onAction(GameAction.Sell(it)) }; selected = null }, contentAlignment = Alignment.Center) {
                                    GamePanel(Modifier.size(42.dp), ReadyGreen, 11.dp) {
                                        GameCoinIcon(Modifier.align(Alignment.TopStart).padding(2.dp), size = 24.dp)
                                        Text(price.toString(), Modifier.align(Alignment.BottomEnd).padding(end = 3.dp), fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
                                    }
                                }
                            }
                        }
                    }
                }
                FooterEntrance(Icons.Outlined.Inventory2, "仓库", Modifier.width(48.dp).fillMaxHeight().testTag("warehouse_open").onGloballyPositioned { warehouseRect = it.boundsInRoot() },
                    subtitle = if (state.day < 4) "第4天开放" else "${state.warehouse.size}/${state.warehouseCapacity}", locked = state.day < 4) { onNavigate("warehouse") }
            }
        }
        DragPreviewOverlay(dragPreview, screenOrigin)
        if (showInfo && piece != null) AlertDialog(onDismissRequest = { showInfo = false }, title = { Text("${pieceName(piece, content)} ${piece.level}级") },
            text = { Text(itemDescription(piece, content) + "\n\n同种同级物品可合成。仓库开放后，将物品拖到右下角仓库入口存放；订单只认棋盘上的物品。") }, confirmButton = { TextButton({ showInfo = false }) { Text("知道了") } })
    }
}

@Composable
private fun FooterEntrance(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier, subtitle: String? = null, locked: Boolean = false, onClick: () -> Unit) {
    GamePanel(modifier.clickable(onClick = onClick)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 1.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Box { Icon(icon, null, Modifier.size(23.dp), tint = Ink); if (locked) Icon(Icons.Outlined.Lock, null, Modifier.align(Alignment.TopEnd).size(11.dp).background(Wood, CircleShape), tint = DarkWood) }
            Text(label, fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
            subtitle?.let { Text(it, fontSize = 8.sp, lineHeight = 11.sp, color = Muted, maxLines = 2) }
        }
    }
}

@Composable
private fun BoardEnergy(energy: Int, countdown: Int, modifier: Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        GamePanel(Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable { expanded = true }.semantics { contentDescription = "体力 $energy，下一点恢复倒计时 $countdown 秒" }, radius = 24.dp) {
            Row(Modifier.fillMaxWidth().padding(start = 3.dp, end = 4.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(23.dp).background(Color(0xFFEAF4FA), CircleShape).border(1.dp, DarkWood, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Bolt, null, Modifier.size(18.dp), tint = Color(0xFF2F7BAE)) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(energy.toString(), fontSize = minOf(16f, 20f / LocalDensity.current.fontScale).sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(if (energy >= MAX_ENERGY) "已满" else "%02d:%02d".format(countdown / 60, countdown % 60), fontSize = 9.sp, lineHeight = 11.sp, color = Muted, maxLines = 1)
                }
            }
        }
        DropdownMenu(expanded, { expanded = false }) { Text("体力 $energy / $MAX_ENERGY", Modifier.padding(16.dp)); Text("每2分钟恢复1点", Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun BoardCoins(coins: Int, modifier: Modifier) {
    GamePanel(modifier.heightIn(min = 44.dp).semantics { contentDescription = "金币 $coins" }, radius = 24.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 3.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            GameCoinIcon(size = 20.dp)
            BoxWithConstraints(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                val font = minOf(18f, maxWidth.value / (coins.toString().length.coerceAtLeast(1) * .62f * LocalDensity.current.fontScale)).coerceAtLeast(8f)
                Text(coins.toString(), fontSize = font.sp, lineHeight = (font + 3).sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

@Composable
private fun NewToolCard(bubble: List<Piece>, boardRect: Rect, onAction: (GameAction) -> Unit, onDragPreview: (DragPreview?) -> Unit) {
    val latestBoardRect by rememberUpdatedState(boardRect)
    val latestAction by rememberUpdatedState(onAction)
    val latestPreview by rememberUpdatedState(onDragPreview)
    GamePanel(Modifier.width(180.dp).height(104.dp).testTag("new_tools"), color = CardPaper) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text("新工具 · 点取或拖入", style = MaterialTheme.typography.labelMedium, color = DarkWood, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    onAccessibleStore: (Int) -> Unit,
    onDragPreview: (DragPreview?) -> Unit,
    onTap: (Int) -> Unit,
    onDrop: (Int, Int?, Offset) -> Unit,
) {
    var dragFrom by remember { mutableStateOf<Int?>(null) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val currentBoard by rememberUpdatedState(board)
    val currentTap by rememberUpdatedState(onTap)
    val currentDrop by rememberUpdatedState(onDrop)
    val currentStore by rememberUpdatedState(onAccessibleStore)
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
            .clip(RoundedCornerShape(20.dp)).background(BoardBlue).border(2.dp, DarkWood, RoundedCornerShape(20.dp)).testTag("workshop_tray"), contentAlignment = Alignment.Center) {
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
                    val tint = if (index == selected) SelectionYellow else if (same) Leaf else BoardCell
                    Box(
                        Modifier.offset(x = cell * (index % BOARD_COLS), y = cell * (index / BOARD_COLS)).size(cell).padding(1.dp)
                            .clip(RoundedCornerShape(10.dp)).background(if (index == selected) Color(0xFFFFF0BD) else if (piece?.kind == PieceKind.GENERATOR) Color(0xFFF3EED2) else BoardCell)
                            .border(if (same || index == selected) 2.dp else 0.dp, tint, RoundedCornerShape(10.dp))
                            .testTag("cell_$index")
                            .semantics {
                                contentDescription = piece?.let { "${pieceName(it, content)} ${it.level}级 第${index + 1}格" } ?: "空格 ${index + 1}"
                                stateDescription = when {
                                    piece?.kind == PieceKind.GENERATOR -> if (energy <= 0) "发射器，体力不足，暂不能产出" else "发射器，点击消耗 1 点体力"
                                    demand != null -> "${if (demand.satisfied) "需求已满足" else "需求仍欠缺"}，订单 ${demand.orderId}，${demand.available}/${demand.required}"
                                    else -> "没有当前订单需求"
                                }
                                if (piece != null) customActions = listOf(CustomAccessibilityAction("存入仓库") { currentStore(index); true })
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
