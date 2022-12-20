package top.xjunz.tasker.ui.task.showcase

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.DialogTaskCreatorBinding
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.ktx.observeTransient
import top.xjunz.tasker.ktx.show
import top.xjunz.tasker.ktx.str
import top.xjunz.tasker.ui.base.BaseBottomSheetDialog
import top.xjunz.tasker.ui.task.editor.FlowEditorDialog
import top.xjunz.tasker.ui.task.editor.TaskMetadataEditor
import top.xjunz.tasker.util.AntiMonkeyUtil.setAntiMoneyClickListener

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
            TaskMetadataEditor().init(XTask.Metadata(R.string.unnamed_task.str)) {
                viewModel.onMetadataEdited.value = it
            }.show(childFragmentManager)
        }
        binding.containerOneshot.setAntiMoneyClickListener {
            TaskMetadataEditor().init(
                XTask.Metadata(R.string.unnamed_task.str, XTask.TYPE_ONESHOT)
            ) {
                viewModel.onMetadataEdited.value = it
            }.show(childFragmentManager)
        }
        binding.containerImportTasks.setAntiMoneyClickListener {

        }
        binding.containerClickMode.setAntiMoneyClickListener {

        }
        binding.containerRecordGesture.setAntiMoneyClickListener {

        }
        binding.containerPreloadTasks.setAntiMoneyClickListener {
            PreloadTaskDialog().show(requireParentFragment().childFragmentManager)
        }
        observeTransient(viewModel.onMetadataEdited) { metadata ->
            FlowEditorDialog().init(null, false).doOnCompletion { flow ->
                XTask().let {
                    metadata.creationTimestamp = System.currentTimeMillis()
                    metadata.modificationTimestamp = metadata.creationTimestamp
                    it.metadata = metadata
                    it.flow = flow.casted()
                    parentViewModel.requestAddNewTask.value = it
                }
            }.asBase(metadata).show(parentFragmentManager)
        }
        observeTransient(parentViewModel.onNewTaskAdded) {
            dismiss()
        }
    }
}