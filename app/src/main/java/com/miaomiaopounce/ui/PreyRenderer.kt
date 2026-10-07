package com.miaomiaopounce.ui

import android.graphics.*
import com.miaomiaopounce.game.*
import kotlin.math.*

/** Original procedural artwork; scales cleanly from card icons to tablets. */
class PreyRenderer {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private fun fill(c: Canvas, color: Int, draw: (Paint) -> Unit) {
        paint.color = color; paint.style = Paint.Style.FILL; paint.alpha = 255; draw(paint)
    }
    private fun line(c: Canvas, color: Int, width: Float, draw: (Paint) -> Unit) {
        paint.color = color; paint.style = Paint.Style.STROKE; paint.strokeWidth = width
        paint.strokeCap = Paint.Cap.ROUND; paint.alpha = 255; draw(paint)
    }
    private fun shape(c: Canvas, color: Int, vararg points: Float) {
        path.reset(); path.moveTo(points[0], points[1])
        for (i in 2 until points.size step 2) path.lineTo(points[i], points[i + 1])
        path.close(); fill(c, color) { c.drawPath(path, it) }
    }

    fun prey(c: Canvas, theme: PreyTheme, p: Prey) {
        c.save(); c.translate(p.x, p.y); c.rotate(p.angle * 180f / PI.toFloat()); c.scale(p.radius, p.radius)
        when (theme) {
            PreyTheme.BUG -> bug(c, p.age, p.variant)
            PreyTheme.FISH -> fish(c, p.age, p.variant)
            PreyTheme.MOUSE -> mouse(c, p.age)
            PreyTheme.DOT -> dot(c, p.age, p.variant)
        }
        c.restore()
    }

    private fun bug(c: Canvas, age: Float, variant: Int) {
        val wing = 0.76f + sin(age * 16f) * 0.13f
        val color = intArrayOf(0xFFC89B43.toInt(), 0xFFD68254.toInt(), 0xFF839C5B.toInt(), 0xFFCA997E.toInt())[variant % 4]
        fill(c, color) {
            c.drawOval(-0.65f, -1.0f * wing, 0.85f, -0.12f, it)
            c.drawOval(-0.65f, 0.12f, 0.85f, 1.0f * wing, it)
        }
        fill(c, 0xFFF6DFA0.toInt()) {
            c.drawOval(-0.7f, -0.75f * wing, 0.18f, -0.13f, it)
            c.drawOval(-0.7f, 0.13f, 0.18f, 0.75f * wing, it)
        }
        fill(c, Palette.ink) { c.drawOval(-0.6f, -0.19f, 0.8f, 0.19f, it); c.drawCircle(0.7f, 0f, 0.27f, it) }
        line(c, Palette.ink, 0.07f) {
            c.drawLine(0.78f, -0.12f, 1.12f, -0.43f, it)
            c.drawLine(0.78f, 0.12f, 1.12f, 0.43f, it)
        }
        fill(c, Palette.white) { c.drawCircle(0.8f, -0.1f, 0.065f, it); c.drawCircle(0.8f, 0.1f, 0.065f, it) }
    }

    private fun fish(c: Canvas, age: Float, variant: Int) {
        val color = intArrayOf(0xFFD77853.toInt(), 0xFFC49739.toInt(), 0xFF4A9297.toInt(), 0xFF9B749A.toInt())[variant % 4]
        val tail = sin(age * 10) * 0.13f
        shape(c, color, -0.65f, 0f, -1.25f, -0.62f + tail, -1.25f, 0.62f + tail)
        fill(c, color) { c.drawOval(-0.87f, -0.62f, 1.1f, 0.62f, it) }
        shape(c, 0x77FFFFFF, -0.1f, -0.1f, -0.48f, -0.45f, -0.48f, 0.25f)
        fill(c, Palette.white) { c.drawCircle(0.58f, -0.2f, 0.22f, it) }
        fill(c, Palette.ink) { c.drawCircle(0.66f, -0.2f, 0.105f, it) }
        line(c, Palette.ink, 0.045f) { c.drawArc(0.66f, 0.08f, 1.0f, 0.32f, 15f, 110f, false, it) }
    }

    private fun mouse(c: Canvas, age: Float) {
        path.reset(); path.moveTo(-0.65f, 0f); path.cubicTo(-1.2f, 0.6f, -1.5f, -0.7f + sin(age * 4f) * 0.1f, -1.85f, 0.1f)
        line(c, 0xFFB9887A.toInt(), 0.11f) { c.drawPath(path, it) }
        fill(c, 0xFF8A8072.toInt()) { c.drawOval(-1.05f, -0.62f, 0.7f, 0.62f, it) }
        fill(c, 0xFFA59988.toInt()) { c.drawOval(0.03f, -0.52f, 1.06f, 0.52f, it) }
        fill(c, 0xFF8A8072.toInt()) { c.drawCircle(0.25f, -0.55f, 0.36f, it); c.drawCircle(0.25f, 0.55f, 0.36f, it) }
        fill(c, 0xFFD9AAA0.toInt()) { c.drawCircle(0.25f, -0.55f, 0.21f, it); c.drawCircle(0.25f, 0.55f, 0.21f, it); c.drawCircle(1.02f, 0f, 0.13f, it) }
        fill(c, Palette.ink) { c.drawCircle(0.64f, -0.24f, 0.10f, it); c.drawCircle(0.64f, 0.24f, 0.10f, it) }
        line(c, Palette.ink, 0.04f) { c.drawLine(0.8f, -0.08f, 1.14f, -0.39f, it); c.drawLine(0.8f, 0.08f, 1.14f, 0.39f, it) }
    }

