package com.example.cathousemerge

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cathousemerge.data.SaveRepository
import com.example.cathousemerge.data.SettingsRepository
import com.example.cathousemerge.game.ContentParser
import com.example.cathousemerge.game.GameAction
import com.example.cathousemerge.game.GameContent
import com.example.cathousemerge.game.GameEffect
import com.example.cathousemerge.game.GameEngine
import com.example.cathousemerge.game.GameSettings
import com.example.cathousemerge.game.GameState
import com.example.cathousemerge.game.MAX_ENERGY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GameUiState(
    val content: GameContent? = null,
    val game: GameState? = null,
    val settings: GameSettings = GameSettings(),
    val message: String? = null,
    val messageId: Long = 0,
    val error: String? = null,
)

@OptIn(FlowPreview::class)
class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val mutableUi = MutableStateFlow(GameUiState())
    val ui = mutableUi.asStateFlow()
    private val mutableEffects = MutableSharedFlow<GameEffect>(extraBufferCapacity = 8)
    val effects = mutableEffects.asSharedFlow()
    private val saves = MutableStateFlow<GameState?>(null)
    private val settingsRepository = SettingsRepository(application)
    private var engine: GameEngine? = null
    private var repository: SaveRepository? = null

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings -> mutableUi.update { it.copy(settings = settings) } }
        }
        viewModelScope.launch {
            try {
                val content = withContext(Dispatchers.IO) {
                    ContentParser.parse { filename -> application.assets.open("content/$filename").bufferedReader().use { it.readText() } }
                }
                val gameEngine = GameEngine(content)
                val saveRepository = SaveRepository(application, content)
                val loaded = withContext(Dispatchers.IO) { saveRepository.load() }
                val now = System.currentTimeMillis()
                val game = gameEngine.recoverEnergy(loaded.state ?: gameEngine.newGame(now), now)
                engine = gameEngine
                repository = saveRepository
                mutableUi.update { it.copy(content = content, game = game) }
                saves.value = game
                if (loaded.recoveredFromDamage) showMessage("存档读取失败，已保留原文件并开启新旅程")
            } catch (error: Exception) {
                mutableUi.update { it.copy(error = error.message ?: "游戏内容加载失败") }
            }
        }
        viewModelScope.launch {
            saves.filterNotNull().debounce(350).collect { state ->
                try {
                    withContext(Dispatchers.IO) {
                        // Read the newest state inside the serialized IO write to avoid an older queued write.
                        repository?.saveLatest { saves.value ?: state }
                    }
                } catch (error: Exception) {
                    showMessage("保存失败，请检查设备剩余空间")
                }
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(1_000)
                refreshEnergy()
            }
        }
    }

    fun dispatch(action: GameAction) {
        val state = mutableUi.value.game ?: return
        val result = engine?.reduce(state, action) ?: return
        updateGame(result.state)
        result.message?.let(::showMessage)
        result.effect?.let(mutableEffects::tryEmit)
    }

    fun changeSettings(settings: GameSettings) {
        mutableUi.update { it.copy(settings = settings) }
        viewModelScope.launch {
            try {
                settingsRepository.save(settings)
            } catch (error: Exception) {
                showMessage("设置暂时无法保存")
            }
        }
    }

    fun reset() {
        val state = engine?.newGame(System.currentTimeMillis()) ?: return
        updateGame(state)
        flushSave()
        mutableUi.update { it.copy(message = null, messageId = it.messageId + 1) }
    }

    fun clearMessage() { mutableUi.update { it.copy(message = null) } }

    fun refreshEnergy() {
        val state = mutableUi.value.game ?: return
        if (state.energy >= MAX_ENERGY) return
        val recovered = engine?.recoverEnergy(state, System.currentTimeMillis()) ?: return
        updateGame(recovered)
    }

    fun flushSave() {
        val state = mutableUi.value.game ?: return
        try {
            repository?.saveLatest { saves.value ?: state }
        } catch (error: Exception) {
            showMessage("保存失败，请检查设备剩余空间")
        }
    }

    private fun updateGame(state: GameState) {
        if (state == mutableUi.value.game) return
        mutableUi.update { it.copy(game = state) }
        saves.value = state
    }

    private fun showMessage(message: String) {
        mutableUi.update { it.copy(message = message, messageId = it.messageId + 1) }
    }
}
