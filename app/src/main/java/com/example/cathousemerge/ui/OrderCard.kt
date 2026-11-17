package com.example.cathousemerge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cathousemerge.R
import com.example.cathousemerge.game.*

internal val DemandPending = Color(0xFFFFF0CA)
internal val DemandPendingInk = Color(0xFF906126)
internal val DemandSatisfied = Color(0xFFE6F0CC)

@Composable
internal fun OrderCard(content: GameContent, state: GameState, order: OrderDef, progress: List<Int>, ready: Boolean, onClick: () -> Unit) {
    val done = order.id in state.delivered
    val height = 104.dp
    val width = if (LocalDensity.current.fontScale > 1.2f) 296.dp else 272.dp
    GamePanel(Modifier.width(width).height(height).testTag("order_${order.id}")
        .semantics {
            stateDescription = if (done) "已交付" else if (ready) "可交付" else "进行中"
        }) {
        val cat = catForDay(order.day)
        Image(painterResource(if (cat in setOf("daju", "ali", "huzi")) R.drawable.ui_order_cat_orange else R.drawable.ui_order_cat_gray),
            "${catName(cat)}的订单", Modifier.align(Alignment.CenterStart).padding(start = 2.dp).size(width = 58.dp, height = 70.dp))
        Column(Modifier.align(Alignment.CenterStart).padding(start = 64.dp, end = 80.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            order.req.forEachIndexed { index, req ->
                val count = if (done) req.count else (progress.getOrNull(index) ?: 0).coerceIn(0, req.count)
                val satisfied = count >= req.count
                val name = content.itemById[req.itemId]?.name ?: req.itemId
                Row(Modifier.fillMaxWidth().heightIn(min = 25.dp).testTag("requirement_${order.id}_$index")
                    .semantics { stateDescription = if (satisfied) "已满足 $count/${req.count}" else "未满足 $count/${req.count}"; contentDescription = "$name $count/${req.count}" },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    GameImage(content.itemById[req.itemId]?.asset ?: "ic_${req.itemId}", null, Modifier.size(25.dp))
                    Text(name, Modifier.widthIn(max = 50.dp), fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Ink)
                    Text("$count/${req.count}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DemandPendingInk, maxLines = 1)
                    if (satisfied) Box(Modifier.size(13.dp).background(ReadyGreen, CircleShape).border(1.dp, ReadyOutline, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Check, null, Modifier.size(10.dp), tint = Leaf) }
                }
            }
        }
        Box(Modifier.align(Alignment.TopEnd).padding(top = 9.dp, end = 8.dp).width(64.dp).height(44.dp)
            .clickable(enabled = !done, role = Role.Button, onClick = onClick).testTag("deliver_${order.id}"), contentAlignment = Alignment.Center) {
            GamePanel(Modifier.fillMaxSize(), if (ready && !done) ReadyGreen else if (done) DemandSatisfied else Color(0xFFFFF0CA), 11.dp) {
                Text(if (done) "已交付" else "交付", Modifier.align(Alignment.Center), fontSize = 12.sp, lineHeight = 16.sp,
                    color = if (ready || done) Leaf else Muted, fontWeight = FontWeight.Bold)
            }
        }
        GameCoin(order.reward, Modifier.align(Alignment.BottomEnd).padding(end = 9.dp, bottom = 8.dp), 15.dp)
    }
}
