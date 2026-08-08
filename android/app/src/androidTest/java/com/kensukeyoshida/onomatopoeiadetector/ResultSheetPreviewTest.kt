package com.kensukeyoshida.onomatopoeiadetector

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kensukeyoshida.onomatopoeiadetector.data.DictionaryLoader
import com.kensukeyoshida.onomatopoeiadetector.engine.OnoEngine
import com.kensukeyoshida.onomatopoeiadetector.ui.AppViewModel
import com.kensukeyoshida.onomatopoeiadetector.ui.ResultSheet
import com.kensukeyoshida.onomatopoeiadetector.ui.theme.MangaTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 判定結果シートを実機・エミュレータ上に表示する。音声入力を伴わずに見た目を確認するための足場。
 * `SCREENSHOT_HOLD_MS` の間だけ表示を保つので、その間に `adb exec-out screencap` で撮影できる。
 */
@RunWith(AndroidJUnit4::class)
class ResultSheetPreviewTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun showsResultSheet() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val application = context.applicationContext as Application
        val engine = OnoEngine(DictionaryLoader.load(context))
        val result = runBlocking { engine.evaluate("ふわふわ") }

        assertEquals(5, result.score)

        composeRule.setContent {
            MangaTheme {
                ResultSheet(
                    result = result,
                    viewModel = AppViewModel(application),
                    onDismiss = {}
                )
            }
        }
        composeRule.waitForIdle()

        val holdMillis = InstrumentationRegistry.getArguments()
            .getString("screenshotHoldMs")
            ?.toLongOrNull()
            ?: 0L
        if (holdMillis > 0) Thread.sleep(holdMillis)
    }
}
