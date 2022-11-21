package top.xjunz.tasker.ui.task.editor

import android.os.Bundle
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.DialogFlowEditorBinding
import top.xjunz.tasker.engine.applet.base.ControlFlow
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.task.applet.nonContainerParent
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.ui.task.selector.AppletSelectorDialog

/**
 * @author xjunz 2022/08/22
 */
class FlowEditorDialog : BaseDialogFragment<DialogFlowEditorBinding>() {

    private val viewModel by viewModels<FlowEditorViewModel>()

    private val adapter by lazy {
        TaskFlowAdapter(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(savedInstanceState)
        observeLiveData()
    }

    private fun initViews(savedInstanceState: Bundle?) {
        binding.fabAddInside.applySystemInsets { v, insets ->
            v.updateLayoutParams<MarginLayoutParams> {
                bottomMargin = insets.bottom + 16.dp
            }
        }
        binding.appBar.applySystemInsets { v, insets ->
            v.updatePadding(top = insets.top)
        }
        binding.rvTaskEditor.adapter = adapter
        if (viewModel.isNewTask && savedInstanceState == null)
            viewModel.generateDefaultFlow()

        binding.ibCombine.setOnClickListener {
            viewModel.mergeSelectedApplets()
        }
        binding.fabAddInside.isVisible = viewModel.isEditingContainerFlow
        binding.fabAddInside.setOnClickListener {
            AppletSelectorDialog().doOnCompletion {
                viewModel.flow.addAll(it)
                viewModel.notifyFlowChanged()
            }.scopedBy(viewModel.flow.nonContainerParent!!).show(parentFragmentManager)
        }
        binding.ibDismiss.setOnClickListener {
            onBackPressed()
        }
        binding.ibSplit.isVisible = viewModel.isEditingContainerFlow
        binding.ibSplit.setOnClickListener {
            viewModel.showSplitConfirmation.value = true
        }
        binding.ibCheck.setOnClickListener {
            viewModel.complete()
            dismiss()
        }
    }

    private fun observeLiveData() {
        observeNostalgic(viewModel.selectedApplet) { prev, cur ->
            if (prev != null)
                adapter.notifyItemChanged(viewModel.applets.require().indexOf(prev), true)
            adapter.notifyItemChanged(viewModel.applets.require().indexOf(cur), true)
        }
        observe(viewModel.applets) {
            adapter.submitList(it)
        }
        observeTransient(viewModel.changedApplet) {
            adapter.notifyItemChanged(viewModel.applets.require().indexOf(it), true)
        }
        observe(viewModel.selectionLiveData) {
            if (it.isEmpty()) {
                if (viewModel.isNewTask) {
                    binding.tvTitle.text = R.string.create_task.text
                } else {
                    binding.tvTitle.text = R.string.edit_task.text
                }
                binding.ibDismiss.setContentDescriptionAndTooltip(R.string.dismiss.text)
            } else {
                binding.tvTitle.text = R.string.format_selection_count.format(it.size)
                binding.ibDismiss.setContentDescriptionAndTooltip(R.string.quit_multi_selection.text)
            }
            val showMergeBtn = it.isNotEmpty() && it.first() !is ControlFlow
            if (binding.ibCombine.isVisible != showMergeBtn) {
                binding.appBar.beginAutoTransition()
                binding.ibCombine.isVisible = showMergeBtn
            }
        }
        observeConfirmation(viewModel.showSplitConfirmation, R.string.prompt_split_container_flow) {
            viewModel.notifySplit()
            dismiss()
        }
    }

    override fun onBackPressed(): Boolean {
        if (viewModel.isInMultiSelectionMode) {
            viewModel.clearSelections()
        } else {
            dismiss()
        }
        return true
    }


    fun doOnCompletion(block: (Flow) -> Unit) = doWhenCreated {
        viewModel.onCompletion = block
    }

    fun doSplit(block: () -> Unit) = doWhenCreated {
        viewModel.doSplit = block
    }

    fun setFlow(flow: Flow) = doWhenCreated {
        viewModel.initWith(flow)
    }

}