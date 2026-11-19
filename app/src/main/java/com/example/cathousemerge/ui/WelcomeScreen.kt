package com.example.cathousemerge.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cathousemerge.BuildConfig
import com.example.cathousemerge.R
import com.example.cathousemerge.game.GameState

private val CoverFont = FontFamily(Font(R.font.leeeaf_hei))

@Composable
internal fun WelcomeScreen(state: GameState, reducedMotion: Boolean, onStart: () -> Unit) {
    val fontScale = LocalDensity.current.fontScale
    val drift = if (reducedMotion) 0f else {
        val transition = rememberInfiniteTransition(label = "coverAtmosphere")
        val offset by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(8_000, easing = LinearEasing), RepeatMode.Reverse), label = "leafDrift")
        offset
    }
    BoxWithConstraints(Modifier.fillMaxSize().testTag("welcome")) {
        val coverWidth = maxWidth
        val coverHeight = maxHeight
        SceneImage("scenes/scene_splash.webp", "一只猫独自守着旧屋", Modifier.fillMaxSize())
        Icon(Icons.Outlined.Cloud, null, Modifier.offset(x = coverWidth * 0.62f, y = coverHeight * 0.05f).size(72.dp), tint = CardPaper.copy(alpha = 0.26f))
        Icon(Icons.Outlined.Cloud, null, Modifier.offset(x = coverWidth * 0.08f, y = coverHeight * 0.27f).size(44.dp), tint = CardPaper.copy(alpha = 0.18f))
        listOf(Triple(0.08f, 0.14f, -22f), Triple(0.86f, 0.25f, 25f), Triple(0.12f, 0.38f, -38f)).forEachIndexed { index, (x, y, turn) ->
            Icon(Icons.Outlined.Eco, null,
                Modifier.offset(x = coverWidth * x + (drift * 5).dp, y = coverHeight * y + (drift * (6 + index * 2)).dp)
                    .size(if (index == 1) 20.dp else 25.dp).graphicsLayer { rotationZ = turn + drift * 8f },
                tint = Color(0xFF9E985E).copy(alpha = 0.55f))
        }
        CarvedTitle(Modifier.align(Alignment.TopCenter).padding(top = coverHeight * 0.115f),
            (coverWidth.value * 0.176f / fontScale.coerceAtLeast(1f)).coerceIn(35f, 72f))
        GameImage("ic_fabric_1", null, Modifier.offset(x = coverWidth * 0.68f, y = coverHeight * 0.48f).size(30.dp)
            .graphicsLayer { rotationZ = -12f }.clearAndSetSemantics { })
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Keep the note and action in the same flow: short screens or larger fonts cannot overlap them.
            Box(Modifier.align(Alignment.Start).padding(bottom = 8.dp).width(minOf(coverWidth * 0.72f, 292.dp))
                .woodPlaque(light = true).padding(horizontal = 18.dp, vertical = 15.dp)) {
                Text("一个旧屋，六只猫。\n把日子，一点点修回来。", fontFamily = CoverFont,
                    fontSize = (coverWidth.value / 20).coerceIn(16f, 20f).sp, lineHeight = 29.sp, color = Ink,
                    modifier = Modifier.testTag("welcome_note"))
            }
            WoodenStartButton(
                if (state.day == 1 && state.delivered.isEmpty() && state.board.all { it == null }) "推开猫屋的门" else "继续第 ${state.day} 天",
                reducedMotion, onStart,
            )
            Text("第 ${state.day} 天 · v${BuildConfig.VERSION_NAME}", fontFamily = CoverFont, fontSize = 12.sp,
                color = Ink, modifier = Modifier.background(Paper.copy(alpha = 0.88f), RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 3.dp))
        }
    }
}

