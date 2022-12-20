package top.xjunz.tasker.ui.task.editor

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.DialogTaskMetadataEditorBinding
import top.xjunz.tasker.engine.task.XTask.Metadata
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.util.AntiMonkeyUtil.setAntiMoneyClickListener

/**
 * @author xjunz 2022/12/15
 */
class TaskMetadataEditor : BaseDialogFragment<DialogTaskMetadataEditorBinding>() {

    override val isFullScreen: Boolean = false

    private class InnerViewModel : ViewModel() {

        lateinit var onCompletion: (Metadata) -> Unit

        lateinit var metadata: Metadata
    }

    private val viewModel by viewModels<InnerViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showSoftInput(binding.etTaskName)
        binding.btnCancel.setAntiMoneyClickListener {
            dismiss()
        }
        binding.etTaskName.setText(viewModel.metadata.title)
        binding.etTaskDesc.setText(viewModel.metadata.description)
        if (viewModel.metadata.title == R.string.unnamed_task.str) {
            binding.etTaskName.selectAll()
        } else {
            binding.etTaskName.setSelectionToEnd()
        }
        binding.btnComplete.setAntiMoneyClickListener {
            if (binding.etTaskName.textString.isEmpty()) {
                toast(R.string.error_empty_input)
                binding.etTaskName.shake()
                return@setAntiMoneyClickListener
            }
            viewModel.metadata.title = binding.etTaskName.textString
            viewModel.metadata.description = binding.etTaskDesc.textString
            viewModel.onCompletion(viewModel.metadata)
            dismiss()
        }
    }

    fun init(initialMetadata: Metadata, doOnCompletion: (Metadata) -> Unit) =
        doWhenCreated {
            viewModel.metadata = initialMetadata
            viewModel.onCompletion = doOnCompletion
        }
}