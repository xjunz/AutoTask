package top.xjunz.tasker.ui.task.showcase

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.task.applet.option.AppletOptionFactory
import top.xjunz.tasker.task.runtime.LocalTaskManager
import top.xjunz.tasker.task.storage.TaskStorage

/**
 * @author xjunz 2022/12/20
 */
class TaskShowcaseViewModel : ViewModel() {

    val requestDeleteTask = MutableLiveData<XTask>()

    val onTaskDeleted = MutableLiveData<XTask>()

    val allTaskLoaded = MutableLiveData<Boolean>()

    val appbarHeight = MutableLiveData<Int>()

    val bottomBarHeight = MutableLiveData<Int>()

    val requestToggleTask = MutableLiveData<XTask>()

    val onTaskToggled = MutableLiveData<XTask>()

    val onTaskUpdated = MutableLiveData<XTask>()

    /**
     * Request adding a new task to [TaskStorage.allTasks].
     */
    val requestAddNewTask = MutableLiveData<XTask>()

    /**
     * When a new task is added to [TaskStorage.allTasks].
     */
    val onNewTaskAdded = MutableLiveData<XTask>()

    val liftedStates = booleanArrayOf(false, false, false)

    fun init() {
        if (TaskStorage.customTaskLoaded) {
            allTaskLoaded.value = true
        } else viewModelScope.launch {
            TaskStorage.loadAllTasks(AppletOptionFactory())
            TaskStorage.customTaskLoaded = true
            LocalTaskManager.initialize(TaskStorage.allTasks.filter {
                it.isEnabled
            })
            allTaskLoaded.value = true
        }
    }

    fun deleteRequestedTask() {
        val task = requestDeleteTask.require()
        runCatching {
            TaskStorage.removeTask(task)
            LocalTaskManager.removeRemoteCache(task.checksum)
            onTaskDeleted.value = task
            toast(R.string.format_task_removed.format(task.metadata.title))
        }.onFailure {
            toastUnexpectedError(it)
        }
    }

    fun addRequestedTask() {
        val task = requestAddNewTask.require()
        if (TaskStorage.allTasks.contains(task)) {
            toast(R.string.error_add_repeated_task)
            return
        }
        viewModelScope.async {
            TaskStorage.persistTask(task)
            onNewTaskAdded.value = task
            toast(R.string.format_new_task_added.format(task.metadata.title))
        }.invokeOnError {
            toastUnexpectedError(it)
        }
    }

    fun updateTask(prevChecksum: Long, task: XTask) {
        viewModelScope.launch {
            val currentChecksum = task.checksum
            var removed = false
            // Assign to old checksum temporarily to remove the old file
            task.metadata.checksum = prevChecksum
            try {
                TaskStorage.removeTask(task)
                LocalTaskManager.removeRemoteCache(prevChecksum)
                removed = true
            } catch (t: Throwable) {
                toastUnexpectedError(t)
            }
            // Restore checksum to current one
            task.metadata.checksum = currentChecksum
            if (!removed) return@launch
            try {
                TaskStorage.persistTask(task)
                onTaskUpdated.value = task
            } catch (t: Throwable) {
                onTaskDeleted.value = task
                toastUnexpectedError(t)
            }
        }
    }

    fun toggleRequestedTask() {
        val it = requestToggleTask.require()
        if (it.isEnabled) {
            it.disable()
            LocalTaskManager.removeResidentTask(it)
        } else {
            it.enable()
            LocalTaskManager.addResidentTask(it)
        }
        TaskStorage.toggleTaskFilename(it)
        onTaskToggled.value = it
    }
}