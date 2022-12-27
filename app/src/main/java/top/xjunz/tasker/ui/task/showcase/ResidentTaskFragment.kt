/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.showcase

import android.os.Bundle
import android.view.View
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.ktx.observeTransient
import top.xjunz.tasker.task.storage.TaskStorage

/**
 * @author xjunz 2022/12/20
 */
class ResidentTaskFragment : BaseTaskShowcaseFragment() {

    override fun initTaskList(): List<XTask> {
        return TaskStorage.allTasks
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeTransient(viewModel.onNewTaskAdded) {
            taskList.add(it)
            if (taskList.size == 1) togglePlaceholder(false)
            adapter.notifyItemInserted(taskList.lastIndex)
        }
    }
}