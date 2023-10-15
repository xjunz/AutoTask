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
import top.xjunz.tasker.ktx.applySystemInsets
import top.xjunz.tasker.ktx.doWhenCreated
import top.xjunz.tasker.ktx.foreColored
import top.xjunz.tasker.ktx.invokeOnError
import top.xjunz.tasker.ktx.observe
import top.xjunz.tasker.ktx.observeTransient
import top.xjunz.tasker.ktx.require
import top.xjunz.tasker.ktx.str
import top.xjunz.tasker.ktx.text
import top.xjunz.tasker.ktx.toastUnexpectedError
import top.xjunz.tasker.task.applet.option.AppletOptionFactory
import top.xjunz.tasker.task.storage.TaskStorage
import top.xjunz.tasker.ui.base.BaseBottomSheetDialog
import top.xjunz.tasker.ui.base.inlineAdapter
import top.xjunz.tasker.ui.main.ColorScheme
import top.xjunz.tasker.util.ClickListenerUtil.setNoDoubleClickListener

/**
 * @author xjunz 2022/12/21
 */
class TaskListDialog : BaseBottomSheetDialog<DialogTaskListBinding>() {

    private class InnerViewModel : ViewModel() {

        var title: CharSequence? = null

        var preloadTaskMode = false

        var exampleTaskMode = false

        var taskList = MutableLiveData<List<XTask>>(emptyList())

        fun loadExampleTasks() {
            if (TaskStorage.exampleTaskLoaded) {
                taskList.value = TaskStorage.getExampleTasks()
            } else viewModelScope.async {
                TaskStorage.loadExampleTasks(AppletOptionFactory)
                taskList.value = TaskStorage.getExampleTasks()
            }.invokeOnError {
                toastUnexpectedError(it)
            }
        }

        fun loadPresetTasks() {
            if (TaskStorage.presetTaskLoaded) {
                taskList.value = TaskStorage.getPresetTasks()
            } else viewModelScope.async {
                TaskStorage.loadPresetTasks(AppletOptionFactory)
                taskList.value = TaskStorage.getPresetTasks()
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
                parentViewModel.requestAddNewTasks.value = listOf(
                    viewModel.taskList.require()[adapterPosition].clone(AppletOptionFactory)
                )
            }
        }) { binding, _, task ->
            binding.btnAdd.isEnabled = !TaskStorage.getAllTasks().contains(task)
            if (binding.btnAdd.isEnabled) {
                binding.btnAdd.text = R.string._import.text
            } else {
                binding.btnAdd.text = R.string.imported.text
            }
            binding.tvTaskName.text = task.metadata.title
            binding.tvTaskDesc.text = if (task.metadata.description.isNullOrEmpty()) {
                R.string.no_desc_provided.str.foreColored(ColorScheme.textColorDisabled)
            } else {
                task.metadata.spannedDescription
            }
            if (task.isResident) {
                binding.ivTaskType.setImageResource(R.drawable.ic_hourglass_bottom_24px)
            } else if (task.isOneshot) {
                binding.ivTaskType.setImageResource(R.drawable.ic_baseline_send_24)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvTitle.text = viewModel.title
        if (viewModel.preloadTaskMode) {
            viewModel.loadPresetTasks()
        } else if (viewModel.exampleTaskMode) {
            viewModel.loadExampleTasks()
        }
        binding.rvTaskList.applySystemInsets { v, insets ->
            v.updatePadding(bottom = insets.bottom)
        }
        binding.btnImportAll.setNoDoubleClickListener {
            val exportable = viewModel.taskList.value?.filter { task ->
                !TaskStorage.getAllTasks().contains(task)
            }?.map { task ->
                task.clone(AppletOptionFactory)
            }
            if (!exportable.isNullOrEmpty()) {
                parentViewModel.requestAddNewTasks.value = exportable
            }
        }
        observe(viewModel.taskList) {
            if (it.isNotEmpty()) {
                binding.rvTaskList.adapter = adapter
                binding.tvTitle.append("（${it.size}）")
            }
        }
        observeTransient(parentViewModel.onNewTaskAdded) {
            adapter.notifyItemChanged(viewModel.taskList.value?.indexOf(it) ?: -1, true)
        }
    }

    fun setPreloadTaskMode() = doWhenCreated {
        viewModel.preloadTaskMode = true
        viewModel.title = R.string.preset_tasks.text
    }

    fun setExampleTaskMode() = doWhenCreated {
        viewModel.exampleTaskMode = true
        viewModel.title = R.string.example_tasks.text
    }

    fun setTitle(title: CharSequence?) = doWhenCreated {
        viewModel.title = title
    }

    fun setTaskList(list: List<XTask>) = doWhenCreated {
        viewModel.taskList.value = list
    }
}