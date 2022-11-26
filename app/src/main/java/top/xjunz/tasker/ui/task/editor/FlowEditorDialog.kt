package top.xjunz.tasker.ui.task.editor

import android.os.Bundle
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.DialogFlowEditorBinding
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.ControlFlow
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.task.applet.nonContainerParent
import top.xjunz.tasker.task.applet.option.ValueDescriptor
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.ui.task.selector.AppletSelectorDialog

/**
 * @author xjunz 2022/08/22
 */
class FlowEditorDialog : BaseDialogFragment<DialogFlowEditorBinding>() {

    private val viewModel by viewModels<FlowEditorViewModel>()

    private val globalViewModel by activityViewModels<GlobalFlowEditorViewModel>()

    private val adapter by lazy {
        TaskFlowAdapter(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        observeLiveData()
    }

    private fun initViews() {
        binding.fabAction.applySystemInsets { v, insets ->
            v.updateLayoutParams<MarginLayoutParams> {
                bottomMargin = insets.bottom + 16.dp
            }
        }
        binding.appBar.applySystemInsets { v, insets ->
            v.updatePadding(top = insets.top)
        }
        binding.rvTaskEditor.adapter = adapter

        binding.ibMerge.setOnClickListener {
            viewModel.showMergeConfirmation.value = true
        }
        binding.fabAction.isVisible = viewModel.isEditingContainerFlow && !viewModel.isReadyOnly
        binding.fabAction.setOnClickListener {
            AppletSelectorDialog().doOnCompletion {
                viewModel.flow.addAll(it)
                viewModel.notifyFlowChanged()
            }.scopedBy(viewModel.flow.nonContainerParent!!).show(childFragmentManager)
        }
        binding.ibDismiss.setOnClickListener {
            onBackPressed()
        }
        binding.ibSplit.isVisible = viewModel.isEditingContainerFlow && !viewModel.isReadyOnly
        binding.ibSplit.setOnClickListener {
            viewModel.showSplitConfirmation.value = true
        }
        binding.ibCheck.isVisible = !viewModel.isReadyOnly
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
        observeTransient(viewModel.onAppletChanged) {
            adapter.notifyItemChanged(viewModel.applets.require().indexOf(it), true)
        }
        observeTransient(globalViewModel.onAppletChanged) {
            viewModel.onAppletChanged.value = it
        }
        observe(viewModel.selectionLiveData) {
            if (viewModel.isSelectingRef) {
                if (viewModel.isSelectingRef
                    && !viewModel.hasCandidateReference(viewModel.flow)
                ) {
                    toast(R.string.no_candidate_reference)
                    binding.tvTitle.text = R.string.no_candidate_reference.text
                } else {
                    val refName = viewModel.refValueDescriptor.name
                    binding.tvTitle.text = R.string.format_select.format(refName)
                }
            } else if (it.isEmpty()) {
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
            if (binding.ibMerge.isVisible != showMergeBtn) {
                binding.appBar.beginAutoTransition()
                binding.ibMerge.isVisible = showMergeBtn
            }
        }
        observeConfirmation(viewModel.showSplitConfirmation, R.string.prompt_split_container_flow) {
            viewModel.notifySplit()
            dismiss()
        }
        observeConfirmation(viewModel.showMergeConfirmation, R.string.prompt_merge_applets) {
            viewModel.mergeSelectedApplets()
        }
        observeTransient(globalViewModel.onReferenceSelected) {
            if (viewModel.isSelectingRef) dismiss()
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

    fun setReferenceToSelect(victim: Applet, ref: ValueDescriptor) = doWhenCreated {
        viewModel.refSelectingApplet = victim
        viewModel.refValueDescriptor = ref
        viewModel.isReadyOnly = true
    }

    fun doOnReferenceSelected(block: (Applet, Int, String) -> Unit) = doWhenCreated {
        viewModel.doOnRefSelected = block
    }

    fun doOnCompletion(block: (Flow) -> Unit) = doWhenCreated {
        viewModel.doOnCompletion = block
    }

    fun doSplit(block: () -> Unit) = doWhenCreated {
        viewModel.doSplit = block
    }

    fun setFlow(flow: Flow?, readonly: Boolean) = doWhenCreated {
        val root = flow ?: globalViewModel.generateDefaultFlow()
        viewModel.initialize(globalViewModel.factory, root, flow == null, readonly)
        if (globalViewModel.setRootFlowIfAbsent(viewModel.flow)) {
            viewModel.addCloseable {
                globalViewModel.clearRootFlow()
            }
        }
    }

}