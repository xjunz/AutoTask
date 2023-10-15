/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.showcase

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import top.xjunz.shared.trace.logcatStackTrace
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.ktx.format
import top.xjunz.tasker.ktx.invokeOnError
import top.xjunz.tasker.ktx.require
import top.xjunz.tasker.ktx.toast
import top.xjunz.tasker.ktx.toastUnexpectedError
import top.xjunz.tasker.task.runtime.LocalTaskManager
import top.xjunz.tasker.task.runtime.LocalTaskManager.isEnabled
import top.xjunz.tasker.task.storage.TaskStorage

/**
 * @author xjunz 2022/12/20
 */
class TaskShowcaseViewModel : ViewModel() {

    val requestTrackTask = MutableLiveData<XTask>()

    val requestEditTask = MutableLiveData<Pair<XTask, Flow?>>()

    val requestDeleteTask = MutableLiveData<XTask>()

    val onTaskDeleted = MutableLiveData<XTask>()

    val requestToggleTask = MutableLiveData<XTask>()

    val onTaskToggled = MutableLiveData<XTask>()

    val onTaskUpdated = MutableLiveData<XTask>()

    /**
     * Request adding a new task.
     */
    val requestAddNewTasks = MutableLiveData<List<XTask>>()

    /**
     * When a new task is added.
     */
    val onNewTaskAdded = MutableLiveData<XTask>()

    val onTaskPauseStateChanged = MutableLiveData<Long>()

    fun listenTaskPauseStateChanges() {
        LocalTaskManager.setOnTaskPausedStateChangedListener {
            onTaskPauseStateChanged.postValue(it)
        }
    }

    override fun onCleared() {
        super.onCleared()
        LocalTaskManager.setOnTaskPausedStateChangedListener(null)
    }

    fun deleteRequestedTask() {
        val task = requestDeleteTask.require()
        runCatching {
            TaskStorage.removeTask(task)
            LocalTaskManager.removeTask(task)
            onTaskDeleted.value = task
            toast(R.string.format_task_removed.format(task.metadata.title))
        }.onFailure {
            toastUnexpectedError(it)
        }
    }

    fun addRequestedTasks() {
        viewModelScope.async {
            val tasks = requestAddNewTasks.require()
            val duplicated = mutableListOf<XTask>()
            val succeeded = mutableListOf<XTask>()
            tasks.forEach {
                if (TaskStorage.getAllTasks().contains(it)) {
                    duplicated.add(it)
                } else {
                    TaskStorage.persistTask(it)
                    TaskStorage.addTask(it)
                    onNewTaskAdded.value = it
                    succeeded.add(it)
                }
            }
            if (duplicated.size == 1 && succeeded.size == 0) {
                toast(R.string.prompt_repeated_task)
            } else if (duplicated.size == 0) {
                toast(R.string.format_import_tasks_succeeded.format(succeeded.size))
            } else {
                toast(R.string.format_import_tasks_failed.format(duplicated.size, succeeded.size))
            }
        }.invokeOnError {
            it.logcatStackTrace()
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
                removed = true
            } catch (t: Throwable) {
                t.logcatStackTrace()
                toastUnexpectedError(t)
            }
            // Restore checksum to current one
            task.metadata.checksum = currentChecksum
            if (!removed) return@launch
            try {
                TaskStorage.persistTask(task)
                TaskStorage.addTask(task)
                LocalTaskManager.updateTask(prevChecksum, task)
                toast(R.string.task_updated)
                onTaskUpdated.value = task
            } catch (t: Throwable) {
                onTaskDeleted.value = task
                toastUnexpectedError(t)
            }
        }
    }

    fun toggleTask(task: XTask) {
        if (task.isEnabled) {
            LocalTaskManager.removeTask(task)
        } else {
            LocalTaskManager.enableResidentTask(task)
        }
        TaskStorage.toggleTaskFilename(task)
        onTaskToggled.value = task
    }
}