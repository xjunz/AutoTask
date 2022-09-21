package top.xjunz.tasker.ui.task.flow

import androidx.annotation.StringRes
import top.xjunz.tasker.R
import top.xjunz.tasker.ktx.text

/**
 * @author xjunz 2022/09/11
 */
sealed class Target(@StringRes val labelRes: Int) {

    val label: CharSequence get() = labelRes.text

    open val elementNames = emptyMap<CharSequence, Array<String>>()

    object PackageName : Target(R.string.target_package_name) {

        override val elementNames: Map<CharSequence, Array<String>> =
            mapOf(
                R.string.preprocess.text to arrayOf(),
                R.string.package_name.text to arrayOf(),
            )

    }
}