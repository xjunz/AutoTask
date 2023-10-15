/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.showcase

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import top.xjunz.tasker.BuildConfig
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.DialogTaskCreatorBinding
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.ktx.observeTransient
import top.xjunz.tasker.ktx.show
import top.xjunz.tasker.ktx.str
import top.xjunz.tasker.ktx.toast
import top.xjunz.tasker.ui.base.BaseBottomSheetDialog
import top.xjunz.tasker.ui.main.MainViewModel.Companion.peekMainViewModel
import top.xjunz.tasker.ui.task.editor.FlowEditorDialog
import top.xjunz.tasker.ui.task.editor.TaskMetadataEditor
import top.xjunz.tasker.util.ClickListenerUtil.setNoDoubleClickListener

/**
 * @author xjunz 2022/12/14
 */
class TaskCreatorDialog : BaseBottomSheetDialog<DialogTaskCreatorBinding>(),
    ActivityResultCallback<Intent?> {

    companion object {
        var REQUESTED_QUICK_TASK_CREATOR = -1
        const val QUICK_TASK_CREATOR_CLICK_AUTOMATION = 1
        const val QUICK_TASK_CREATOR_GESTURE_RECORDER = 2
        const val QUICK_TASK_CREATOR_AUTO_CLICK = 3
    }

    private class InnerViewModel : ViewModel() {

        val onMetadataEdited = MutableLiveData<XTask.Metadata>()
    }

    private val parentViewModel by activityViewModels<TaskShowcaseViewModel>()

    private val viewModel by viewModels<InnerViewModel>()

    private lateinit var selectTaskFromSAFLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectTaskFromSAFLauncher = registerForActivityResult(
            object : ActivityResultContract<String, Intent?>() {
                override fun createIntent(context: Context, input: String): Intent {
                    return Intent(Intent.ACTION_OPEN_DOCUMENT)
                        .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("*/*")
                }

                override fun parseResult(resultCode: Int, intent: Intent?): Intent? {
                    if (resultCode == Activity.RESULT_CANCELED || intent == null) {
                        toast(R.string.cancelled)
                    }
                    return intent
                }
            }, this
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        binding.containerResident.setNoDoubleClickListener {
            val metadata = XTask.Metadata(R.string.unnamed_task.str)
            metadata.version = BuildConfig.VERSION_CODE
            TaskMetadataEditor().init(metadata) {
                viewModel.onMetadataEdited.value = metadata
            }.show(childFragmentManager)
        }
        binding.containerOneshot.setNoDoubleClickListener {
            val metadata = XTask.Metadata(R.string.unnamed_task.str, XTask.TYPE_ONESHOT)
            metadata.version = BuildConfig.VERSION_CODE
            TaskMetadataEditor().init(metadata) {
                viewModel.onMetadataEdited.value = metadata
            }.show(childFragmentManager)
        }
        binding.containerImportTasks.setNoDoubleClickListener {
            selectTaskFromSAFLauncher.launch("")
        }
        binding.tvClickAutomation.setNoDoubleClickListener {
            REQUESTED_QUICK_TASK_CREATOR = QUICK_TASK_CREATOR_CLICK_AUTOMATION
            viewModel.onMetadataEdited.value =
                XTask.Metadata(R.string.click_automation.str, XTask.TYPE_ONESHOT)
            dismiss()
        }
        binding.tvRecordGesture.setNoDoubleClickListener {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                toast(R.string.tip_api_too_low)
                return@setNoDoubleClickListener
            }
            REQUESTED_QUICK_TASK_CREATOR = QUICK_TASK_CREATOR_GESTURE_RECORDER
            viewModel.onMetadataEdited.value =
                XTask.Metadata(R.string.perform_custom_gestures.str, XTask.TYPE_ONESHOT)
            dismiss()
        }
        binding.btnAutoClick.setNoDoubleClickListener {
            REQUESTED_QUICK_TASK_CREATOR = QUICK_TASK_CREATOR_AUTO_CLICK
            viewModel.onMetadataEdited.value =
                XTask.Metadata(R.string.auto_click.str, XTask.TYPE_RESIDENT)
            dismiss()
        }
        binding.containerPreloadTasks.setNoDoubleClickListener {
            TaskCollectionSelectorDialog().show(requireActivity().supportFragmentManager)
        }
        observeTransient(viewModel.onMetadataEdited) { metadata ->
            val task = XTask()
            task.metadata = metadata
            FlowEditorDialog().initBase(task, false).doOnTaskEdited {
                parentViewModel.requestAddNewTasks.value = listOf(task)
            }.show(parentFragmentManager)
        }
        observeTransient(parentViewModel.onNewTaskAdded) {
            dismiss()
        }
    }

    override fun onActivityResult(result: Intent?) {
        peekMainViewModel().requestImportTask.value = result
    }
}