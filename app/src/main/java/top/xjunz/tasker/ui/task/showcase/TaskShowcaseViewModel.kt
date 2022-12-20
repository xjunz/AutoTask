package top.xjunz.tasker.ui.task.showcase

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.ktx.format
import top.xjunz.tasker.ktx.require
import top.xjunz.tasker.ktx.toast
import top.xjunz.tasker.task.TaskManager
import top.xjunz.tasker.task.TaskStorage
import top.xjunz.tasker.task.applet.option.AppletOptionFactory

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
            allTaskLoaded.value = true
        }
    }

    fun deleteRequestedTask() {
        val task = requestDeleteTask.require()
        if (TaskStorage.removeTask(task)) {
            onTaskDeleted.value = task
            toast(R.string.format_task_removed.format(task.metadata.title))
        } else {
            toast(R.string.format_error_code.format(-1))
        }
    }

    fun addRequestedTask() {
        val task = requestAddNewTask.require()
        viewModelScope.launch {
            val errorCode = TaskStorage.persistTask(task)
            if (errorCode == -3) {
                toast(R.string.error_add_repeated_task)
            } else if (errorCode != 0) {
                toast(R.string.format_error_code.format(errorCode))
            } else {
                onNewTaskAdded.value = task
                toast(R.string.format_new_task_added.format(task.metadata.title))
            }
        }
    }

    fun updateTask(task: XTask) {
        viewModelScope.launch {
            val errorCode = TaskStorage.updateExistingTask(task)
            if (errorCode == -3) {
                
            }
            onTaskUpdated.value = task
        }
    }

    fun toggleRequestedTask() {
        val it = requestToggleTask.require()
        if (it.isEnabled) {
            it.disable()
            TaskManager.disableResidentTask(it)
        } else {
            it.enable()
            TaskManager.addNewEnabledResidentTask(it)
        }
        TaskStorage.toggleTaskFilename(it)
        onTaskToggled.value = it
    }
}