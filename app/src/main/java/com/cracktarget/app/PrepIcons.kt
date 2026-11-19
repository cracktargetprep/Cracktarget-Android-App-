package com.cracktarget.app

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

internal enum class PrepIcon { HOME, STUDY, TESTS, UPDATES, ACCOUNT, NEWS, VACANCY, BOOKS, PAPERS, ARROW, BACK, DOWNLOAD }

internal class PrepIconDrawable(private val icon: PrepIcon, color: Int, private val size: Int) : Drawable() {
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        strokeWidth = 1.7f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        style = Paint.Style.STROKE
    }

    override fun draw(canvas: Canvas) {
        canvas.save()
        canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
        canvas.scale(bounds.width() / 24f, bounds.height() / 24f)
        fun line(vararg points: Float) {
            val path = Path().apply {
                moveTo(points[0], points[1])
                for (index in 2 until points.size step 2) lineTo(points[index], points[index + 1])
            }
            canvas.drawPath(path, ink)
        }
        fun box(x: Float, y: Float, right: Float, bottom: Float) {
            canvas.drawRoundRect(x, y, right, bottom, 2f, 2f, ink)
        }
        when (icon) {
            PrepIcon.HOME -> {
                line(3f, 11f, 12f, 3f, 21f, 11f)
                line(6f, 10f, 6f, 21f, 10f, 21f, 10f, 15f, 14f, 15f, 14f, 21f, 18f, 21f, 18f, 10f)
            }
            PrepIcon.STUDY -> {
                line(2f, 9f, 12f, 4f, 22f, 9f, 12f, 14f, 2f, 9f)
                line(6f, 11f, 6f, 17f, 12f, 20f, 18f, 17f, 18f, 11f)
            }
            PrepIcon.TESTS -> {
                box(5f, 4f, 19f, 21f)
                line(8f, 9f, 10f, 11f, 14f, 7f)
                line(8f, 15f, 16f, 15f)
                line(8f, 18f, 13f, 18f)
            }
            PrepIcon.UPDATES -> {
                box(4f, 6f, 20f, 20f)
                line(8f, 3f, 8f, 8f, 16f, 8f, 16f, 3f)
                line(4f, 11f, 20f, 11f)
                line(8f, 15f, 10f, 15f)
                line(14f, 15f, 16f, 15f)
            }
            PrepIcon.ACCOUNT -> {
                canvas.drawCircle(12f, 8f, 3.7f, ink)
                canvas.drawArc(4f, 14f, 20f, 25f, 180f, 180f, false, ink)
            }
            PrepIcon.NEWS -> {
                box(4f, 4f, 20f, 20f)
                line(8f, 8f, 16f, 8f)
                line(8f, 12f, 16f, 12f)
                line(8f, 16f, 13f, 16f)
            }
            PrepIcon.VACANCY -> {
                box(3f, 8f, 21f, 20f)
                line(8f, 8f, 8f, 5f, 16f, 5f, 16f, 8f)
                line(3f, 12f, 21f, 12f)
                line(10f, 12f, 10f, 15f, 14f, 15f, 14f, 12f)
            }
            PrepIcon.BOOKS -> {
                box(4f, 4f, 10f, 20f)
                box(10f, 4f, 16f, 20f)
                line(17f, 6f, 20f, 5f, 23f, 19f, 20f, 20f)
                line(6f, 8f, 8f, 8f)
                line(12f, 8f, 14f, 8f)
            }
            PrepIcon.PAPERS -> {
                box(7f, 6f, 20f, 21f)
                line(16f, 3f, 4f, 3f, 4f, 17f)
                line(10f, 11f, 17f, 11f)
                line(10f, 15f, 17f, 15f)
            }
            PrepIcon.ARROW -> {
                line(5f, 12f, 19f, 12f)
                line(14f, 7f, 19f, 12f, 14f, 17f)
            }
            PrepIcon.BACK -> {
                line(19f, 12f, 5f, 12f)
                line(10f, 7f, 5f, 12f, 10f, 17f)
            }
            PrepIcon.DOWNLOAD -> {
                line(12f, 3f, 12f, 15f)
                line(8f, 11f, 12f, 15f, 16f, 11f)
                line(4f, 16f, 4f, 20f, 20f, 20f, 20f, 16f)
            }
        }
        canvas.restore()
    }

    override fun getIntrinsicWidth() = size
    override fun getIntrinsicHeight() = size
    override fun setAlpha(alpha: Int) { ink.alpha = alpha; invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) { ink.colorFilter = colorFilter; invalidateSelf() }
    @Deprecated("Drawable API")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}
