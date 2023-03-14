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
import androidx.core.view.*
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.transition.platform.MaterialFadeThrough
import io.ktor.util.reflect.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import top.xjunz.tasker.Preferences
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.DialogFlowEditorBinding
import top.xjunz.tasker.engine.applet.action.Action
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.applet.base.RootFlow
import top.xjunz.tasker.engine.applet.base.StaticError
import top.xjunz.tasker.engine.applet.util.buildHierarchy
import top.xjunz.tasker.engine.applet.util.findChildByHierarchy
import top.xjunz.tasker.engine.applet.util.isContainer
import top.xjunz.tasker.engine.applet.util.whichReference
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.task.applet.*
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.task.applet.option.AppletOptionFactory
import top.xjunz.tasker.task.applet.option.descriptor.ArgumentDescriptor
import top.xjunz.tasker.task.runtime.LocalTaskManager
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.ui.common.PreferenceHelpDialog
import top.xjunz.tasker.ui.common.TextEditorDialog
import top.xjunz.tasker.ui.main.ColorScheme
import top.xjunz.tasker.ui.main.EventCenter.doOnEventReceived
import top.xjunz.tasker.ui.task.selector.AppletSelectorDialog
import top.xjunz.tasker.ui.task.showcase.TaskCreatorDialog
import top.xjunz.tasker.util.ClickListenerUtil.setNoDoubleClickListener
import top.xjunz.tasker.util.formatTime

/**
 * @author xjunz 2022/08/22
 */
class FlowEditorDialog : BaseDialogFragment<DialogFlowEditorBinding>() {

    val gvm get() = vm.global

    private val vm by viewModels<FlowEditorViewModel>()

    private val factory = AppletOptionFactory

    private var confirmed = false

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
        if (vm.isSelectingReferent && !vm.hasCandidateReferents(vm.flow)) {
            toast(R.string.format_no_candidate_referent.format(vm.referentDescriptor.name))
            dismiss()
            return null
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm.notifyFlowChanged()
        initViews(savedInstanceState)
        observeLiveData()
    }

