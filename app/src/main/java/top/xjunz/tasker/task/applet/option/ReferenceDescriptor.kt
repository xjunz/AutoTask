package top.xjunz.tasker.task.applet.option

import androidx.annotation.StringRes

/**
 * @author xjunz 2022/11/20
 */
data class ReferenceDescriptor(@StringRes val name: Int, val type: Class<*>)