package top.xjunz.tasker.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import androidx.appcompat.widget.AppCompatTextView

/**
 * @author xjunz 2022/10/07
 */
class LeftCorneredTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs) {

    inline val Number.dpFloat get() = resources.displayMetrics.density * toFloat()

    private val paint by lazy {
        Paint().apply {
            color = context.getColor(com.google.android.material.R.color.material_on_surface_stroke)
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 1.dpFloat
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    val w = view.width.toFloat()
                    val h = view.height.toFloat()
                    val r = 16.dpFloat
                    val path = Path()
                    path.moveTo(0F, 0F)
                    path.lineTo(w, 0F)
                    path.lineTo(w, h)
                    path.lineTo(r, h)
                    path.arcTo(0F, h - 2 * r, 2 * r, h, 90F, 90F, false)
                    path.close()
                    @Suppress("DEPRECATION")
                    outline.setConvexPath(path)
                }
            }
            invalidateOutline()
        }
    }

    private val radius = 16.dpFloat

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val h = height.toFloat()
        val w = width.toFloat()
        canvas.drawLine(w, h - paint.strokeWidth / 2, radius, h - paint.strokeWidth / 2, paint)
        canvas.drawArc(
            0F, h - 2 * radius - paint.strokeWidth / 2, 2 * radius, h - paint.strokeWidth / 2,
            90F, 90F, false, paint
        )
    }
}