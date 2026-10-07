package com.miaomiaopounce

import android.view.*
import android.widget.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import androidx.test.platform.app.InstrumentationRegistry
import com.miaomiaopounce.game.*
import com.miaomiaopounce.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.Before
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppAcceptanceTest {
    @get:Rule val activityRule = ActivityTestRule(MainActivity::class.java, true, false)
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun ui(block: () -> Unit) { instrumentation.runOnMainSync(block); instrumentation.waitForIdleSync() }
    private fun find(tag: String): View? = activityRule.activity.window.decorView.findViewWithTag(tag)
    private fun click(tag: String) = ui { assertNotNull("Missing $tag", find(tag)); find(tag)!!.performClick() }
    @Before fun reset() {
        instrumentation.targetContext.getSharedPreferences("pounce", android.content.Context.MODE_PRIVATE).edit().clear().commit()
        activityRule.launchActivity(null)
    }
    private fun dialogButton(label: String) {
        ui {
            val activity = activityRule.activity
            val dialogField = MainActivity::class.java.getDeclaredField("ownerDialog").apply { isAccessible = true }
            val dialog = dialogField.get(activity) as android.app.AlertDialog
            if (label == "正确答案") {
                assertTrue("Answer list must actually be visible", dialog.listView.isShown)
                assertNotNull(dialog.listView.getChildAt(1))
                dialog.listView.performItemClick(dialog.listView.getChildAt(1), 1, 1)
            }
            else dialog.getButton(if (label == "继续玩" || label == "开始") android.content.DialogInterface.BUTTON_POSITIVE else android.content.DialogInterface.BUTTON_NEGATIVE).performClick()
        }
    }
    private fun beginGame() {
        click("startButton"); dialogButton("开始")
    }
    @Test fun allThemesRenderAndPawTapCaptures() {
        for (theme in PreyTheme.entries) {
            click("theme_${theme.name}"); beginGame()
            ui {
                val view = find("gameCanvas") as GameView
                view.pause()
                assertTrue(view.width > 0); assertEquals(theme, view.engine.settings.theme)
                val prey = view.engine.prey.first { it.visible }
                val x = prey.x; val y = prey.y; view.resume()
                val now = android.os.SystemClock.uptimeMillis()
                val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0)
                view.dispatchTouchEvent(down); down.recycle()
                val up = MotionEvent.obtain(now, now + 10, MotionEvent.ACTION_UP, x, y, 0)
                view.dispatchTouchEvent(up); up.recycle()
                assertTrue(view.engine.captures >= 1)
            }
            ui { activityRule.activity.onBackPressedDispatcher.onBackPressed() }
            dialogButton("正确答案"); dialogButton("结束")
            assertNotNull(find("resultHome")); click("resultHome")
        }
    }
    @Test fun settingsPersistAcrossActivityRecreation() {
        click("gentlePreset"); click("theme_FISH"); click("countPlus"); click("duration_0")
        activityRule.finishActivity(); activityRule.launchActivity(null)
        val saved = com.miaomiaopounce.data.SettingsStore(instrumentation.targetContext).load()
        assertEquals(PreyTheme.FISH, saved.theme); assertEquals(2, saved.count)
        assertEquals(Pace.GENTLE, saved.pace); assertFalse(saved.sound); assertEquals(1, saved.minutes)
        ui { assertEquals("2", (find("countValue") as TextView).text.toString()) }
    }
    @Test fun touchDiagnosticCountsAndClears() {
        click("touchTestButton")
        ui {
            val view = find("touchCanvas") as TouchTestView
            val now = android.os.SystemClock.uptimeMillis()
            val event = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, view.width / 2f, view.height / 2f, 0)
            view.dispatchTouchEvent(event); event.recycle()
            assertEquals(1, view.touchCount); assertEquals(1, view.targetHits)
        }
        click("touchClear")
        ui { assertEquals(0, (find("touchCanvas") as TouchTestView).touchCount) }
        click("touchBack"); assertNotNull(find("startButton"))
    }
    @Test fun naturalTimeoutShowsSummaryOnce() {
        click("duration_0"); beginGame()
        ui { val v = find("gameCanvas") as GameView; v.engine.update(61f) }
        // A frame invokes the real finish callback.
        Thread.sleep(250); instrumentation.waitForIdleSync()
        assertNotNull(find("resultHome"))
        assertEquals(1, com.miaomiaopounce.data.SettingsStore(instrumentation.targetContext).sessions)
        click("resultHome")
        assertEquals(1, com.miaomiaopounce.data.SettingsStore(instrumentation.targetContext).sessions)
    }
    @Test fun heldCornerOpensGuardAndRequiresCorrectAnswer() {
        beginGame()
        ui {
            val v = find("gameCanvas") as GameView
            val now = android.os.SystemClock.uptimeMillis()
            val e = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 10f, 10f, 0)
            v.dispatchTouchEvent(e); e.recycle()
        }
        Thread.sleep(1800); instrumentation.waitForIdleSync()
        val before = arrayOf(0f)
        ui { before[0] = (find("gameCanvas") as GameView).engine.elapsedSeconds }
        Thread.sleep(120)
        ui { assertEquals(before[0], (find("gameCanvas") as GameView).engine.elapsedSeconds, 0f) }
        dialogButton("正确答案"); dialogButton("继续玩")
        Thread.sleep(120)
        ui { assertTrue((find("gameCanvas") as GameView).engine.elapsedSeconds > before[0]) }
    }
    @Test fun twoPawsCaptureIndependentlyThroughNativeTouchEvents() {
        click("gentlePreset"); click("countPlus"); beginGame()
        ui {
            val v = find("gameCanvas") as GameView
            v.pause()
            val a = v.engine.prey[0]; val b = v.engine.prey[1]
            a.x = v.width * 0.30f; a.y = v.height * 0.40f
            b.x = v.width * 0.70f; b.y = v.height * 0.60f
            val properties = arrayOf(
                MotionEvent.PointerProperties().apply { id = 7; toolType = MotionEvent.TOOL_TYPE_FINGER },
                MotionEvent.PointerProperties().apply { id = 41; toolType = MotionEvent.TOOL_TYPE_FINGER }
            )
            val coords = arrayOf(
                MotionEvent.PointerCoords().apply { x = a.x; y = a.y; pressure = 1f; size = 0.1f },
                MotionEvent.PointerCoords().apply { x = b.x; y = b.y; pressure = 1f; size = 0.1f }
            )
            val now = android.os.SystemClock.uptimeMillis(); v.resume()
            fun dispatch(action: Int, count: Int) {
                val e = MotionEvent.obtain(now, now + 10, action, count, properties, coords, 0, 0, 1f, 1f, 0, 0, android.view.InputDevice.SOURCE_TOUCHSCREEN, 0)
                v.dispatchTouchEvent(e); e.recycle()
            }
            dispatch(MotionEvent.ACTION_DOWN, 1)
            dispatch(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), 2)
            assertEquals(2, v.engine.captures); assertEquals(2, v.engine.touches)
            dispatch(MotionEvent.ACTION_POINTER_UP, 2)
            dispatch(MotionEvent.ACTION_CANCEL, 2)
        }
    }
    @Test fun canvasSurvivesRotationAndBackgroundAlwaysRequiresResume() {
        beginGame()
        var engine: GameEngine? = null
        ui {
            engine = (find("gameCanvas") as GameView).engine
            activityRule.activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        Thread.sleep(350); instrumentation.waitForIdleSync()
        ui {
            val v = find("gameCanvas") as GameView
            assertSame(engine, v.engine)
            assertTrue(v.width > 0 && v.height > 0)
            v.engine.prey.forEach { p -> assertTrue(p.x in 0f..v.width.toFloat() && p.y in 0f..v.height.toFloat()) }
        }
        // Simulate even a brief lifecycle interruption; onStop is not required to keep the game paused.
        ui { instrumentation.callActivityOnPause(activityRule.activity); instrumentation.callActivityOnResume(activityRule.activity) }
        val pausedTime = arrayOf(0f)
        ui { pausedTime[0] = (find("gameCanvas") as GameView).engine.elapsedSeconds }
        Thread.sleep(150)
        ui { assertEquals(pausedTime[0], (find("gameCanvas") as GameView).engine.elapsedSeconds, 0f) }
        dialogButton("继续玩")
        ui { activityRule.activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }
}
