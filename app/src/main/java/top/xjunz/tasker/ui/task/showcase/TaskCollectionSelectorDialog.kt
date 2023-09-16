/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.showcase

import android.os.Bundle
import android.view.View
import top.xjunz.tasker.databinding.DialogTaskCollectionSelectorBinding
import top.xjunz.tasker.ktx.show
import top.xjunz.tasker.ui.base.BaseBottomSheetDialog
import top.xjunz.tasker.util.ClickListenerUtil.setNoDoubleClickListener

/**
 * @author xjunz 2023/09/17
 */
class TaskCollectionSelectorDialog : BaseBottomSheetDialog<DialogTaskCollectionSelectorBinding>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.containerExampleTasks.setNoDoubleClickListener {
            TaskListDialog().setExampleTaskMode().show(requireActivity().supportFragmentManager)
            dismiss()
        }
        binding.containerPreloadTasks.setNoDoubleClickListener {
            TaskListDialog().setPreloadTaskMode().show(requireActivity().supportFragmentManager)
            dismiss()
        }
    }
}