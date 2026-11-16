package com.example.cathousemerge.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cathousemerge.game.MAX_ENERGY
import com.example.cathousemerge.R

internal val JournalMuted = Color(0xFF786B58)
internal val JournalWoodInk = Color(0xFF875B34)

@Composable
internal fun JournalPaper(modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.ui_journal_paper), null, modifier, contentScale = ContentScale.FillBounds)
}

@Composable
internal fun JournalCoins(coins: Int, modifier: Modifier = Modifier) {
    Surface(modifier, shape = CircleShape, color = CardPaper.copy(alpha = 0.94f)) {
        Box(Modifier.heightIn(min = 40.dp).padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
            CoinAmount(coins)
        }
    }
}

@Composable
internal fun JournalEnergy(energy: Int, countdown: Int) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(onClick = { expanded = true }, shape = CircleShape, color = CardPaper.copy(alpha = 0.9f)) {
            Row(Modifier.heightIn(min = 44.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Bolt, "体力", Modifier.size(18.dp), tint = JournalWoodInk)
                Column {
                    Text(energy.toString(), style = MaterialTheme.typography.titleSmall, color = Ink)
                    Text(if (energy >= MAX_ENERGY) "已满" else "%02d:%02d".format(countdown / 60, countdown % 60), style = MaterialTheme.typography.labelMedium, fontSize = 10.sp, color = JournalMuted)
                }
            }
        }
        DropdownMenu(expanded, { expanded = false }) {
            Text("体力 $energy / $MAX_ENERGY", Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            Text("每 2 分钟恢复 1 点", Modifier.padding(horizontal = 20.dp, vertical = 8.dp), style = MaterialTheme.typography.bodySmall)
        }
    }
}
