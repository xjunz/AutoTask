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
        return TaskStorage.getAllTasks().filter { it.metadata.taskType == XTask.TYPE_RESIDENT }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeTransient(viewModel.onNewTaskAdded) {
            // This may happen when the page is not yet enter its stage while
            // a new task is added.
            if (taskList.contains(it)) return@observeTransient
            if (it.metadata.taskType != XTask.TYPE_RESIDENT) return@observeTransient
            taskList.add(it)
            if (taskList.size == 1) togglePlaceholder(false)
            adapter.notifyItemInserted(taskList.lastIndex)
        }
        observeTransient(viewModel.onTaskToggled) {
            adapter.notifyItemChanged(taskList.indexOf(it), true)
        }
    }
}