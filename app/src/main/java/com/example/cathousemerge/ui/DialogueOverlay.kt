package com.example.cathousemerge.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cathousemerge.game.DialogueLine
import com.example.cathousemerge.game.DialogueScene
import kotlinx.coroutines.delay

@Composable
internal fun DialogueOverlay(
    id: String,
    scene: DialogueScene,
    room: String,
    roomRepaired: Boolean,
    reducedMotion: Boolean,
    onFinish: () -> Unit,
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val textStyle = TextStyle(fontFamily = BodyFont, fontSize = 20.sp, lineHeight = 32.sp, color = Ink)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val bodyWidthPx = with(density) { (maxWidth - 80.dp).roundToPx().coerceAtLeast(1) }
        // Page using the actual width and font scale, so no text is lost behind maxLines.
        val pages = remember(id, scene, bodyWidthPx, density.fontScale) {
            buildList {
                scene.lines.forEach { source ->
                    var remaining = source.text
                    while (remaining.isNotEmpty()) {
                        val layout = measurer.measure(remaining, textStyle, maxLines = 3, constraints = Constraints(maxWidth = bodyWidthPx))
                        val end = if (layout.hasVisualOverflow) layout.getLineEnd(layout.lineCount - 1).coerceAtLeast(1) else remaining.length
                        add(DialogueLine(source.who, remaining.take(end)))
                        remaining = remaining.drop(end)
                    }
                }
            }
        }
        var page by rememberSaveable(id, bodyWidthPx, density.fontScale) { mutableIntStateOf(0) }
        val line = pages.getOrNull(page) ?: return@BoxWithConstraints
        var visible by remember(id, page, bodyWidthPx, density.fontScale) { mutableIntStateOf(if (reducedMotion) line.text.length else 0) }
        LaunchedEffect(id, page, bodyWidthPx, reducedMotion) {
            if (reducedMotion) visible = line.text.length
            else while (visible < line.text.length) { delay(33); visible = (visible + 1).coerceAtMost(line.text.length) }
        }
        val advance: () -> Unit = {
            if (visible < line.text.length) visible = line.text.length
            else if (page < pages.lastIndex) page++
            else onFinish()
        }
        val cardHeight = with(density) { 96.sp.toDp() } + 88.dp
        Box(Modifier.fillMaxSize().clickable(onClick = advance).testTag("dialogue_next")) {
            SceneImage("rooms/room_${room}_${if (roomRepaired) "after" else "before"}.webp", "当前房间", Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.60f)))
            TextButton(onFinish, Modifier.align(Alignment.TopEnd).padding(12.dp).testTag("dialogue_skip")) {
                Text("略过这段", color = CardPaper)
            }
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
                .height(cardHeight + 112.dp)) {
                Surface(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(cardHeight)
                        .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = Ink.copy(alpha = 0.18f), spotColor = Ink.copy(alpha = 0.18f))
                        .testTag("dialogue_card"),
                    color = CardPaper, shape = RoundedCornerShape(24.dp), border = BorderStroke(4.dp, Wood),
                ) {
                    Box {
                        Text(line.text.take(visible), Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 32.dp)
                            .testTag("dialogue_text"), style = textStyle, maxLines = 3)
                        IconButton(advance, Modifier.align(Alignment.BottomEnd).padding(end = 4.dp, bottom = 4.dp)
                            .size(48.dp).testTag("dialogue_advance")) {
                            Icon(
                                if (page == pages.lastIndex) Icons.Outlined.ArrowDropDown else Icons.Outlined.FastForward,
                                if (visible < line.text.length) "显示完整对话" else if (page == pages.lastIndex) "结束对话" else "下一句",
                                Modifier.size(24.dp), tint = Color(0xFFE8D2B0),
                            )
                        }
                    }
                }
                if (line.who != "narrator" && line.who != "player") {
                    GameImage("cat_${line.who}", catName(line.who), Modifier.offset(x = 8.dp).size(128.dp).testTag("dialogue_avatar"))
                }
                val ribbon = remember { GenericShape { size, _ ->
                    moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width - size.height * 0.25f, size.height / 2)
                    lineTo(size.width, size.height); lineTo(0f, size.height); close()
                } }
                Surface(Modifier.offset(x = (-4).dp, y = 92.dp).testTag("dialogue_name"), color = Color(0xFF7FA05A), shape = ribbon) {
                    Text(catName(line.who), Modifier.padding(horizontal = 24.dp, vertical = 7.dp), fontFamily = BodyFont,
                        fontSize = 18.sp, color = Color.White)
                }
            }
        }
    }
}