@Composable
private fun CarvedTitle(modifier: Modifier, fontSize: Float) {
    val style = TextStyle(fontFamily = CoverFont, fontSize = fontSize.sp)
    Box(modifier.testTag("welcome_title")) {
        Text("猫屋合合", Modifier.offset(y = 4.dp).clearAndSetSemantics { }, style = style, color = Color(0xFF704A32), maxLines = 1)
        listOf(-2 to -2, 0 to -2, 2 to -2, -2 to 0, 2 to 0, -2 to 2, 0 to 2, 2 to 2).forEach { (x, y) ->
            Text("猫屋合合", Modifier.offset(x = x.dp, y = y.dp).clearAndSetSemantics { }, style = style, color = Color(0xFF8E603B), maxLines = 1)
        }
        Text("猫屋合合", style = style, color = Color(0xFFFFDDA4), maxLines = 1)
    }
}

@Composable
private fun WoodenStartButton(label: String, reducedMotion: Boolean, onStart: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val breath = if (reducedMotion) 1f else {
        val transition = rememberInfiniteTransition(label = "woodButton")
        val scale by transition.animateFloat(1f, 1.012f, infiniteRepeatable(tween(2_000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "doorBreath")
        scale
    }
    val pressScale by animateFloatAsState(if (pressed && !reducedMotion) 0.98f else 1f, tween(100), label = "doorPress")
    Row(Modifier.fillMaxWidth().heightIn(min = 76.dp).graphicsLayer { scaleX = breath * pressScale; scaleY = breath * pressScale }
        .woodPlaque(light = false).clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onStart)
        .testTag("start_game").padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
        Icon(Icons.Outlined.Pets, null, Modifier.size(24.dp), tint = Color(0xFFFFDDA4))
        Text(label, Modifier.weight(1f, fill = false), fontFamily = CoverFont, fontSize = 24.sp,
            color = CardPaper, style = TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Ink.copy(alpha = 0.3f), Offset(0f, 2f), 2f)))
        Icon(Icons.Outlined.Key, null, Modifier.size(24.dp), tint = Color(0xFFFFDDA4))
    }
}

/** Code-native wood finish: the existing illustration remains the cover's visual authority. */
private fun Modifier.woodPlaque(light: Boolean): Modifier {
    val shape = RoundedCornerShape(12.dp)
    return shadow(6.dp, shape, ambientColor = Ink.copy(alpha = 0.18f), spotColor = Ink.copy(alpha = 0.18f))
        .clip(shape).background(Brush.verticalGradient(if (light) listOf(Color(0xFFF6E9D0), Color(0xFFE9D4B1)) else listOf(Color(0xFFBA8652), Color(0xFFA26A3E))))
        .drawBehind {
            val inset = 1.5.dp.toPx()
            drawRoundRect(DarkWood, Offset(inset, inset), Size(size.width - inset * 2, size.height - inset * 2), CornerRadius(11.dp.toPx()), style = Stroke(2.dp.toPx()))
            val inner = 5.dp.toPx()
            drawRoundRect(Color(0xFFFFE4B5).copy(alpha = if (light) 0.65f else 0.48f), Offset(inner, inner), Size(size.width - inner * 2, size.height - inner * 2), CornerRadius(8.dp.toPx()), style = Stroke(1.dp.toPx()))
            listOf(0.16f, 0.63f, 0.87f).forEachIndexed { i, fraction ->
                val y = size.height * fraction
                val grain = Path().apply { moveTo(10.dp.toPx(), y); cubicTo(size.width * 0.28f, y - (i + 1) * 3.dp.toPx(), size.width * 0.67f, y + 4.dp.toPx(), size.width - 10.dp.toPx(), y - 1.dp.toPx()) }
                drawPath(grain, DarkWood.copy(alpha = if (light) 0.07f else 0.15f), style = Stroke(0.7.dp.toPx()))
            }
            if (!light) listOf(Offset(11.dp.toPx(), 12.dp.toPx()), Offset(size.width - 11.dp.toPx(), size.height - 12.dp.toPx())).forEach { nail ->
                drawCircle(Color(0xFF87532F), 2.dp.toPx(), nail)
                drawCircle(Color(0xFFE9BE87), 0.8.dp.toPx(), nail - Offset(0.5.dp.toPx(), 0.5.dp.toPx()))
            }
        }
}
