package com.miaomiaopounce.game

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random
import kotlin.math.hypot

class GameEngineTest {
    private fun engine(settings: GameSettings = GameSettings()) = GameEngine(settings, Random(123)).apply { resize(1280f, 800f) }

    @Test fun capturesOnContactAndNeverTwice() {
        val e = engine(GameSettings(count = 1)); val p = e.prey.single()
        assertEquals(1, e.touch(listOf(Contact(p.x, p.y)), 1).size)
        assertEquals(0, e.prey.size)
        assertTrue(e.touch(listOf(Contact(p.x, p.y))).isEmpty())
        assertEquals(1, e.captures); assertEquals(1, e.touches)
    }
    @Test fun forgivingHitAreaAndMissesAreSeparate() {
        val e = engine(GameSettings(count = 1)); val p = e.prey.single()
        assertTrue(e.touch(listOf(Contact(0f, 0f)), 1).isEmpty())
        assertEquals(1, e.touch(listOf(Contact(p.x + p.radius * 1.4f, p.y)), 1).size)
        assertEquals(2, e.touches)
    }
    @Test fun multiTouchCapturesTwoIndependentTargets() {
        val e = engine(GameSettings(count = 2)); val a = e.prey[0]; val b = e.prey[1]
        a.x = 300f; a.y = 300f; b.x = 900f; b.y = 500f
        assertEquals(2, e.touch(listOf(Contact(a.x, a.y), Contact(b.x, b.y)), 2).size)
        assertEquals(2, e.captures)
    }
    @Test fun delayedRespawnAvoidsHeldPawAndMaintainsCount() {
        val e = engine(GameSettings(count = 1)); val p = e.prey.single(); val contact = Contact(p.x, p.y)
        e.touch(listOf(contact), 1)
        e.update(0.1f); assertTrue(e.prey.isEmpty())
        repeat(35) { e.update(0.05f) }
        val new = e.prey.single()
        assertNotEquals(p.id, new.id)
        assertTrue(hypot(new.x - contact.x, new.y - contact.y) > new.radius * 1.45f)
        assertEquals(1, e.captures)
    }
    @Test fun noTimeAdvancesUnlessUpdatedAndSessionStopsExactly() {
        val e = engine(GameSettings(minutes = 1)); val first = e.prey.first().copy()
        assertEquals(0f, e.elapsedSeconds, 0f)
        assertEquals(first.x, e.prey.first().x)
        e.update(59.5f); assertFalse(e.finished)
        e.update(0.5f); assertTrue(e.finished)
        val p = e.prey.first()
        assertTrue(e.touch(listOf(Contact(p.x, p.y)), 1).isEmpty())
        e.update(100f); assertEquals(60f, e.elapsedSeconds, 0.001f)
    }
    @Test fun boundsHoldAcrossThemesPacesAndLargeResize() {
        for (theme in PreyTheme.entries) for (pace in Pace.entries) {
            val e = engine(GameSettings(theme = theme, pace = pace, count = 8, minutes = 10, size = PreySize.LARGE))
            repeat(2000) { e.update(0.04f) }
            e.resize(800f, 1280f)
            repeat(1000) {
                e.update(0.04f)
                e.prey.forEach { p ->
                    assertTrue(p.x.isFinite() && p.y.isFinite())
                    assertTrue(p.x > p.radius && p.x < e.width - p.radius)
                    assertTrue(p.y > p.radius && p.y < e.height - p.radius)
                }
            }
            assertEquals(8, e.prey.size)
        }
    }
    @Test fun hidingPreyCannotBeCaptured() {
        val e = engine(GameSettings(count = 1)); val p = e.prey.single(); p.phase = MotionPhase.HIDING
        assertTrue(e.touch(listOf(Contact(p.x, p.y)), 1).isEmpty())
        assertEquals(0, e.captures)
    }
    @Test fun invalidInputsAndSettingsDoNotCorruptSimulation() {
        val e = engine(GameSettings(count = 50, volume = 500, minutes = -1))
        assertEquals(8, e.prey.size); assertEquals(60, e.settings.volume); assertEquals(1, e.settings.minutes)
        e.update(Float.NaN); e.update(-2f)
        assertEquals(0f, e.elapsedSeconds)
        assertTrue(e.touch(listOf(Contact(Float.NaN, 5f)), -3).isEmpty())
        assertEquals(0, e.touches)
    }
    @Test fun noHidingPresetKeepsAllTargetsVisible() {
        val e = engine(GameSettings(hiding = false, count = 8))
        repeat(2000) { e.update(0.05f); assertTrue(e.prey.all { it.visible }) }
    }
    @Test fun holdDoesNotCaptureTargetsDuringSimulation() {
        val e = engine(GameSettings(count = 1, hiding = false)); val p = e.prey.single()
        val contact = Contact(600f, 400f)
        e.touch(listOf(contact), 1)
        p.x = contact.x; p.y = contact.y
        repeat(100) { e.update(0.02f) }
        assertEquals(0, e.captures)
    }
}