    private fun initViews(savedInstanceState: Bundle?) {
        binding.fabAction.applySystemInsets { v, insets ->
            v.updateLayoutParams<MarginLayoutParams> {
                bottomMargin = insets.bottom + 16.dp
            }
        }
        binding.appBar.applySystemInsets { v, insets ->
            v.updatePadding(top = insets.top)
        }
        binding.appBar.addLiftOnScrollListener { _, _ ->
            if (!vm.isInTrackMode) {
                binding.divider.isInvisible = binding.appBar.isLifted
            }
        }
        binding.rvTaskEditor.applySystemInsets { v, insets ->
            v.updatePadding(bottom = insets.bottom + 16.dp)
        }
        if (savedInstanceState == null) {
            binding.rvTaskEditor.post {
                binding.rvTaskEditor.adapter = adapter
            }
        } else {
            binding.rvTaskEditor.adapter = adapter
        }
        vm.isFabVisible.value =
            vm.isInEditionMode || (vm.isSelectingReferent && gvm.selectedReferents.isNotEmpty())
        if (vm.isSelectingReferent) {
            binding.fabAction.text = R.string.confirm_reference.text
            binding.fabAction.setIconResource(R.drawable.ic_baseline_add_link_24)
        }
        binding.fabAction.setNoDoubleClickListener {
            if (vm.isSelectingReferent) {
                if (gvm.selectedReferents.isNotEmpty()) {
                    confirmArgumentSelections()
                }
            } else {
                val flow = vm.flow
                if (flow.size == flow.maxSize) {
                    toast(R.string.format_error_reach_max_applet_size.format(flow.maxSize))
                    return@setNoDoubleClickListener
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
        binding.ibSplit.setNoDoubleClickListener {
            vm.showSplitConfirmation.value = true
        }
        binding.ibCheck.isVisible = !vm.isReadyOnly
        binding.ibCheck.setNoDoubleClickListener {
            val error = vm.flow.performStaticCheck()
            vm.onAppletChanged.value = vm.staticError?.victim
            vm.staticError = error
            if (error != null && error.victim !== vm.flow) {
                if (!adapter.currentList.contains(error.victim)) {
                    if (error.victim.parent == null) {
                        // Too deep that the hierarchy is not yet built
                        vm.flow.buildHierarchy()
                    }
                    adapter.menuHelper.triggerMenuItem(
                        null, error.victim.requireParent(), R.id.item_open_in_new
                    )
                    toast(R.string.prompt_fix_static_errors)
                } else {
                    vm.onAppletChanged.value = error.victim
                    val i = adapter.currentList.indexOf(error.victim)
                    binding.rvTaskEditor.scrollPositionToCenterVertically(i, true) {
                        it.shake()
                        toast(R.string.prompt_fix_static_errors)
                    }
                }
            } else {
                if (vm.complete()) {
                    confirmed = true
                    dismiss()
                }
            }
        }
        if (vm.flow is RootFlow) {
            binding.rvBreadCrumbs.isVisible = false
        } else {
            binding.rvBreadCrumbs.isVisible = true
            binding.rvBreadCrumbs.adapter = FlowCascadeAdapter(vm)
        }
        if (vm.isInTrackMode) {
            showHeader()
            binding.cvHeader.setNoDoubleClickListener {
                binding.ibHeaderAction.performClick()
            }
            binding.ibHeaderAction.setNoDoubleClickListener {
                TaskSnapshotSelectorDialog().show(childFragmentManager)
            }
            binding.ibCheck.isVisible = true
            binding.ibCheck.setIconResource(R.drawable.ic_clear_all_24px)
            binding.ibCheck.setNoDoubleClickListener {
                vm.showClearSnapshotsConfirmation.value = true
            }
            binding.ibCheck.post {
                binding.ibCheck.setContentDescriptionAndTooltip(R.string.clear_all.text)
            }
            binding.ibMenu.isVisible = true
            binding.ibMenu.setIconResource(R.drawable.ic_description_24px)
            binding.ibMenu.post {
                binding.ibMenu.setContentDescriptionAndTooltip(R.string.log.text)
            }
            binding.ibMenu.setNoDoubleClickListener {
                if (gvm.currentSnapshot?.log.isNullOrEmpty()) {
                    toast(R.string.no_log)
                } else {
                    SnapshotLogDialog().setSnapshot(vm.task.title, gvm.currentSnapshot!!)
                        .show(childFragmentManager)
                }
            }
        } else if (vm.isSelectingReferent) {
            observeOnce(vm.requestShowReferentTip) {
                showHeader()
                binding.tvHeaderTitle.isVisible = false
                binding.ibHeaderAction.isVisible = false
                binding.tvHeaderDesc.setTextColor(ColorScheme.textColorPrimary)
                binding.tvHeaderDesc.setText(R.string.tip_selecting_referents)
            }
        } else if (vm.isBase) {
            binding.tvTitle.setNoDoubleClickListener {
                TaskMetadataEditor().init(vm.metadata) {
                    vm.selectionLiveData.notifySelfChanged()
                }.show(childFragmentManager)
            }
            navigateToDestFlow()
        }
        when (TaskCreatorDialog.REQUESTED_QUICK_TASK_CREATOR) {
            TaskCreatorDialog.QUICK_TASK_CREATOR_CLICK_AUTOMATION -> {
                menuHelper.triggerMenuItem(null, (vm.flow[1] as Flow)[0], R.id.item_add_before)
            }
            TaskCreatorDialog.QUICK_TASK_CREATOR_GESTURE_RECORDER -> {
                binding.fabAction.performClick()
            }
        }
    }

    private fun showHeader() {
        binding.cvHeader.isVisible = true
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
    }

    private fun navigateToDestFlow() {
        val dest = vm.flowToNavigate
        if (dest != -1L) {
            menuHelper.triggerMenuItem(
                null, vm.flow.findChildByHierarchy(dest), R.id.item_open_in_new
            )
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
        TextEditorDialog().setCaption(caption).configEditText {
            it.setMaxLength(Applet.MAX_REFERENCE_ID_LENGTH)
        }.init(R.string.set_referent.text, def) {
            if (!gvm.isReferentLegalForSelections(it)) {
                return@init R.string.error_tag_exists.text
            }
            gvm.renameReferentInRoot(referentNames, it)
            // Because current flow is a clone, so renaming in root will not
            // affect current flow, still need a rename.
            gvm.setReferentForSelections(it)
            vm.doOnArgSelected(it)
            gvm.onReferentSelected.value = true
            return@init null
        }.setDropDownData((referentNames + resultNames).toSet().toTypedArray())
            .show(childFragmentManager)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun observeLiveData() {
        observeNostalgic(vm.selectedApplet) { prev, cur ->
            if (prev != null) {
                adapter.notifyItemChanged(adapter.currentList.indexOf(prev), true)
            }
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
                vm.isFabVisible.value = it.isEmpty()
            }
        }
        observeTransient(vm.onAppletChanged) {
            adapter.notifyItemChanged(adapter.currentList.indexOf(it), true)
        }
        observeTransient(gvm.onAppletChanged) {
            adapter.notifyItemChanged(adapter.currentList.indexOf(it), true)
        }
        observe(vm.selectionLiveData) {
            if (vm.isSelectingReferent) {
                val name = vm.referentDescriptor.name
                binding.tvTitle.text = R.string.format_select.formatSpans(name.foreColored())
            } else if (it.isEmpty()) {
                if (vm.isBase) {
                    if (!vm.isReadyOnly) {
                        binding.tvTitle.setDrawableEnd(R.drawable.ic_chevron_right_24px)
                    }
                    binding.tvTitle.text = vm.metadata.title
                } else {
                    if (vm.isInTrackMode) {
                        binding.tvTitle.text = vm.metadata.title
                    } else {
                        binding.tvTitle.text = R.string.edit_rules.text
                    }
                }
                binding.ibDismiss.setContentDescriptionAndTooltip(R.string.dismiss.text)
            } else {
                binding.tvTitle.text = R.string.format_selection_count.format(it.size)
                binding.ibDismiss.setContentDescriptionAndTooltip(R.string.quit_multi_selection.text)
            }
            if (!vm.isInTrackMode) {
                binding.ibMenu.isVisible = it.isNotEmpty()
                if (it.isNotEmpty()) {
                    val popup = if (it.size > 1) menuHelper.createBatchMenu(binding.ibMenu, it)
                    else menuHelper.createStandaloneMenu(binding.ibMenu, it.single())
                    binding.ibMenu.setOnTouchListener(popup.dragToOpenListener)
                    binding.ibMenu.setNoDoubleClickListener {
                        popup.show()
                    }
                }
            }
        }
        observePrompt(vm.showTaskRepeatedPrompt, R.string.prompt_repeated_task)
        observeDialog(vm.showTitleRepeatedPrompt) { title ->
            requireContext().makeSimplePromptDialog(
                msg = R.string.prompt_task_title_repeated.formatSpans(title.bold())
            ).setPositiveButton(R.string.ignore_and_continue) { _, _ ->
                vm.ignoreRepeatedTitle = true
                binding.ibCheck.performClick()
            }.setNegativeButton(R.string.rename) { _, _ ->
                binding.tvTitle.performClick()
            }.show().also {
                it.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(ColorScheme.colorError)
            }
        }
        observeConfirmation(vm.showSplitConfirmation, R.string.prompt_split_container_flow) {
            vm.doSplit()
            dismiss()
        }
        observeConfirmation(vm.showMergeConfirmation, R.string.prompt_merge_applets) {
            vm.mergeSelectedApplets()
        }
        observeDangerousConfirmation(
            vm.showQuitConfirmation,
            R.string.prompt_discard_flow_changes,
            R.string.do_not_save
        ) {
            dismiss()
        }
        observeDangerousConfirmation(
            vm.showClearSnapshotsConfirmation,
            R.string.prompt_clear_snapshots,
            R.string.clear_all
        ) {
            LocalTaskManager.clearSnapshots(vm.task)
            gvm.onSnapshotsCleared.value = true
        }
        observeTransient(gvm.onSnapshotsCleared) {
            dismiss()
        }
        observeTransient(gvm.onReferentSelected) {
            if (vm.isSelectingReferent) dismiss()
        }
        val behavior = (binding.fabAction.layoutParams as CoordinatorLayout.LayoutParams).behavior
                as HideBottomViewOnScrollBehavior
        observe(vm.isFabVisible) { show ->
            binding.root.beginAutoTransition(binding.fabAction, MaterialFadeThrough())
            if (show) behavior.slideUp(binding.fabAction, true)
            binding.fabAction.isVisible = show
            if (show) {
                binding.fabAction.doOnPreDraw {
                    binding.rvTaskEditor.updatePadding(bottom = it.height + 16.dp)
                }
            }
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
            if (gvm.currentSnapshot !== snapshot) {
                gvm.currentSnapshot = snapshot
            }
            binding.tvHeaderTitle.text = R.string.format_task_snapshots.format(
                it + 1, gvm.allSnapshots.require().size
            )
            binding.tvHeaderDesc.text = R.string.format_task_snapshot_info_1.formatAsHtml(
                snapshot.startTimestamp.formatTime(),
                if (snapshot.isRunning) "-" else snapshot.endTimestamp.formatTime(),
                if (snapshot.isRunning) R.string.running.str
                else if (snapshot.isSuccessful) R.string.succeeded.str
                else R.string.failed.str
            )
            adapter.notifyItemRangeChanged(0, adapter.currentList.size)
        }
        doOnEventReceived<Applet>(AppletOption.EVENT_TOGGLE_RELATION) {
            if (!adapter.currentList.contains(it)) return@doOnEventReceived
            if (vm.isMultiSelected(it)) {
                vm.toggleMultiSelection(it)
            } else {
                if (it is Action<*> && Preferences.showToggleRelationTip) {
                    PreferenceHelpDialog().init(
                        R.string.tip,
                        R.string.tip_applet_relation
                    ) { noMore ->
                        Preferences.showToggleRelationTip = !noMore
                    }.show(childFragmentManager)
                }
                it.toggleRelation()
                vm.onAppletChanged.value = it
            }
        }
        if (vm.isSelectingReferent) return
        doOnEventReceived<Pair<String, Applet>>(AppletOption.EVENT_NAVIGATE_REFERENCE) {
            val applet = it.second
            if (!adapter.currentList.contains(applet)) return@doOnEventReceived

            if (vm.isMultiSelected(applet)) {
                vm.toggleMultiSelection(applet)
                return@doOnEventReceived
            }
            val referent = it.first
            val option = factory.requireOption(applet)
            val whichArg = applet.whichReference(referent)
            val arg = option.arguments[whichArg]
            FlowEditorDialog().init(vm.task, gvm.root, true, gvm)
                .setArgumentToSelect(applet, arg, referent)
                .doOnArgumentSelected { newReferent ->
                    gvm.referenceEditor.setReference(applet, arg, whichArg, newReferent)
                    vm.clearStaticErrorIfNeeded(applet, StaticError.PROMPT_RESET_REFERENCE)
                    gvm.referenceEditor.getReferenceChangedApplets().forEach { changed ->
                        vm.onAppletChanged.value = changed
                    }
                    gvm.referenceEditor.reset()
                }.show(childFragmentManager)
        }
        doOnEventReceived<Applet>(AppletOption.EVENT_EDIT_VALUE) {
            if (!adapter.currentList.contains(it)) return@doOnEventReceived
            if (vm.isMultiSelected(it)) {
                vm.toggleMultiSelection(it)
            } else {
                menuHelper.triggerMenuItem(null, it, R.id.item_edit)
            }
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

    fun setArgumentToSelect(anchor: Applet, ref: ArgumentDescriptor, referent: String?) =
        doWhenCreated {
            vm.referentAnchor = anchor
            vm.referentDescriptor = ref
            vm.isReadyOnly = true
            if (referent != null) {
                gvm.selectReferentsWithName(anchor, referent)
            }
            vm.addCloseable {
                gvm.clearReferentSelections()
            }
        }

    fun doOnArgumentSelected(block: (String) -> Unit) = doWhenCreated {
        vm.doOnArgSelected = block
    }

    fun doOnResult(block: (Boolean, Flow) -> Unit) = doWhenCreated {
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
        if (error != null) {
            vm.staticError = StaticError(vm.flow[error.victim.index], error.code, error.arg)
        }
    }

    fun initBase(task: XTask, readonly: Boolean) = doWhenCreated {
        vm.global = viewModels<GlobalFlowEditorViewModel>().value
        if (task.flow == null) {
            task.flow = vm.generateDefaultFlow(task.metadata.taskType)
        }
        vm.task = task
        vm.isReadyOnly = readonly
        vm.initFlow(task.requireFlow())
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
        vm.isReadyOnly = readonly
        vm.initFlow(flow)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (!confirmed && !vm.isReadyOnly && !vm.isBase) {
            vm.onFlowCompleted(true, gvm.unmodifiedRoots.getValue(vm.flow))
        }
    }
}