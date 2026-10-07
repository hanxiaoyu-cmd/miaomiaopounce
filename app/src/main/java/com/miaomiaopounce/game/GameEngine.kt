package com.miaomiaopounce.game

import kotlin.math.*
import kotlin.random.Random

data class Contact(val x: Float, val y: Float)
fun shelterLocations(width: Float, height: Float) = listOf(Contact(width * 0.24f, height * 0.28f), Contact(width * 0.76f, height * 0.72f))
enum class MotionPhase { MOVING, RESTING, DARTING, HIDING }
data class Prey(
    val id: Long, var x: Float, var y: Float, val radius: Float,
    var angle: Float, var phase: MotionPhase, var phaseRemaining: Float,
    val variant: Int, var age: Float = 0f
) { val visible: Boolean get() = phase != MotionPhase.HIDING }
data class Capture(val id: Long, val x: Float, val y: Float, val radius: Float, val variant: Int)
data class SessionResult(val captures: Int, val touches: Int, val elapsedSeconds: Float, val theme: PreyTheme)

/** Pure simulation: no Android dependency; only active foreground time advances a session. */
class GameEngine(settings: GameSettings, private val random: Random = Random.Default) {
    val settings = settings.sanitized()
    val prey = mutableListOf<Prey>()
    var width = 0f; private set
    var height = 0f; private set
    var elapsedSeconds = 0f; private set
    var captures = 0; private set
    var touches = 0; private set
    var finished = false; private set
    private var nextId = 1L
    private val respawns = mutableListOf<Float>()
    private var contacts: List<Contact> = emptyList()
    val remainingSeconds get() = (settings.minutes * 60 - elapsedSeconds).coerceAtLeast(0f)

    fun resize(w: Float, h: Float) {
        if (w <= 0f || h <= 0f) return
        val oldWidth = width; val oldHeight = height
        width = w; height = h
        if (oldWidth > 0 && oldHeight > 0) {
            val previous = prey.toList()
            prey.clear()
            previous.forEach { p ->
                val r = min(width, height) * settings.size.fraction
                prey += p.copy(x = p.x / oldWidth * width, y = p.y / oldHeight * height, radius = r).also { bound(it) }
            }
        }
        if (prey.isEmpty() && respawns.isEmpty() && !finished) repeat(settings.count) { spawn() }
    }

    fun update(deltaSeconds: Float) {
        if (finished || width <= 0 || height <= 0 || !deltaSeconds.isFinite() || deltaSeconds <= 0) return
        elapsedSeconds = (elapsedSeconds + deltaSeconds).coerceAtMost(settings.minutes * 60f)
        if (remainingSeconds <= 0f) { finished = true; return }
        // Discard a long frame's physics displacement rather than tunnelling across the screen.
        val dt = deltaSeconds.coerceAtMost(0.05f)
        val pending = respawns.toList()
        respawns.clear()
        pending.forEach { val t = it - deltaSeconds; if (t <= 0) spawn() else respawns += t }
        prey.forEach { p ->
            p.age += dt
            p.phaseRemaining -= dt
            if (p.phaseRemaining <= 0) transition(p)
            if (p.phase == MotionPhase.MOVING || p.phase == MotionPhase.DARTING) {
                val base = min(width, height) * 0.16f * settings.pace.multiplier
                val speed = base * if (p.phase == MotionPhase.DARTING) 2.0f else 1f
                val curve = when (settings.theme) {
                    PreyTheme.FISH -> sin(p.age * 2.5f) * 0.6f
                    PreyTheme.BUG -> sin(p.age * 5f) * 0.32f
                    PreyTheme.MOUSE -> 0f
                    PreyTheme.DOT -> sin(p.age * 3f) * 0.2f
                }
                p.x += cos(p.angle + curve) * speed * dt
                p.y += sin(p.angle + curve) * speed * dt
                bound(p)
            }
        }
    }

    private fun transition(p: Prey) {
        when (p.phase) {
            MotionPhase.RESTING -> {
                p.angle += random.nextFloat() * 2.2f - 1.1f
                p.phase = if (random.nextFloat() < 0.3f) MotionPhase.DARTING else MotionPhase.MOVING
                p.phaseRemaining = between(0.65f, 1.8f)
            }
            MotionPhase.HIDING -> { p.phase = MotionPhase.RESTING; p.phaseRemaining = between(0.35f, 0.9f) }
            else -> {
                val nearShelter = shelterLocations(width, height).any { hypot(p.x - it.x, p.y - it.y) < min(width, height) * 0.12f }
                p.phase = if (settings.hiding && settings.theme != PreyTheme.DOT && nearShelter && random.nextFloat() < 0.5f)
                    MotionPhase.HIDING else MotionPhase.RESTING
                p.phaseRemaining = between(0.4f, 1.25f)
            }
        }
    }

    /** Call for down/move events. Stationary held paws cannot automatically farm passing prey. */
    fun touch(points: List<Contact>, newTouches: Int = 0): List<Capture> {
        contacts = points.filter { it.x.isFinite() && it.y.isFinite() }
        if (finished) return emptyList()
        touches += newTouches.coerceAtLeast(0)
        val hit = prey.filter { p -> p.visible && contacts.any { hypot(it.x - p.x, it.y - p.y) <= p.radius * 1.45f } }
        val results = hit.map { Capture(it.id, it.x, it.y, it.radius, it.variant) }
        prey.removeAll(hit.toSet())
        captures += results.size
        repeat(results.size) { respawns += between(0.65f, 1.25f) }
        return results
    }

    fun release(points: List<Contact>) { contacts = points }
    fun clearContacts() { contacts = emptyList() }
    fun result() = SessionResult(captures, touches, elapsedSeconds, settings.theme)

    private fun spawn() {
        val radius = min(width, height) * settings.size.fraction
        val margin = min(width, height) * 0.09f + radius
        var x = width / 2; var y = height / 2
        repeat(30) {
            x = between(margin, (width - margin).coerceAtLeast(margin))
            y = between(margin, (height - margin).coerceAtLeast(margin))
            if (contacts.none { hypot(it.x - x, it.y - y) < radius * 3.2f }) {
                prey += Prey(nextId++, x, y, radius, between(0f, PI.toFloat() * 2), MotionPhase.MOVING, between(0.8f, 2.3f), random.nextInt(4))
                return
            }
        }
        // Retry later when an unusually large touch cluster leaves no clear spawn point.
        respawns += 0.5f
    }

    private fun bound(p: Prey) {
        val margin = min(width, height) * 0.09f + p.radius
        val right = (width - margin).coerceAtLeast(margin)
        val bottom = (height - margin).coerceAtLeast(margin)
        if (p.x < margin || p.x > right) { p.angle = PI.toFloat() - p.angle; p.x = p.x.coerceIn(margin, right) }
        if (p.y < margin || p.y > bottom) { p.angle = -p.angle; p.y = p.y.coerceIn(margin, bottom) }
    }
    private fun between(a: Float, b: Float) = a + random.nextFloat() * (b - a)
}
