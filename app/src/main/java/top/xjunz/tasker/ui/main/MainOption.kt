/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import top.xjunz.tasker.R

/**
 * @author xjunz 2023/02/27
 */
sealed class MainOption(
    @StringRes val title: Int,
    @DrawableRes val icon: Int,
    @StringRes var desc: Int = -1,
    @DrawableRes val enterIcon: Int = -1
) {

    object CustomTasks :
        MainOption(
            R.string.task_list,
            R.drawable.ic_gesture_24px,
            enterIcon = R.drawable.ic_chevron_right_24px
        )

    object Feedback : MainOption(R.string.feedback, R.drawable.ic_chat_24px)

    object About :
        MainOption(R.string.about, R.drawable.ic_baseline_more_vert_24, R.string.more_to_say)

    companion object {
        val ALL_OPTIONS = arrayOf(Feedback, About)
    }
}