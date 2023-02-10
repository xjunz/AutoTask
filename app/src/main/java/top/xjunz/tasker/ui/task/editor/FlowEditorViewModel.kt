/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.editor

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.base.*
import top.xjunz.tasker.engine.dto.AppletDTO.Serializer.toDTO
import top.xjunz.tasker.engine.dto.ChecksumUtil
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.ktx.eq
import top.xjunz.tasker.ktx.notifySelfChanged
import top.xjunz.tasker.ktx.require
import top.xjunz.tasker.ktx.toast
import top.xjunz.tasker.task.applet.clone
import top.xjunz.tasker.task.applet.depth
import top.xjunz.tasker.task.applet.flow.PreloadFlow
import top.xjunz.tasker.task.applet.isContainer
import top.xjunz.tasker.task.applet.option.AppletOptionFactory
import top.xjunz.tasker.task.applet.option.descriptor.ArgumentDescriptor
import top.xjunz.tasker.task.storage.TaskStorage

/**
 * @author xjunz 2022/09/10
 */
class FlowEditorViewModel(states: SavedStateHandle) : FlowViewModel(states) {

    val metadata: XTask.Metadata by lazy {
        task.metadata.copy()
    }

    lateinit var global: GlobalFlowEditorViewModel

    lateinit var task: XTask

    var flowToNavigate: Long = -1

    val factory = AppletOptionFactory

    val selectionLiveData = MutableLiveData(mutableListOf<Applet>())

    inline val selections: MutableList<Applet> get() = selectionLiveData.require()

    inline val isInMultiSelectionMode get() = selections.size > 0

    inline val isInEditionMode get() = flow.parent != null && !isReadyOnly

    lateinit var referentAnchor: Applet

    lateinit var argumentDescriptor: ArgumentDescriptor

    val isSelectingArgument get() = ::referentAnchor.isInitialized

    val showSplitConfirmation = MutableLiveData<Boolean>()

    val showMergeConfirmation = MutableLiveData<Boolean>()

    val isBase: Boolean get() = flow is RootFlow

    val selectedApplet = MutableLiveData<Applet>()

    val isFabVisible = MutableLiveData<Boolean>()

    val showQuitConfirmation = MutableLiveData<Boolean>()

    val showTaskRepeatedPrompt = MutableLiveData<Boolean>()

    lateinit var onFlowCompleted: (Flow) -> Unit

    lateinit var onTaskCompleted: () -> Unit

    lateinit var doOnArgSelected: (String) -> Unit

    lateinit var doSplit: () -> Unit

    var staticError: StaticError? = null

    var isInTrackMode: Boolean = false

    private fun multiSelect(applet: Applet) {
        if (selections.isNotEmpty() && selections.first().parent != applet.parent) {
            toast(R.string.prompt_applet_multi_selection_depth)
        } else {
            selectionLiveData.require().add(applet)
            onAppletChanged.value = applet
            selectionLiveData.notifySelfChanged()
        }
    }

    fun isSelected(applet: Applet): Boolean {
        return selectedApplet eq applet || isMultiSelected(applet)
    }

    fun toggleMultiSelection(applet: Applet) {
        if (isMultiSelected(applet)) {
            multiUnselect(applet)
        } else {
            multiSelect(applet)
        }
    }

    fun multiUnselect(applet: Applet) {
        selections.remove(applet)
        onAppletChanged.value = applet
        selectionLiveData.notifySelfChanged()
    }

    fun isMultiSelected(applet: Applet): Boolean {
        return selections.contains(applet)
    }

    fun singleSelect(index: Int) {
        selectedApplet.value = applets.value?.getOrNull(index)
    }

    fun clearSelections() {
        val itr = selections.iterator()
        while (itr.hasNext()) {
            val removed = itr.next()
            itr.remove()
            onAppletChanged.value = removed
        }
        selectionLiveData.notifySelfChanged()
    }

    private fun flatmapFlow(flow: Flow, depth: Int = 0): List<Applet> {
        val ret = mutableListOf<Applet>()
        flow.forEachIndexed `continue`@{ index, applet ->

            if (applet is PreloadFlow && !isSelectingArgument)
                return@`continue`

            applet.index = index
            applet.parent = flow
            ret.add(applet)

            if (applet is Flow && collapsedFlows.contains(applet))
                return@`continue`

            if (applet is Flow && depth < 2) {
                ret.addAll(flatmapFlow(applet, depth + 1))
            }
        }
        return ret
    }

    override fun flatmapFlow(): List<Applet> {
        return flatmapFlow(flow)
    }

