/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.showcase

import android.os.Bundle
import android.view.View
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.DialogTaskShowcaseBinding
import top.xjunz.tasker.ui.base.BaseDialogFragment

/**
 * @author xjunz 2022/07/30
 */
class TaskShowcaseDialog : BaseDialogFragment<DialogTaskShowcaseBinding>() {

    override val isFullScreen = true

    override val windowAnimationStyle: Int = R.style.DialogAnimationSlide

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }
}