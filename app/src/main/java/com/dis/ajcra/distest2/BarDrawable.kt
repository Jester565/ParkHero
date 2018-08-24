package com.dis.ajcra.distest2

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable


class BarDrawable: Drawable {
    private val themeColors: ArrayList<Int>

    constructor(colors: ArrayList<Int>)
        :super()
    {
        this.themeColors = colors
    }

    override fun getConstantState(): ConstantState {
        return object: ConstantState() {
            override fun newDrawable(): Drawable {
                return BarDrawable(themeColors)
            }

            override fun getChangingConfigurations(): Int {
                return 0
            }
        }
    }

    override fun draw(canvas: Canvas) {
        // get drawable dimensions
        val bounds = bounds

        val width = bounds.right - bounds.left
        val height = bounds.bottom - bounds.top

        // draw background gradient
        val backgroundPaint = Paint()
        val barWidth = width / themeColors.size
        val barWidthRemainder = width % themeColors.size
        for (i in themeColors.indices) {
            backgroundPaint.setColor(themeColors[i])
            canvas.drawRect((i * barWidth).toFloat(), 0f, ((i + 1) * barWidth).toFloat(), height.toFloat(), backgroundPaint)
        }

        // draw remainder, if exists
        if (barWidthRemainder > 0) {
            canvas.drawRect((themeColors.size * barWidth).toFloat(), 0f, (themeColors.size * barWidth + barWidthRemainder).toFloat(), height.toFloat(), backgroundPaint)
        }
    }

    override fun setAlpha(alpha: Int) {}

    override fun setColorFilter(cf: ColorFilter?) {

    }

    override fun getOpacity(): Int {
        return PixelFormat.OPAQUE
    }

}