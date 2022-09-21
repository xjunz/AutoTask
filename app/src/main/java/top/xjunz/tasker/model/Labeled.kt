package top.xjunz.tasker.model

import androidx.annotation.StringRes
import top.xjunz.tasker.ktx.str

/**
 * @author xjunz 2022/07/23
 */
open class Labeled(@StringRes private val labelRes: Int) {
    val label get() = if (labelRes == -1) null else labelRes.str
}