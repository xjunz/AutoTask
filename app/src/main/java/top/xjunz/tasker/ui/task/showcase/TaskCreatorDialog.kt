/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.showcase

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.DialogTaskCreatorBinding
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.ktx.observeTransient
import top.xjunz.tasker.ktx.show
import top.xjunz.tasker.ktx.str
import top.xjunz.tasker.task.inspector.InspectorMode
import top.xjunz.tasker.ui.base.BaseBottomSheetDialog
import top.xjunz.tasker.ui.task.editor.FlowEditorDialog
import top.xjunz.tasker.ui.task.editor.TaskMetadataEditor
import top.xjunz.tasker.ui.task.inspector.FloatingInspectorDialog
import top.xjunz.tasker.util.ClickUtil.setAntiMoneyClickListener

/**
 * @author xjunz 2022/12/14
 */
class TaskCreatorDialog : BaseBottomSheetDialog<DialogTaskCreatorBinding>() {

    private class InnerViewModel : ViewModel() {

        val onMetadataEdited = MutableLiveData<XTask.Metadata>()
    }

    private val parentViewModel by lazy {
        requireParentFragment().viewModels<TaskShowcaseViewModel>().value
    }

    private val viewModel by viewModels<InnerViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.containerResidentTasks.setAntiMoneyClickListener {
            val metadata = XTask.Metadata(R.string.unnamed_task.str)
            TaskMetadataEditor().init(metadata) {
                viewModel.onMetadataEdited.value = metadata
            }.show(childFragmentManager)
        }
        binding.containerOneshot.setAntiMoneyClickListener {
            val metadata = XTask.Metadata(R.string.unnamed_task.str, XTask.TYPE_ONESHOT)
            TaskMetadataEditor().init(metadata) {
                viewModel.onMetadataEdited.value = metadata
            }.show(childFragmentManager)
        }
        binding.containerImportTasks.setAntiMoneyClickListener {

        }
        binding.tvClickMode.setAntiMoneyClickListener {

        }
        binding.tvRecordGesture.setAntiMoneyClickListener {
            FloatingInspectorDialog().setMode(InspectorMode.GESTURE_RECORDER)
                .show(childFragmentManager)
        }
        binding.containerPreloadTasks.setAntiMoneyClickListener {
            PreloadTaskDialog().show(requireParentFragment().childFragmentManager)
        }
        observeTransient(viewModel.onMetadataEdited) { metadata ->
            val task = XTask()
            task.metadata = metadata
            FlowEditorDialog().initBase(task, false).doOnTaskEdited {
                parentViewModel.requestAddNewTask.value = task
            }.show(parentFragmentManager)
        }
        observeTransient(parentViewModel.onNewTaskAdded) {
            dismiss()
        }
    }
}