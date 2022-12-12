package top.xjunz.tasker.ui.demo

import android.content.Context
import android.view.View

/**
 * @author xjunz 2022/12/12
 */
abstract class Demonstration(val context: Context) {

    abstract fun getView(): View

    abstract fun startDemonstration()

    abstract fun stopDemonstration()
}