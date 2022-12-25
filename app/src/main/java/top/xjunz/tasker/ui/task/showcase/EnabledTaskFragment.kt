package top.xjunz.tasker.ui.task.showcase

import android.os.Bundle
import android.view.View
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.ktx.observeTransient
import top.xjunz.tasker.task.storage.TaskStorage

/**
 * @author xjunz 2022/12/20
 */
class EnabledTaskFragment : BaseTaskShowcaseFragment() {

    override fun initTaskList(): List<XTask> {
        return TaskStorage.allTasks.filter { it.isEnabled }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeTransient(viewModel.onTaskToggled) {
            if (it.isEnabled) {
                taskList.add(it)
                adapter.notifyItemInserted(taskList.lastIndex)
                if (taskList.size == 1) togglePlaceholder(false)
            } else {
                val index = taskList.indexOf(it)
                check(index >= 0)
                taskList.removeAt(index)
                adapter.notifyItemRemoved(index)
                if (taskList.isEmpty()) togglePlaceholder(true)
            }
        }
    }
}