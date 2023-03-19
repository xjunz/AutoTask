/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.showcase

import android.os.Bundle
import android.view.View
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView.Adapter
import kotlinx.coroutines.async
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.DialogTaskListBinding
import top.xjunz.tasker.databinding.ItemTaskListBinding
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.task.applet.option.AppletOptionFactory
import top.xjunz.tasker.task.storage.TaskStorage
import top.xjunz.tasker.ui.base.BaseBottomSheetDialog
import top.xjunz.tasker.ui.base.inlineAdapter
import top.xjunz.tasker.util.ClickListenerUtil.setNoDoubleClickListener

/**
 * @author xjunz 2022/12/21
 */
class TaskListDialog : BaseBottomSheetDialog<DialogTaskListBinding>() {

    private class InnerViewModel : ViewModel() {

        var title: CharSequence? = null

        var preloadTaskMode = false

        var taskList = MutableLiveData<List<XTask>>(emptyList())

        fun preloadTasks() {
            if (TaskStorage.presetTaskLoaded) {
                taskList.value = TaskStorage.getPreloadTasks()
            } else viewModelScope.async {
                TaskStorage.loadPresetTasks(AppletOptionFactory)
                TaskStorage.presetTaskLoaded = true
                taskList.value = TaskStorage.getPreloadTasks()
            }.invokeOnError {
                toastUnexpectedError(it)
            }
        }
    }

    private val parentViewModel by activityViewModels<TaskShowcaseViewModel>()

    private val viewModel by viewModels<InnerViewModel>()

    private val adapter: Adapter<*> by lazy {
        inlineAdapter(viewModel.taskList.require(), ItemTaskListBinding::class.java, {
            binding.btnAdd.setNoDoubleClickListener {
                parentViewModel.requestAddNewTask.value =
                    viewModel.taskList.require()[adapterPosition]
            }
        }) { binding, _, task ->
            binding.btnAdd.isEnabled = !TaskStorage.getAllTasks().contains(task)
            if (binding.btnAdd.isEnabled) {
                binding.btnAdd.text = R.string._import.text
            } else {
                binding.btnAdd.text = R.string.imported.text
            }
            binding.tvTaskName.text = task.metadata.title
            binding.tvTaskDesc.text = task.metadata.spannedDescription
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvTitle.text = viewModel.title
        if (viewModel.preloadTaskMode) {
            viewModel.preloadTasks()
        }
        binding.rvTaskList.applySystemInsets { v, insets ->
            v.updatePadding(bottom = insets.bottom)
        }
        observe(viewModel.taskList) {
            if (it.isNotEmpty()) {
                binding.rvTaskList.adapter = adapter
            }
        }
        observeTransient(parentViewModel.onNewTaskAdded) {
            adapter.notifyItemChanged(TaskStorage.getPreloadTasks().indexOf(it), true)
        }
    }

    fun setPreloadTaskMode() = doWhenCreated {
        viewModel.preloadTaskMode = true
        viewModel.title = R.string.preload_tasks.text
    }

    fun setTitle(title: CharSequence?) = doWhenCreated {
        viewModel.title = title
    }

    fun setTaskList(list: List<XTask>) = doWhenCreated {
        viewModel.taskList.value = list
    }
}