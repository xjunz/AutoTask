package top.xjunz.tasker.ktx

import androidx.annotation.FloatRange
import androidx.core.graphics.ColorUtils

/**
 * @author xjunz 2022/12/11
 */
fun Int.alphaModified(@FloatRange(from = 0.0, to = 1.0) alpha: Float): Int {
    return ColorUtils.setAlphaComponent(this, (alpha * 0xFF).toInt())
}