    private fun dot(c: Canvas, age: Float, variant: Int) {
        val colors = intArrayOf(0xFFE2B858.toInt(), 0xFF93C1BF.toInt(), 0xFFE69A78.toInt(), 0xFFC3A8D7.toInt())
        val scale = 0.92f + sin(age * 3.5f) * 0.08f
        fill(c, colors[variant % 4]) { c.drawCircle(0f, 0f, scale, it) }
        fill(c, 0x77FFFFFF) { c.drawCircle(-0.3f, -0.3f, 0.22f, it) }
    }

    fun habitat(c: Canvas, theme: PreyTheme, width: Float, height: Float, preview: Boolean = false) {
        c.drawColor(Palette.habitat(theme))
        val unit = min(width, height)
        val speck = if (theme == PreyTheme.DOT) 0x1254B6A0 else 0x159AA788
        fill(c, speck) { p ->
            for (i in 0..40) c.drawCircle(((i * 173 + 53) % 997) / 997f * width, ((i * 239 + 107) % 991) / 991f * height, unit * 0.003f, p)
        }
        if (theme == PreyTheme.FISH) {
            line(c, 0x2649878A, unit * 0.003f) {
                c.drawOval(width * 0.65f, -height * 0.23f, width * 1.13f, height * 0.22f, it)
                c.drawOval(-width * 0.2f, height * 0.8f, width * 0.32f, height * 1.2f, it)
            }
        }
        if (theme != PreyTheme.DOT) {
            shelterLocations(width, height).forEach { shelter ->
                val r = unit * 0.062f
                if (theme == PreyTheme.FISH) {
                    fill(c, 0xFFADC3B8.toInt()) { c.drawOval(shelter.x - r, shelter.y - r * 0.65f, shelter.x + r, shelter.y + r * 0.65f, it) }
                    fill(c, 0xFFC5D7CA.toInt()) { c.drawOval(shelter.x - r * 0.65f, shelter.y - r * 0.50f, shelter.x + r * 0.5f, shelter.y + r * 0.2f, it) }
                } else if (theme == PreyTheme.MOUSE) {
                    fill(c, 0xFFBDA889.toInt()) { c.drawRoundRect(shelter.x - r, shelter.y - r * 0.65f, shelter.x + r, shelter.y + r * 0.65f, r * 0.5f, r * 0.5f, it) }
                    fill(c, 0xFF877864.toInt()) { c.drawOval(shelter.x - r * 0.55f, shelter.y - r * 0.42f, shelter.x + r * 0.55f, shelter.y + r * 0.42f, it) }
                } else {
                    fill(c, 0xFFADB982.toInt()) {
                        c.drawOval(shelter.x - r, shelter.y - r * 0.60f, shelter.x + r * 0.1f, shelter.y + r * 0.55f, it)
                        c.drawOval(shelter.x - r * 0.2f, shelter.y - r, shelter.x + r * 0.75f, shelter.y + r * 0.6f, it)
                    }
                }
            }
            c.save(); c.translate(width * 0.025f, height * 0.98f); c.scale(unit * 0.09f, unit * 0.09f)
            leafCluster(c, if (theme == PreyTheme.FISH) 0xFF80AAA0.toInt() else 0xFF9DAE7A.toInt())
            c.restore()
            c.save(); c.translate(width * 0.97f, height * 0.07f); c.rotate(180f); c.scale(unit * 0.09f, unit * 0.09f)
            leafCluster(c, if (theme == PreyTheme.MOUSE) 0xFFB9A886.toInt() else 0xFFAABD92.toInt())
            c.restore()
        }
        if (preview) {
            paint.style = Paint.Style.FILL; paint.color = if (theme == PreyTheme.DOT) 0x66FFFFFF else 0x66435C4B
            paint.textSize = unit * 0.036f; paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            c.drawText("试着轻点一下", width * 0.065f, height * 0.91f, paint)
        }
    }

    private fun leafCluster(c: Canvas, color: Int) {
        for (i in 0..3) {
            c.save(); c.rotate(-25f + i * 29f)
            fill(c, color) { c.drawOval(-0.22f, -1.9f, 0.3f, 0.15f, it) }
            c.restore()
        }
    }

    fun capture(c: Canvas, theme: PreyTheme, hit: Capture, progress: Float) {
        val alpha = ((1f - progress) * 150).toInt().coerceIn(0, 255)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = hit.radius * 0.06f
        paint.color = if (theme == PreyTheme.DOT) Palette.white else Palette.accent(theme); paint.alpha = alpha
        c.drawCircle(hit.x, hit.y, hit.radius * (0.5f + progress * 1.6f), paint)
        paint.style = Paint.Style.FILL
        for (i in 0..4) {
            val a = i * PI.toFloat() * 2f / 5f
            val d = hit.radius * (0.5f + progress * 1.3f)
            c.drawCircle(hit.x + cos(a) * d, hit.y + sin(a) * d, hit.radius * 0.12f * (1f - progress), paint)
        }
        paint.alpha = 255
    }
}
