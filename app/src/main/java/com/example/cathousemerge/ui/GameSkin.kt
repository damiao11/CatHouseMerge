package com.example.cathousemerge.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cathousemerge.game.*

internal val BoardBlue = Color(0xFF91C2D0)
internal val BoardCell = Color(0xFFE5EFF0)
internal val ReadyGreen = Color(0xFFBFE584)
internal val ReadyOutline = Color(0xFF71913E)
internal val SelectionYellow = Color(0xFFE69B22)

/** The approved cream surface, caramel outline and slightly deeper lower edge. */
@Composable
internal fun GamePanel(modifier: Modifier = Modifier, color: Color = CardPaper, radius: Dp = 16.dp, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.background(DarkWood, RoundedCornerShape(radius))
        .padding(start = 2.dp, top = 2.dp, end = 2.dp, bottom = 4.dp)
        .clip(RoundedCornerShape((radius - 2.dp).coerceAtLeast(0.dp))).background(color), content = content)
}

@Composable
internal fun GameCoin(value: Int, modifier: Modifier = Modifier, size: Dp = 18.dp) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        GameCoinIcon(size = size)
        Text(value.toString(), color = Ink, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
internal fun GameCoinIcon(modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Box(modifier.size(size).background(Wood, CircleShape).border(1.5.dp, DarkWood, CircleShape), contentAlignment = Alignment.Center) {
        Text("¥", color = Ink, fontSize = (size.value * .57f).sp, lineHeight = size.value.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
internal fun GameSquareButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.clickable(role = Role.Button, onClickLabel = label, onClick = onClick).semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        GamePanel(Modifier.fillMaxWidth().height(40.dp), radius = 12.dp) {
            Icon(icon, null, Modifier.align(Alignment.Center).size(21.dp), tint = Ink)
        }
    }
}

@Composable
internal fun GamePrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, done: Boolean = false) {
    GamePanel(modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onClick), if (done) ReadyGreen else Wood, 14.dp) {
        Text(text, Modifier.align(Alignment.Center).padding(horizontal = 12.dp, vertical = 9.dp), color = if (done) Leaf else Ink,
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun GameBackground(modifier: Modifier = Modifier) {
    Box(modifier.background(Paper)) {
        Icon(Icons.Outlined.Pets, null, Modifier.align(Alignment.TopStart).padding(start = 76.dp, top = 5.dp).size(22.dp), tint = Leaf.copy(alpha = .08f))
        Icon(Icons.Outlined.Pets, null, Modifier.align(Alignment.CenterEnd).padding(end = 12.dp).size(21.dp), tint = Leaf.copy(alpha = .07f))
        Icon(Icons.Outlined.Pets, null, Modifier.align(Alignment.BottomStart).padding(start = 48.dp, bottom = 5.dp).size(18.dp), tint = Leaf.copy(alpha = .08f))
    }
}

internal fun itemDescription(piece: Piece, content: GameContent): String = when {
    piece.kind == PieceKind.GENERATOR -> "点击产出材料，拖动可调整位置。"
    piece.itemId == "metal_1" -> "把松动的木件重新钉牢。"
    piece.itemId == "wood_3" -> "平整结实，适合修补旧屋。"
    piece.itemId == "wood_4" -> "收好零碎，也收好生活。"
    piece.itemId == "light_3" -> "一盏暖灯，照亮安静的角落。"
    piece.itemId == "relic_4" -> "停住的指针，藏着一段旧时光。"
    piece.chain == "wood" -> "结实的木料，慢慢修好旧屋。"
    piece.chain == "metal" -> "小小零件，让连接更牢靠。"
    piece.chain == "fabric" -> "柔软的织物，给房间添点温暖。"
    piece.chain == "light" -> "留一盏暖灯，等猫咪回家。"
    piece.chain == "ceramic" -> "细细打磨，把日常过得精致。"
    piece.chain == "plant" -> "一抹新绿，让窗边有了生机。"
    else -> "旧物里的故事，等你慢慢发现。"
}
