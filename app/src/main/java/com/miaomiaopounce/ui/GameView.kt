package com.miaomiaopounce.ui

import android.content.Context
import android.graphics.Canvas
import android.view.*
import com.miaomiaopounce.game.*

class GameView @JvmOverloads constructor(context: Context, val engine: GameEngine = GameEngine(GameSettings()), private val preview: Boolean = false) : View(context), Choreographer.FrameCallback {
    private val renderer = PreyRenderer()
    private val effects = mutableListOf<Pair<Capture, Float>>()
    private var running = false
    private var lastFrame = 0L
    private var attached = false
    private var cornerPointer = -1
    private var cornerX = 0f; private var cornerY = 0f
    private var guardTriggered = false
    private val guardSize get() = 60 * resources.displayMetrics.density
    var onCapture: (() -> Unit)? = null
    var onFinished: (() -> Unit)? = null
    var onOwnerRequest: (() -> Unit)? = null
    private val ownerRunnable = Runnable {
        if (cornerPointer >= 0 && running && !preview) {
            guardTriggered = true; pause(); onOwnerRequest?.invoke()
        }
    }
    init { contentDescription = if (preview) "互动预览" else "猫咪游戏区域"; isFocusable = true }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) { engine.resize(w.toFloat(), h.toFloat()) }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); attached = true; resume() }
    override fun onDetachedFromWindow() { pause(); attached = false; super.onDetachedFromWindow() }
    fun resume() {
        if (running || !attached || engine.finished) return
        running = true; lastFrame = 0L; engine.clearContacts()
        Choreographer.getInstance().postFrameCallback(this)
    }
    fun pause() {
        running = false; lastFrame = 0L; cancelGuard(); engine.clearContacts()
        Choreographer.getInstance().removeFrameCallback(this)
    }
    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return
        val delta = if (lastFrame == 0L) 0f else (frameTimeNanos - lastFrame) / 1_000_000_000f
        lastFrame = frameTimeNanos
        engine.update(delta)
        val previous = effects.toList(); effects.clear()
        previous.forEach { (hit, age) -> if (age + delta < 0.38f) effects += hit to age + delta }
        invalidate()
        if (engine.finished) {
            running = false
            if (!preview) onFinished?.invoke()
        } else Choreographer.getInstance().postFrameCallback(this)
    }
    override fun onDraw(canvas: Canvas) {
        renderer.habitat(canvas, engine.settings.theme, width.toFloat(), height.toFloat(), preview)
        engine.prey.filter { it.visible }.forEach { renderer.prey(canvas, engine.settings.theme, it) }
        effects.forEach { (hit, age) -> renderer.capture(canvas, engine.settings.theme, hit, age / 0.38f) }
    }
    private fun points(event: MotionEvent, exclude: Int = -1) = (0 until event.pointerCount)
        .filter { it != exclude }.map { Contact(event.getX(it), event.getY(it)) }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!running) return true
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = event.actionIndex
                if (!preview && event.getX(i) < guardSize && event.getY(i) < guardSize && cornerPointer < 0) {
                    cornerPointer = event.getPointerId(i); cornerX = event.getX(i); cornerY = event.getY(i)
                    guardTriggered = false; postDelayed(ownerRunnable, 1600)
                }
                registerCaptures(engine.touch(points(event), 1))
            }
            MotionEvent.ACTION_MOVE -> {
                val i = if (cornerPointer < 0) -1 else event.findPointerIndex(cornerPointer)
                if (i >= 0 && (kotlin.math.abs(event.getX(i) - cornerX) > 16 * resources.displayMetrics.density ||
                        kotlin.math.abs(event.getY(i) - cornerY) > 16 * resources.displayMetrics.density)) cancelGuard()
                registerCaptures(engine.touch(points(event)))
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (event.getPointerId(event.actionIndex) == cornerPointer) cancelGuard()
                engine.release(points(event, event.actionIndex))
                if (event.actionMasked == MotionEvent.ACTION_UP && !guardTriggered) performClick()
            }
            MotionEvent.ACTION_CANCEL -> { cancelGuard(); engine.clearContacts() }
        }
        return true
    }
    private fun registerCaptures(hits: List<Capture>) { if (hits.isNotEmpty()) { hits.forEach { effects += it to 0f }; onCapture?.invoke(); invalidate() } }
    private fun cancelGuard() { removeCallbacks(ownerRunnable); cornerPointer = -1 }
    override fun performClick(): Boolean { super.performClick(); return true }
}