    fun mergeSelectedApplets() {
        check(selections.size > 1)
        val first = selections.first()
        if (first.depth == Applet.MAX_FLOW_NESTED_DEPTH) {
            toast(R.string.too_deeply_nested)
            return
        }
        val parent = first.requireParent()
        val insertPosition = selections.minOf { it.index }
        val container = factory.flowRegistry.containerFlow.yield() as Flow
        container.addAll(selections)
        container.parent = parent
        selections.forEach {
            parent.remove(it)
        }
        selections.clear()
        selectionLiveData.notifySelfChanged()
        if (parent.lastIndex >= insertPosition) {
            parent.add(insertPosition, container)
        } else {
            parent.add(container)
        }
        container.forEach {
            onAppletChanged.value = it
        }
        updateChildrenIndexesIfNeeded(parent)
        notifyFlowChanged()
    }

    /**
     * Split a container flow into its parent. This is the reverse operation against [mergeSelectedApplets].
     */
    fun splitContainerFlow(container: Flow) {
        check(container.isContainer)
        val parent = container.requireParent()
        parent.remove(container)
        parent.addAll(container.index, container)
        updateChildrenIndexesIfNeeded(parent)
        notifyFlowChanged()
    }

    fun initialize(
        initialFlow: Flow,
        readonly: Boolean,
    ) {
        isReadyOnly = readonly
        flow = if (readonly) initialFlow else initialFlow.clone(factory)
    }

    fun complete(): Boolean {
        if (isBase) {
            val checksum = calculateChecksum()
            if (checksum != task.checksum) {
                if (TaskStorage.getAllTasks().any { it.checksum == checksum }) {
                    showTaskRepeatedPrompt.value = true
                    return false
                }
                metadata.modificationTimestamp = System.currentTimeMillis()
                if (metadata.checksum == -1L) {
                    metadata.creationTimestamp = metadata.modificationTimestamp
                }
                metadata.checksum = checksum
                task.metadata = metadata
                task.flow = flow.casted()
                onTaskCompleted.invoke()
            }
        } else {
            onFlowCompleted.invoke(flow)
        }
        return true
    }

    private fun Applet.hasResultWithDescriptor(): Boolean {
        val option = factory.findOption(this)
        if (option != null && option.findResults(argumentDescriptor).isNotEmpty()) {
            return true
        }
        if (this is Flow) {
            return any {
                it.hasResultWithDescriptor()
            }
        }
        return false
    }

    fun hasCandidateReferents(flow: Flow): Boolean {
        return flow.any {
            it.hasResultWithDescriptor()
        }
    }

    fun notifyFlowAbilityChanged(flow: Flow, depth: Int = 2) {
        onAppletChanged.value = flow
        flow.forEach {
            if (it is Flow && depth > 0) {
                notifyFlowAbilityChanged(it, depth - 1)
            } else {
                onAppletChanged.value = it
            }
        }
    }

    fun clearStaticErrorIfNeeded(target: Applet, prompt: Int): Boolean {
        if (staticError?.victim === target && staticError?.prompt == prompt) {
            staticError = null
            return true
        }
        return false
    }

    fun addBefore(target: Applet, peers: List<Applet>) {
        val parent = target.requireParent()
        parent.addAll(target.index, peers)
        val removed = clearStaticErrorIfNeeded(target, StaticError.PROMPT_ADD_BEFORE)
        if (!updateChildrenIndexesIfNeeded(parent) && removed) {
            onAppletChanged.value = target
        }
        notifyFlowChanged()
    }

    fun addAfter(target: Applet, peers: List<Applet>) {
        val parent = target.requireParent()
        if (target.index == parent.lastIndex) {
            parent.addAll(peers)
        } else {
            parent.addAll(target.index + 1, peers)
        }
        clearStaticErrorIfNeeded(target, StaticError.PROMPT_ADD_AFTER)
        // Divider changed
        onAppletChanged.value = target
        notifyFlowChanged()
    }

    fun addInside(target: Flow, children: List<Applet>) {
        val last = target.lastOrNull()
        target.addAll(children)
        clearStaticErrorIfNeeded(target, StaticError.PROMPT_ADD_INSIDE)
        // Notify divider changed
        onAppletChanged.value = last
        // Notify action icon changed
        if (target.size == children.size) {
            onAppletChanged.value = target
        }
        notifyFlowChanged()
    }

    fun calculateChecksum(): Long {
        return ChecksumUtil.calculateChecksum(flow.toDTO(), metadata)
    }

    fun generateDefaultFlow(taskType: Int): RootFlow {
        val root = factory.flowRegistry.rootFlow.yield() as RootFlow
        root.add(factory.flowRegistry.preloadFlow.yield())
        if (taskType == XTask.TYPE_RESIDENT) {
            val whenFlow = factory.flowRegistry.whenFlow.yield() as When
            whenFlow.add(factory.eventRegistry.contentChanged.yield())
            root.add(whenFlow)
        }
        root.add(factory.flowRegistry.ifFlow.yield())
        root.add(factory.flowRegistry.doFlow.yield())
        return root
    }
}