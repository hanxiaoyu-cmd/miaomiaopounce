package com.miaomiaopounce.ui

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

class TouchTestView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trail = ArrayDeque<PointF>()
    private var contacts = emptyList<PointF>()
    var onReading: ((Int, Int) -> Unit)? = null
    var touchCount = 0; private set
    var targetHits = 0; private set
    private val targetRadius get() = min(width, height) * 0.13f
    init { contentDescription = "猫爪触摸测试区域"; tag = "touchCanvas" }
    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Palette.habitat(com.miaomiaopounce.game.PreyTheme.BUG))
        val r = targetRadius
        paint.style = Paint.Style.STROKE; paint.color = 0x338A9D68; paint.strokeWidth = r * 0.025f
        canvas.drawCircle(width * 0.5f, height * 0.5f, r * 1.4f, paint)
        paint.style = Paint.Style.FILL; paint.color = Palette.orange
        canvas.drawCircle(width * 0.5f, height * 0.5f, r, paint)
        paint.color = Palette.white; paint.textSize = r * 0.29f; paint.textAlign = Paint.Align.CENTER
        canvas.drawText("拍拍这里", width * 0.5f, height * 0.5f + r * 0.10f, paint)
        trail.forEachIndexed { i, p ->
            paint.color = Palette.ink; paint.alpha = (30 + i.toFloat() / trail.size.coerceAtLeast(1) * 80).toInt()
            canvas.drawCircle(p.x, p.y, min(width, height) * 0.012f, paint)
        }
        paint.alpha = 255
        contacts.forEach { p ->
            paint.color = Palette.ink; paint.style = Paint.Style.STROKE; paint.strokeWidth = 3f
            canvas.drawCircle(p.x, p.y, r * 0.45f, paint)
        }
        paint.style = Paint.Style.FILL
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        contacts = (0 until event.pointerCount).filter {
            !((event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_POINTER_UP) && it == event.actionIndex)
        }.map { PointF(event.getX(it), event.getY(it)) }
        if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
            touchCount++
            val i = event.actionIndex
            if (kotlin.math.hypot(event.getX(i) - width * 0.5f, event.getY(i) - height * 0.5f) <= targetRadius) targetHits++
        }
        if (event.actionMasked == MotionEvent.ACTION_CANCEL) contacts = emptyList()
        contacts.forEach { trail += it; if (trail.size > 150) trail.removeFirst() }
        onReading?.invoke(touchCount, contacts.size); invalidate()
        if (event.actionMasked == MotionEvent.ACTION_UP) performClick()
        return true
    }
    override fun performClick(): Boolean { super.performClick(); return true }
    fun clear() { trail.clear(); contacts = emptyList(); touchCount = 0; targetHits = 0; onReading?.invoke(0, 0); invalidate() }
}
