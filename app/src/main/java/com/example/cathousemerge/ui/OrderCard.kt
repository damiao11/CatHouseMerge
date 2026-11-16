package com.example.cathousemerge.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cathousemerge.game.*

internal val DemandPending = Color(0xFFE8D9B5)
internal val DemandPendingInk = Color(0xFF986B2F)
internal val DemandSatisfied = Color(0xFFE1EDD4)

@Composable
internal fun OrderCard(content: GameContent, state: GameState, order: OrderDef, progress: List<Int>, ready: Boolean, onClick: () -> Unit) {
    val done = order.id in state.delivered
    val border by animateColorAsState(if (ready && !done) Leaf else if (done) Line else Wood, label = "orderBorder")
    val background by animateColorAsState(if (done) Color(0xFFF0EEE4) else if (ready) Color(0xFFEDF4E4) else CardPaper, label = "orderBackground")
    Surface(
        onClick = onClick, enabled = !done,
        modifier = Modifier.width(if (order.req.size > 2) 224.dp else 180.dp).height(96.dp).testTag("order_${order.id}")
            .semantics { stateDescription = if (done) "已交付" else if (ready) "可交付" else "进行中" },
        shape = RoundedCornerShape(16.dp), color = background,
        border = BorderStroke(if (ready && !done) 2.dp else 1.dp, border),
    ) {
        if (done) {
            Row(Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.CheckCircle, "已完成", tint = Leaf)
                Column { Text("已经交付", style = MaterialTheme.typography.titleSmall); Text("谢谢你呀。", style = MaterialTheme.typography.bodySmall, color = Muted) }
            }
        } else {
            Column(Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                Text(if (ready) "可交付" else "进行中", style = MaterialTheme.typography.labelMedium, fontSize = 10.sp, color = if (ready) Leaf else DarkWood)
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.width(36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CatAvatar(catForDay(order.day), Modifier.size(32.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.MonetizationOn, null, Modifier.size(11.dp), tint = DarkWood)
                            Text(order.reward.toString(), style = MaterialTheme.typography.labelMedium, fontSize = 10.sp)
                        }
                    }
                    Spacer(Modifier.width(5.dp))
                    order.req.forEachIndexed { index, req ->
                        val count = (progress.getOrNull(index) ?: 0).coerceIn(0, req.count)
                        val satisfied = count >= req.count
                        val tile by animateColorAsState(if (satisfied) DemandSatisfied else DemandPending, label = "requirement")
                        Box(Modifier.weight(1f).height(60.dp).padding(horizontal = 2.dp).clip(RoundedCornerShape(8.dp)).background(tile)
                            .testTag("requirement_${order.id}_$index")
                            .semantics { stateDescription = if (satisfied) "已满足 $count/${req.count}" else "未满足 $count/${req.count}" }) {
                            GameImage(content.itemById[req.itemId]?.asset ?: "ic_${req.chain}_${req.level}", content.itemById[req.itemId]?.name,
                                Modifier.align(Alignment.TopCenter).padding(top = 2.dp).size(38.dp))
                            Text("$count/${req.count}", Modifier.align(Alignment.BottomEnd).padding(end = 4.dp, bottom = 2.dp),
                                style = MaterialTheme.typography.labelMedium, fontSize = 11.sp, color = if (satisfied) Leaf else Ink)
                        }
                    }
                }
            }
        }
    }
}
