package top.xjunz.tasker.ui.task.showcase

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import top.xjunz.tasker.databinding.DialogTaskCreatorBinding
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.ktx.observeTransient
import top.xjunz.tasker.ktx.show
import top.xjunz.tasker.ktx.toast
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.ui.task.editor.FlowEditorDialog
import top.xjunz.tasker.ui.task.editor.TaskMetadataEditor
import top.xjunz.tasker.util.AntiMonkeyUtil.setAntiMoneyClickListener

/**
 * @author xjunz 2022/12/14
 */
class TaskCreatorDialog : BaseDialogFragment<DialogTaskCreatorBinding>() {

    override val isFullScreen: Boolean = false

    private class InnerViewModel : ViewModel() {

        val onMetadataEdited = MutableLiveData<XTask.Metadata>()
    }

    private val viewModel by viewModels<InnerViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.containerLongRunning.setAntiMoneyClickListener {
            TaskMetadataEditor().init {
                viewModel.onMetadataEdited.value = it
            }.show(childFragmentManager)
        }
        binding.containerOneshot.setAntiMoneyClickListener {

        }
        binding.ibDismiss.setOnClickListener {
            dismiss()
        }
        observeTransient(viewModel.onMetadataEdited) {
            FlowEditorDialog().init(null, false).doOnCompletion {
                toast("Completed!")
            }.asBase(it).show(parentFragmentManager)
            dismiss()
        }
    }
}