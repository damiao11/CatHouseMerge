package com.example.cathousemerge.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cathousemerge.R
import com.example.cathousemerge.game.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal val Paper = Color(0xFFFBF6EC)
internal val CardPaper = Color(0xFFFFFDF8)
internal val Ink = Color(0xFF3E3226)
internal val Muted = Color(0xFF8A7A66)
internal val Wood = Color(0xFFD9A15B)
internal val DarkWood = Color(0xFFA9703C)
internal val Clay = Color(0xFFE2703A)
internal val Leaf = Color(0xFF55703C)
internal val Line = Color(0xFFE8DCC8)
internal val ErrorInk = Color(0xFFAD463C)
internal val BodyFont = FontFamily(Font(R.font.noto_sans_sc))
internal val TitleFont = FontFamily(Font(R.font.noto_serif_sc))

@Composable
internal fun CatTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Clay, onPrimary = CardPaper, secondary = DarkWood,
            background = Paper, onBackground = Ink, surface = CardPaper,
            onSurface = Ink, onSurfaceVariant = Muted, outline = Line,
            error = ErrorInk, secondaryContainer = Color(0xFFF0E1C9),
        ),
        typography = Typography(
            headlineLarge = androidx.compose.ui.text.TextStyle(fontFamily = TitleFont, fontSize = 32.sp, fontWeight = FontWeight.Bold),
            headlineMedium = androidx.compose.ui.text.TextStyle(fontFamily = TitleFont, fontSize = 26.sp, fontWeight = FontWeight.SemiBold),
            titleLarge = androidx.compose.ui.text.TextStyle(fontFamily = TitleFont, fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
            titleMedium = androidx.compose.ui.text.TextStyle(fontFamily = BodyFont, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
            titleSmall = androidx.compose.ui.text.TextStyle(fontFamily = BodyFont, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            bodyLarge = androidx.compose.ui.text.TextStyle(fontFamily = BodyFont, fontSize = 16.sp, lineHeight = 26.sp),
            bodyMedium = androidx.compose.ui.text.TextStyle(fontFamily = BodyFont, fontSize = 14.sp, lineHeight = 22.sp),
            bodySmall = androidx.compose.ui.text.TextStyle(fontFamily = BodyFont, fontSize = 12.sp, lineHeight = 18.sp),
            labelLarge = androidx.compose.ui.text.TextStyle(fontFamily = BodyFont, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            labelMedium = androidx.compose.ui.text.TextStyle(fontFamily = BodyFont, fontSize = 12.sp, fontWeight = FontWeight.Medium),
        ),
        shapes = Shapes(medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(24.dp)),
        content = content,
    )
}

@Composable
internal fun GameImage(asset: String, description: String?, modifier: Modifier = Modifier) {
    val id = DrawableAssets[asset]
    if (id != null) Image(painterResource(id), description, modifier, contentScale = ContentScale.Fit)
    else Icon(Icons.Outlined.Pets, description, modifier, tint = DarkWood)
}

@Composable
internal fun SceneImage(path: String, description: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, path) {
        value = withContext(Dispatchers.IO) {
            runCatching { context.assets.open(path).use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }.getOrNull()
        }
    }
    Box(modifier.background(Paper)) {
        bitmap?.let { Image(it, description, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    }
}

@Composable
internal fun CatAvatar(cat: String, modifier: Modifier = Modifier) {
    GameImage("cat_$cat", catName(cat), modifier.clip(CircleShape).background(Paper).border(1.dp, Line, CircleShape))
}

@Composable
internal fun CoinAmount(coins: Int, light: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(Icons.Outlined.MonetizationOn, null, Modifier.size(18.dp), tint = if (light) Wood else DarkWood)
        Text(coins.toString(), style = MaterialTheme.typography.titleSmall, color = if (light) CardPaper else Ink)
    }
}

@Composable
internal fun PageHeader(title: String, onBack: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回") }
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
        trailing()
    }
}

@Composable
internal fun MainButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick, modifier.heightIn(min = 56.dp), enabled = enabled, shape = CircleShape, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)) {
        Text(text, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun SmallAction(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    TextButton(onClick, modifier.heightIn(min = 48.dp)) {
        Icon(icon, null, Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

internal fun roomName(room: String): String = when (room) {
    "porch" -> "门廊"
    "entry" -> "玄关"
    "living" -> "客厅"
    "bedroom" -> "卧室"
    "kitchen" -> "厨房"
    "yard" -> "院子"
    "attic" -> "阁楼"
    else -> "全屋"
}

internal fun catName(cat: String): String = when (cat) {
    "daju" -> "大橘"
    "huahua" -> "花花"
    "tuanzi" -> "团子"
    "meiqiu" -> "煤球"
    "ali" -> "阿狸"
    "huzi" -> "虎子"
    "player" -> "小满"
    else -> "旁白"
}

internal fun catForDay(day: Int): String = when {
    day <= 6 -> "daju"
    day <= 9 -> "huahua"
    day <= 12 -> "tuanzi"
    day <= 15 -> "meiqiu"
    day <= 18 -> "ali"
    else -> "huzi"
}

internal fun pieceName(piece: Piece, content: GameContent): String =
    if (piece.kind == PieceKind.GENERATOR) "${content.chainById[piece.chain]?.name.orEmpty()}工具台"
    else content.itemById[piece.itemId]?.name ?: "物品"
