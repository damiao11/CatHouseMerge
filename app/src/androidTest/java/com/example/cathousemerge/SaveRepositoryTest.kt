package com.example.cathousemerge

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.cathousemerge.data.SaveRepository
import com.example.cathousemerge.game.ContentParser
import com.example.cathousemerge.game.GameEngine
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SaveRepositoryTest {
    private fun isolatedContext(): Context {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val directory = File(app.cacheDir, "save-test-${System.nanoTime()}").apply { mkdirs() }
        return object : ContextWrapper(app) {
            override fun getFilesDir(): File = directory
        }
    }

    @Test
    fun atomicBackupRestoresAndNewestSnapshotIsWritten() {
        val context = isolatedContext()
        try {
            val content = ContentParser.parse { context.assets.open("content/$it").bufferedReader().use { it.readText() } }
            val state = GameEngine(content).newGame(1_000_000)
            val store = SaveRepository(context, content)
            store.save(state)
            File(context.filesDir, "save.json").copyTo(File(context.filesDir, "save.json.bak"))
            File(context.filesDir, "save.json").writeText("interrupted content")
            assertEquals(state, store.load().state)
            val latest = state.copy(coins = 50)
            store.saveLatest { latest }
            assertEquals(latest, SaveRepository(context, content).load().state)
        } finally {
            context.filesDir.deleteRecursively()
        }
    }

    @Test
    fun damagedContentIsPreservedBeforeStartingAgain() {
        val context = isolatedContext()
        try {
            val content = ContentParser.parse { context.assets.open("content/$it").bufferedReader().use { it.readText() } }
            File(context.filesDir, "save.json").writeText("{broken")
            val restored = SaveRepository(context, content).load()
            assertNull(restored.state)
            assertTrue(restored.recoveredFromDamage)
            val archive = context.filesDir.listFiles()!!.single { it.name.startsWith("save_broken_") }
            assertEquals("{broken", archive.readText())
        } finally {
            context.filesDir.deleteRecursively()
        }
    }
}
