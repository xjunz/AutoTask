package top.xjunz.tasker.ui.task.editor

import android.os.Bundle
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.transition.platform.MaterialFadeThrough
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.DialogFlowEditorBinding
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.ControlFlow
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.task.applet.nonContainerParent
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.task.applet.option.ValueDescriptor
import top.xjunz.tasker.task.applet.whichReference
import top.xjunz.tasker.ui.ColorScheme
import top.xjunz.tasker.ui.MainViewModel.Companion.peekMainViewModel
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.ui.common.TextEditorDialog
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
        initGlobalViewModel()
        initViews()
        observeLiveData()
    }

    private fun initGlobalViewModel() {
        if (viewModel.isBase) {
            globalViewModel.setRoot(viewModel.flow)
            viewModel.addCloseable {
                globalViewModel.clearRootFlow()
            }
        }
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
            if (viewModel.selections.size < 2) {
                toast(R.string.error_merge_single_applet)
            } else {
                viewModel.showMergeConfirmation.value = true
            }
        }
        viewModel.isFabVisible.value = viewModel.isEditingContainerFlow ||
                (viewModel.isSelectingRef && globalViewModel.selectedRefs.isNotEmpty())
        if (viewModel.isEditingContainerFlow) {
            binding.fabAction.text = R.string.add_rules.text
            binding.fabAction.setIconResource(R.drawable.ic_baseline_add_24)
        } else if (viewModel.isSelectingRef) {
            binding.fabAction.text = R.string.confirm_ref.text
            binding.fabAction.setIconResource(R.drawable.ic_baseline_add_link_24)
        }
        binding.fabAction.setOnClickListener {
            if (viewModel.isSelectingRef) {
                if (globalViewModel.selectedRefs.isNotEmpty())
                    confirmReferenceSelections()
            } else {
                AppletSelectorDialog().doOnCompletion {
                    viewModel.flow.addAll(it)
                    viewModel.notifyFlowChanged()
                }.scopedBy(viewModel.flow.nonContainerParent!!).show(childFragmentManager)
            }
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

    private fun confirmReferenceSelections() {
        val refids = globalViewModel.selectedRefs.mapNotNull { (applet, which) ->
            applet.refids[which]
        }
        val distinctRefids = refids.toSet()
        val caption = if (globalViewModel.selectedRefs.size > 1) {
            R.string.prompt_set_refid.text +
                    "\n\n" + R.string.help_multi_references.text.relativeSize(.8F)
                .quoted(ColorScheme.colorPrimary).bold()
        } else {
            R.string.prompt_set_refid.text
        }
        val dialog = TextEditorDialog().setCaption(caption).configEditText {
            it.setMaxLength(Applet.Configurator.MAX_REFERENCE_ID_LENGTH)
        }.init(R.string.set_refid.text, distinctRefids.singleOrNull()) {
            if (!globalViewModel.isRefidLegalForSelections(it)) {
                return@init R.string.error_tag_exists.text
            }
            globalViewModel.setRefidForSelections(it)
            globalViewModel.renameRefidInRoot(distinctRefids, it)
            viewModel.doOnRefSelected(it)
            globalViewModel.onReferenceSelected.value = true
            return@init null
        }
        if (refids.size == globalViewModel.selectedRefs.size && distinctRefids.size == 1) {
            // All applets have the same refid
            dialog.setDropDownData(arrayOf(distinctRefids.single()))
        } else if (distinctRefids.size > 1) {
            // Multiple refids
            dialog.setDropDownData(distinctRefids.toTypedArray())
        }
        dialog.show(childFragmentManager)
        /*if (applet.referred.containsKey(which)) {
            // Already a tag for this reference
            viewModel.doOnRefSelected(applet, which, applet.referred.getValue(which))
            // Notify ref selected
            globalViewModel.onReferenceSelected.value = true
        } else {

        }*/
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
            adapter.notifyItemChanged(viewModel.applets.require().indexOf(it))
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
        val behavior = (binding.fabAction.layoutParams as CoordinatorLayout.LayoutParams).behavior
                as HideBottomViewOnScrollBehavior
        observe(viewModel.isFabVisible) {
            if (it != binding.fabAction.isVisible) {
                binding.root.beginAutoTransition(binding.fabAction, MaterialFadeThrough())
            }
            if (it) {
                behavior.slideUp(binding.fabAction, true)
            }
            binding.fabAction.isVisible = it
        }
        val mainViewModel = peekMainViewModel()
        mainViewModel.doOnAction(this, AppletOption.ACTION_TOGGLE_RELATION) {
            val hashcode = it.toInt()
            // May not found in this dialog, check it
            viewModel.applets.require().firstOrNull { applet ->
                applet.hashCode() == hashcode
            }?.run {
                toggleRelation()
                viewModel.onAppletChanged.value = this
            }
        }
        if (!viewModel.isSelectingRef)
            mainViewModel.doOnAction(this, AppletOption.ACTION_NAVIGATE_REFERENCE) { data ->
                val split = data.split(Char(0))
                val hashcode = split[1].toInt()
                val applet = adapter.currentList.firstOrNull {
                    it.hashCode() == hashcode
                } ?: return@doOnAction
                val refid = split[0]
                val option = globalViewModel.factory.requireOption(applet)
                val arg = option.arguments[applet.whichReference(refid)]
                FlowEditorDialog().setFlow(globalViewModel.root, true)
                    .setReferenceToSelect(applet, arg, refid)
                    .doOnReferenceSelected {
                        globalViewModel.tracer.getReferenceChangedApplets().forEach {
                            viewModel.onAppletChanged.value = it
                        }
                        globalViewModel.tracer.reset()
                    }.show(childFragmentManager)
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

    fun setReferenceToSelect(victim: Applet, ref: ValueDescriptor, refid: String?) = doWhenCreated {
        viewModel.refSelectingApplet = victim
        viewModel.refValueDescriptor = ref
        viewModel.isReadyOnly = true
        if (refid != null)
            globalViewModel.addRefSelectionWithRefid(victim, refid)
        viewModel.addCloseable {
            globalViewModel.clearRefSelections()
        }
    }

    fun doOnReferenceSelected(block: (String) -> Unit) = doWhenCreated {
        viewModel.doOnRefSelected = block
    }

    fun doOnCompletion(block: (Flow) -> Unit) = doWhenCreated {
        viewModel.doOnCompletion = block
    }

    fun doSplit(block: () -> Unit) = doWhenCreated {
        viewModel.doSplit = block
    }

    fun asBase() = doWhenCreated {
        viewModel.isBase = true
    }

    fun setFlow(flow: Flow?, readonly: Boolean) = doWhenCreated {
        val root = flow ?: globalViewModel.generateDefaultFlow()
        viewModel.initialize(globalViewModel.factory, root, flow == null, readonly)
    }
}