package com.example.cathousemerge

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FullBleedCoverTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun coverFillsThePhysicalWindowWithoutGreenSystemBarBands() {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("welcome_background", true).fetchSemanticsNodes().isNotEmpty() }
        compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription("一只猫独自守着旧屋", true).fetchSemanticsNodes().isNotEmpty() }
        val cover = compose.onNodeWithTag("welcome", true).fetchSemanticsNode().boundsInRoot
        val background = compose.onNodeWithTag("welcome_background", true).fetchSemanticsNode().boundsInRoot
        val title = compose.onNodeWithTag("welcome_title", true)
        title.assertContentDescriptionEquals("猫屋合合").assertIsDisplayed()
        compose.onNodeWithTag("start_game").assertIsDisplayed()
        compose.onNodeWithText(" · v${BuildConfig.VERSION_NAME}", substring = true).assertIsDisplayed()
        assertEquals(cover, background)
        assertTrue(title.fetchSemanticsNode().boundsInRoot.width >= cover.width * 0.9f)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val screenshot = instrumentation.uiAutomation.takeScreenshot()
        assertNotNull(screenshot)
        assertTrue("Cover must reach behind both system bars", cover.height >= screenshot.height - 1f)
        fun isGreenBand(pixel: Int): Boolean =
            kotlin.math.abs(Color.red(pixel) - 220) < 8 && kotlin.math.abs(Color.green(pixel) - 232) < 8 && kotlin.math.abs(Color.blue(pixel) - 156) < 8
        assertFalse("No green band above the illustration", isGreenBand(screenshot.getPixel(screenshot.width / 4, 8)))
        assertFalse("No green band below the illustration", isGreenBand(screenshot.getPixel(screenshot.width / 4, screenshot.height - 8)))
        val file = File(instrumentation.targetContext.getExternalFilesDir("ui-review-20261126"), "welcome-full-screen.png")
        file.parentFile!!.mkdirs()
        file.outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        screenshot.recycle()
    }
}
