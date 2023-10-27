/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.showcase

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DiffUtil
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.ktx.observeTransient
import top.xjunz.tasker.task.runtime.LocalTaskManager.isEnabled
import top.xjunz.tasker.task.storage.TaskStorage

/**
 * @author xjunz 2022/12/20
 */
class ResidentTaskFragment : BaseTaskShowcaseFragment() {

    private val taskComparator = Comparator<XTask> { o1, o2 ->
        var ret = o1.isEnabled.compareTo(o2.isEnabled)
        if (ret == 0) {
            ret = -o1.metadata.modificationTimestamp.compareTo(o2.metadata.modificationTimestamp)
        }
        return@Comparator ret
    }

    override fun initTaskList(): List<XTask> {
        return TaskStorage.getResidentTasks().sortedWith(taskComparator)
    }

    override val index: Int = 1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeTransient(viewModel.onNewTaskAdded) {
            // This may happen when the page is not yet enter its stage while
            // a new task is added.
            if (taskList.contains(it)) return@observeTransient
            if (it.metadata.taskType != XTask.TYPE_RESIDENT) return@observeTransient
            taskList.add(it)
            if (taskList.size == 1) togglePlaceholder(false)
            // adapter.notifyItemInserted(taskList.lastIndex)
            notifyBadgeNumberChanged()
            updateList(it)
        }
        observeTransient(viewModel.onTaskToggled) {
            updateList(it)
        }
    }

    private fun updateList(task: XTask) {
        val oldList = ArrayList(taskList)
        taskList.sortWith(taskComparator)
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int {
                return oldList.size
            }

            override fun getNewListSize(): Int {
                return taskList.size
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldList[oldItemPosition] === taskList[newItemPosition]
            }

            override fun areContentsTheSame(
                oldItemPosition: Int,
                newItemPosition: Int
            ): Boolean {
                return oldList[oldItemPosition] !== task
            }

            override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any {
                return PAYLOAD_ENABLED_STATUS
            }
        }
        DiffUtil.calculateDiff(diffCallback, true).dispatchUpdatesTo(adapter)
    }

}