package com.example.cathousemerge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cathousemerge.game.*

internal val DemandPending = Color(0xFFFFF0CA)
internal val DemandPendingInk = Color(0xFF906126)
internal val DemandSatisfied = Color(0xFFE6F0CC)

@Composable
internal fun OrderCard(content: GameContent, state: GameState, order: OrderDef, progress: List<Int>, ready: Boolean, onClick: () -> Unit) {
    val done = order.id in state.delivered
    val fontScale = LocalDensity.current.fontScale
    val width = if (fontScale > 1.2f) 320.dp else 288.dp
    val itemSize = when {
        fontScale > 1.4f -> 34.dp
        fontScale > 1.2f -> 40.dp
        order.req.size == 1 -> 52.dp
        else -> 48.dp
    }
    GamePanel(Modifier.width(width).height(104.dp).testTag("order_${order.id}")
        .semantics { stateDescription = if (done) "已交付" else if (ready) "可交付" else "进行中" }) {
        Row(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val cat = catForDay(order.day)
            GameImage("cat_$cat", "${catName(cat)}的订单", Modifier.size(50.dp).testTag("order_avatar_${order.id}"))
            Row(Modifier.weight(1f).testTag("order_items_${order.id}"),
                horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                order.req.forEachIndexed { index, req ->
                    val count = if (done) req.count else (progress.getOrNull(index) ?: 0).coerceIn(0, req.count)
                    val satisfied = count >= req.count
                    val item = content.itemById[req.itemId]
                    val name = item?.name ?: req.itemId
                    Column(Modifier.weight(1f).testTag("requirement_${order.id}_$index")
                        .semantics(mergeDescendants = true) {
                            stateDescription = if (satisfied) "已满足 $count/${req.count}" else "未满足 $count/${req.count}"
                            contentDescription = "$name $count/${req.count}"
                        }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Box(Modifier.size(itemSize).testTag("order_item_${order.id}_$index")) {
                            GameImage(item?.asset ?: "ic_${req.itemId}", null, Modifier.fillMaxSize())
                            if (satisfied) Box(Modifier.align(Alignment.BottomEnd).size(13.dp).background(ReadyGreen, CircleShape)
                                .border(1.dp, ReadyOutline, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Check, null, Modifier.size(10.dp), tint = Leaf)
                            }
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)) {
                            Text(name, Modifier.weight(1f, fill = false), fontSize = 12.sp, lineHeight = 16.sp,
                                fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Ink)
                            Text("$count/${req.count}", Modifier.testTag("order_count_${order.id}_$index"), fontSize = 14.sp, lineHeight = 18.sp,
                                fontWeight = FontWeight.Bold, color = if (satisfied) Leaf else DemandPendingInk, maxLines = 1)
                        }
                    }
                }
            }
            Column(Modifier.width(64.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.fillMaxWidth().height(44.dp)
                    .clickable(enabled = !done, role = Role.Button, onClick = onClick).testTag("deliver_${order.id}")) {
                    GamePanel(Modifier.fillMaxSize(), if (ready && !done) ReadyGreen else if (done) DemandSatisfied else DemandPending, 11.dp) {
                        Text(if (done) "已交付" else "交付", Modifier.align(Alignment.Center), fontSize = 12.sp, lineHeight = 16.sp,
                            color = if (ready || done) Leaf else Muted, fontWeight = FontWeight.Bold)
                    }
                }
                GameCoin(order.reward, size = 16.dp)
            }
        }
    }
}
