package com.example.cathousemerge

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cathousemerge.game.GameEffect
import com.example.cathousemerge.ui.CatHouseApp

class MainActivity : ComponentActivity() {
    private var gameViewModel: GameViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val model: GameViewModel = viewModel()
            gameViewModel = model
            val ui by model.ui.collectAsStateWithLifecycle()
            val content = ui.content
            val state = ui.game
            val sound = remember(ui.settings.soundVolume) {
                if (ui.settings.soundVolume <= 0f) null else runCatching {
                    ToneGenerator(AudioManager.STREAM_MUSIC, (ui.settings.soundVolume * 70).toInt())
                }.getOrNull()
            }
            DisposableEffect(sound) { onDispose { sound?.release() } }
            LaunchedEffect(model, sound, ui.settings.haptics) {
                model.effects.collect { effect ->
                    val tone = when (effect) {
                        GameEffect.SPAWN -> ToneGenerator.TONE_PROP_BEEP
                        GameEffect.MERGE -> ToneGenerator.TONE_PROP_ACK
                        GameEffect.DELIVER, GameEffect.BUILD, GameEffect.DAY -> ToneGenerator.TONE_PROP_BEEP2
                    }
                    sound?.startTone(tone, if (effect == GameEffect.SPAWN) 35 else 90)
                    if (ui.settings.haptics && effect != GameEffect.SPAWN) {
                        val feedback = if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.CLOCK_TICK
                        window.decorView.performHapticFeedback(feedback)
                    }
                }
            }
            if (content != null && state != null) {
                CatHouseApp(
                    content = content,
                    state = state,
                    settings = ui.settings,
                    message = ui.message,
                    messageId = ui.messageId,
                    onAction = model::dispatch,
                    onSettings = model::changeSettings,
                    onReset = model::reset,
                    onMessageShown = model::clearMessage,
                )
            } else {
                MaterialTheme {
                    Column(
                        Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (ui.error == null) CircularProgressIndicator()
                        Text(ui.error ?: "正在打开猫屋…", Modifier.padding(top = 16.dp))
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        gameViewModel?.refreshEnergy()
    }

    override fun onPause() {
        gameViewModel?.flushSave()
        super.onPause()
    }
}
