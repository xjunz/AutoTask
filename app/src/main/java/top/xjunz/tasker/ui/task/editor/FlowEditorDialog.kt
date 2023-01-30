/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.editor

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.text.parseAsHtml
import androidx.core.view.*
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.transition.platform.MaterialFadeThrough
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.DialogFlowEditorBinding
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.applet.base.RootFlow
import top.xjunz.tasker.engine.applet.base.StaticError
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.task.applet.*
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.task.applet.option.AppletOptionFactory
import top.xjunz.tasker.task.applet.option.descriptor.ArgumentDescriptor
import top.xjunz.tasker.task.runtime.LocalTaskManager
import top.xjunz.tasker.ui.ColorScheme
import top.xjunz.tasker.ui.MainViewModel
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.ui.common.TextEditorDialog
import top.xjunz.tasker.ui.task.selector.AppletSelectorDialog
import top.xjunz.tasker.ui.task.showcase.TaskShowcaseDialog
import top.xjunz.tasker.ui.task.showcase.TaskShowcaseViewModel
import top.xjunz.tasker.util.AntiMonkeyUtil.setAntiMoneyClickListener
import top.xjunz.tasker.util.formatTime

/**
 * @author xjunz 2022/08/22
 */
class FlowEditorDialog : BaseDialogFragment<DialogFlowEditorBinding>() {

    private val vm by viewModels<FlowEditorViewModel>()

    val gvm get() = vm.global

    private val mvm by activityViewModels<MainViewModel>()

    private val factory = AppletOptionFactory

    private val adapter by lazy {
        TaskFlowAdapter(this)
    }

