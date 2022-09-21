package top.xjunz.tasker.task

import androidx.annotation.StringRes
import top.xjunz.tasker.R
import top.xjunz.tasker.model.Labeled

/**
 * @author xjunz 2022/07/25
 */
sealed class TaskType(@StringRes nameRes: Int) : Labeled(nameRes) {
    object Preload : TaskType(R.string.preload)
    object Rule : TaskType(R.string.rule)
    object Script : TaskType(R.string.script)
}