    private val menuHelper by lazy {
        AppletOperationMenuHelper(vm, childFragmentManager)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (vm.isSelectingArgument && !vm.hasCandidateReferents(vm.flow)) {
            toast(R.string.no_candidate_reference)
            dismiss()
            return null
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm.notifyFlowChanged()
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
        binding.appBar.addLiftOnScrollListener { _, _ ->
            if (!vm.isInTrackMode)
                binding.divider.isInvisible = binding.appBar.isLifted
        }
        binding.rvTaskEditor.adapter = adapter
        vm.isFabVisible.value =
            vm.isInEditionMode || (vm.isSelectingArgument && gvm.selectedReferents.isNotEmpty())
        if (vm.isInEditionMode) {
            binding.fabAction.text = R.string.add_rules.text
            binding.fabAction.setIconResource(R.drawable.ic_baseline_add_24)
        } else if (vm.isSelectingArgument) {
            binding.fabAction.text = R.string.confirm_ref.text
            binding.fabAction.setIconResource(R.drawable.ic_baseline_add_link_24)
        }
        binding.fabAction.setAntiMoneyClickListener {
            if (vm.isSelectingArgument) {
                if (gvm.selectedReferents.isNotEmpty())
                    confirmArgumentSelections()
            } else {
                val flow = vm.flow
                if (flow.size == flow.requiredSize) {
                    toast(R.string.error_reach_max_applet_size)
                    return@setAntiMoneyClickListener
                }
                AppletSelectorDialog().init(flow) {
                    vm.addInside(flow, it)
                }.show(childFragmentManager)
            }
        }
        binding.ibDismiss.setOnClickListener {
            onBackPressed()
        }
        binding.ibSplit.isVisible = vm.flow.isContainer && vm.isInEditionMode
        binding.ibSplit.setAntiMoneyClickListener {
            vm.showSplitConfirmation.value = true
        }
        binding.ibCheck.isVisible = !vm.isReadyOnly
        binding.ibCheck.setAntiMoneyClickListener {
            val error = vm.flow.performStaticCheck()
            vm.onAppletChanged.value = vm.staticError?.victim
            vm.staticError = error
            if (error != null && error.victim !== vm.flow) {
                toast(R.string.prompt_fix_static_error)
                if (!adapter.currentList.contains(error.victim)) {
                    if (error.victim.parent == null) {
                        // Too deep that the hierarchy is not yet built
                        vm.flow.buildHierarchy()
                    }
                    adapter.menuHelper.triggerMenuItem(
                        null, error.victim.requireParent(), R.id.item_open_in_new
                    )
                } else {
                    vm.onAppletChanged.value = error.victim
                    val item = binding.rvTaskEditor.findViewHolderForAdapterPosition(
                        adapter.currentList.indexOf(error.victim)
                    )?.itemView
                    if (item != null && item.isActivated) {
                        item.shake()
                    }
                }
            } else {
                if (vm.complete()) dismiss()
            }
        }
        if (vm.flow is RootFlow) {
            binding.rvBreadCrumbs.isVisible = false
        } else {
            binding.rvBreadCrumbs.isVisible = true
            binding.rvBreadCrumbs.adapter = FlowCascadeAdapter(vm)
        }
        binding.cvMetadata.setAntiMoneyClickListener {
            binding.ibSnapshotSelector.performClick()
        }
        binding.ibSnapshotSelector.setAntiMoneyClickListener {
            TaskSnapshotSelectorDialog().show(childFragmentManager)
        }
        if (vm.isInTrackMode) {
            binding.cvMetadata.isVisible = true
            binding.divider.isInvisible = true
            binding.appBarContainer.updateLayoutParams<AppBarLayout.LayoutParams> {
                scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or
                        AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP or
                        AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
            }
            binding.appBar.addOnOffsetChangedListener { _, verticalOffset ->
                val alpha = 1F - (-verticalOffset.toFloat() / binding.appBarContainer.height)
                binding.appBarContainer.alpha = alpha
            }
            binding.appBar.isLiftOnScroll = false
            binding.ibCheck.isVisible = true
            binding.ibCheck.setImageResource(R.drawable.ic_edit_24dp)
            val pvm = getParentViewModel<TaskShowcaseDialog, TaskShowcaseViewModel>()
            binding.ibCheck.setAntiMoneyClickListener {
                if (vm.isBase) {
                    pvm.requestEditTask.value = vm.task to null
                } else {
                    pvm.requestEditTask.value = vm.task to vm.flow
                }
            }
            observeTransient(pvm.requestEditTask) {
                dismiss()
            }
        } else if (vm.isBase) {
            binding.tvTitle.setAntiMoneyClickListener {
                TaskMetadataEditor().init(vm.metadata) {
                    vm.selectionLiveData.notifySelfChanged()
                }.show(childFragmentManager)
            }
        }
        navigateToDestFlow()
    }

    private fun navigateToDestFlow() {
        val dest = vm.flowToNavigate
        if (dest != -1L) {
            menuHelper.triggerMenuItem(null, vm.flow.findChild(dest), R.id.item_open_in_new)
            vm.flowToNavigate = -1L
        }
    }

    private fun confirmArgumentSelections() {
        val referentNamesList = gvm.getSelectedReferentNames()
        val referentNames = referentNamesList.toSet()
        // All referents have the same name, just do it!
        if (referentNames.size == 1 && gvm.selectedReferents.size == referentNamesList.size) {
            val referent = referentNames.single()
            gvm.setReferentForSelections(referent)
            vm.doOnArgSelected(referent)
            gvm.onReferentSelected.value = true
            return
        }
        val resultNames = gvm.selectedReferents.map { (applet, which) ->
            val option = AppletOptionFactory.requireOption(applet)
            option.results[which].name.toString()
        }.toSet()
        var def: String? = referentNames.singleOrNull()
        if (def == null && resultNames.size == 1) {
            def = resultNames.single()
        }
        val caption = if (gvm.selectedReferents.size > 1) {
            R.string.prompt_set_referent.text +
                    "\n\n" + R.string.help_multi_references.text.relativeSize(.8F)
                .quoted(ColorScheme.colorPrimary).bold()
        } else {
            R.string.prompt_set_referent.text
        }
        val dialog = TextEditorDialog().setCaption(caption).configEditText {
            it.setMaxLength(Applet.MAX_REFERENCE_ID_LENGTH)
        }.init(R.string.set_referent.text, def) {
            if (!gvm.isReferentLegalForSelections(it)) {
                return@init R.string.error_tag_exists.text
            }
            gvm.setReferentForSelections(it)
            gvm.renameReferentInRoot(referentNames, it)
            vm.doOnArgSelected(it)
            gvm.onReferentSelected.value = true
            return@init null
        }
        // Multiple referents
        dialog.setDropDownData((referentNames + resultNames).toSet().toTypedArray())
        dialog.show(childFragmentManager)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun observeLiveData() {
        observeNostalgic(vm.selectedApplet) { prev, cur ->
            if (prev != null)
                adapter.notifyItemChanged(adapter.currentList.indexOf(prev), true)
            adapter.notifyItemChanged(adapter.currentList.indexOf(cur), true)
        }
        observe(vm.applets) {
            adapter.submitList(it)
            if (binding.containerPlaceholder.isVisible != it.isEmpty()) {
                if (it.isEmpty()) {
                    binding.root.beginAutoTransition(
                        binding.containerPlaceholder, MaterialFadeThrough()
                    )
                }
                binding.containerPlaceholder.isVisible = it.isEmpty()
            }
        }
        observeTransient(vm.onAppletChanged) {
            adapter.notifyItemChanged(adapter.currentList.indexOf(it), true)
        }
        observeTransient(gvm.onAppletChanged) {
            adapter.notifyItemChanged(adapter.currentList.indexOf(it))
        }
        observe(vm.selectionLiveData) {
            if (vm.isSelectingArgument) {
                val name = vm.argumentDescriptor.name
                binding.tvTitle.text = R.string.format_select.formatSpans(name.foreColored())
            } else if (it.isEmpty()) {
                if (vm.isBase) {
                    if (!vm.isReadyOnly)
                        binding.tvTitle.setDrawableEnd(R.drawable.ic_baseline_chevron_right_24)
                    binding.tvTitle.text = vm.metadata.title
                } else {
                    binding.tvTitle.text = R.string.edit_rules.text
                }
                binding.ibDismiss.setContentDescriptionAndTooltip(R.string.dismiss.text)
            } else {
                binding.tvTitle.text = R.string.format_selection_count.format(it.size)
                binding.ibDismiss.setContentDescriptionAndTooltip(R.string.quit_multi_selection.text)
            }
            binding.ibMenu.isVisible = it.isNotEmpty()
            if (it.isNotEmpty()) {
                val popup = if (it.size > 1) menuHelper.createBatchMenu(binding.ibMenu, it)
                else menuHelper.createStandaloneMenu(binding.ibMenu, it.single())
                binding.ibMenu.setOnTouchListener(popup.dragToOpenListener)
                binding.ibMenu.setAntiMoneyClickListener {
                    popup.show()
                }
            }
        }
        observePrompt(vm.showTaskRepeatedPrompt, R.string.prompt_repeated_task)
        observeConfirmation(vm.showSplitConfirmation, R.string.prompt_split_container_flow) {
            vm.doSplit()
            dismiss()
        }
        observeConfirmation(vm.showMergeConfirmation, R.string.prompt_merge_applets) {
            vm.mergeSelectedApplets()
        }
        observeDialog(vm.showQuitConfirmation) {
            requireContext().makeSimplePromptDialog(msg = R.string.prompt_discard_flow_changes) {
                dismiss()
            }.setPositiveButton(R.string.do_not_save) { _, _ ->
                dismiss()
            }.setNegativeButton(android.R.string.cancel, null).show().also {
                it.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(ColorScheme.colorError)
            }
        }
        observeTransient(gvm.onReferentSelected) {
            if (vm.isSelectingArgument) dismiss()
        }
        val behavior = (binding.fabAction.layoutParams as CoordinatorLayout.LayoutParams).behavior
                as HideBottomViewOnScrollBehavior
        observe(vm.isFabVisible) {
            if (it != binding.fabAction.isVisible) {
                binding.root.beginAutoTransition(binding.fabAction, MaterialFadeThrough())
            }
            if (it) {
                behavior.slideUp(binding.fabAction, true)
            }
            binding.fabAction.isVisible = it
        }
        observeTransient(vm.onAppletLongClicked) {
            if (!vm.isReadyOnly && !vm.isInMultiSelectionMode) {
                vm.toggleMultiSelection(it)
                binding.appBar.setExpanded(true)
            }
        }
        observeNotNull(gvm.currentSnapshotIndex) {
            if (!vm.isInTrackMode) return@observeNotNull
            val snapshot = gvm.allSnapshots.require()[it]
            gvm.currentSnapshot = snapshot
            binding.tvSnapshotTitle.text = R.string.format_task_snapshots.format(
                it + 1, gvm.allSnapshots.require().size
            )
            binding.tvSnapshotDesc.text = R.string.format_task_snapshot_info_1.format(
                snapshot.startTimestamp.formatTime(),
                if (snapshot.isRunning) "-" else snapshot.endTimestamp.formatTime(),
                if (snapshot.isRunning) R.string.running.str
                else if (snapshot.isSuccessful) R.string.succeeded.str
                else R.string.failed.str
            ).parseAsHtml()
            adapter.notifyItemRangeChanged(0, adapter.currentList.size)
        }
        mvm.doOnAction(this, AppletOption.ACTION_TOGGLE_RELATION) {
            val hashcode = it.toInt()
            // May not found in this dialog, check it
            adapter.currentList.firstOrNull { applet ->
                applet.hashCode() == hashcode
            }?.run {
                toggleRelation()
                vm.onAppletChanged.value = this
            }
        }
        if (vm.isSelectingArgument) return
        mvm.doOnAction(this, AppletOption.ACTION_NAVIGATE_REFERENCE) { data ->
            val split = data.split(Char(0))
            val hashcode = split[1].toInt()
            val applet = adapter.currentList.firstOrNull {
                it.hashCode() == hashcode
            } ?: return@doOnAction
            val referent = split[0]
            val option = factory.requireOption(applet)
            val whichArg = applet.whichReference(referent)
            val arg = option.arguments[whichArg]
            FlowEditorDialog().init(vm.task, gvm.root, true, gvm)
                .setArgumentToSelect(applet, arg, referent)
                .doOnArgumentSelected { newReferent ->
                    gvm.referenceEditor.setReference(applet, arg, whichArg, newReferent)
                    vm.clearStaticErrorIfNeeded(applet, StaticError.PROMPT_RESET_REFERENCE)
                    gvm.referenceEditor.getReferenceChangedApplets().forEach {
                        vm.onAppletChanged.value = it
                    }
                    gvm.referenceEditor.reset()
                }.show(childFragmentManager)
        }
        mvm.doOnAction(this, AppletOption.ACTION_EDIT_VALUE) { data ->
            val hashcode = data.toInt()
            val applet = adapter.currentList.firstOrNull {
                it.hashCode() == hashcode
            } ?: return@doOnAction
            menuHelper.triggerMenuItem(null, applet, R.id.item_edit)
        }
    }

    override fun onBackPressed(): Boolean {
        if (vm.isInMultiSelectionMode) {
            vm.clearSelections()
        } else if (vm.isBase && !vm.isReadyOnly && vm.calculateChecksum() != vm.task.checksum) {
            vm.showQuitConfirmation.value = true
        } else {
            dismiss()
        }
        return true
    }

    fun setFlowToNavigate(dest: Long?): FlowEditorDialog {
        if (dest != null) {
            doWhenCreated {
                vm.flowToNavigate = dest
            }
        }
        return this
    }

    fun setArgumentToSelect(victim: Applet, ref: ArgumentDescriptor, referent: String?) =
        doWhenCreated {
            vm.referentAnchor = victim
            vm.argumentDescriptor = ref
            vm.isReadyOnly = true
            if (referent != null)
                gvm.selectReferentsWithName(victim, referent)
            vm.addCloseable {
                gvm.clearReferentSelections()
            }
        }

    fun doOnArgumentSelected(block: (String) -> Unit) = doWhenCreated {
        vm.doOnArgSelected = block
    }

    fun doAfterFlowEdited(block: (Flow) -> Unit) = doWhenCreated {
        vm.onFlowCompleted = block
    }

    fun doOnTaskEdited(block: () -> Unit) = doWhenCreated {
        vm.onTaskCompleted = block
    }

    fun doSplit(block: () -> Unit) = doWhenCreated {
        vm.doSplit = block
    }

    fun setTrackMode() = doWhenCreated {
        vm.isInTrackMode = true
        if (!gvm.allSnapshots.isNull()) return@doWhenCreated
        lifecycleScope.launch(Dispatchers.Default) {
            gvm.allSnapshots.postValue(LocalTaskManager.getAllSnapshots(vm.task))
            gvm.currentSnapshotIndex.postValue(0)
        }
    }

    fun setStaticError(error: StaticError?) = doWhenCreated {
        if (error != null)
            vm.staticError = StaticError(vm.flow[error.victim.index], error.code, error.arg)
    }

    fun initBase(task: XTask, readonly: Boolean) = doWhenCreated {
        if (task.flow == null) {
            task.flow = vm.generateDefaultFlow(task.metadata.taskType)
        }
        vm.task = task
        vm.initialize(task.requireFlow(), readonly)
        vm.global = viewModels<GlobalFlowEditorViewModel>().value
        gvm.root = vm.flow as RootFlow
    }

    fun init(
        task: XTask,
        flow: Flow,
        readonly: Boolean,
        gvm: GlobalFlowEditorViewModel
    ): FlowEditorDialog = doWhenCreated {
        vm.task = task
        vm.global = gvm
        vm.initialize(flow, readonly)
    }
}