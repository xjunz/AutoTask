package top.xjunz.tasker.ui.task.editor

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.ControlFlow
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.applet.base.When
import top.xjunz.tasker.ktx.notifySelfChanged
import top.xjunz.tasker.ktx.require
import top.xjunz.tasker.ktx.toast
import top.xjunz.tasker.task.applet.clone
import top.xjunz.tasker.task.applet.depth
import top.xjunz.tasker.task.applet.isContainer
import top.xjunz.tasker.task.applet.option.ValueDescriptor

/**
 * @author xjunz 2022/09/10
 */
class FlowEditorViewModel(states: SavedStateHandle) : FlowViewModel(states) {

    val selectionLiveData = MutableLiveData(Flow())

    inline val selections: Flow get() = selectionLiveData.require()

    inline val isInMultiSelectionMode get() = selections.size > 0

    inline val isEditingContainerFlow get() = flow.parent != null && flow.isContainer

    val isSelectingReference get() = ::referenceToSelect.isInitialized

    val showSplitConfirmation = MutableLiveData<Boolean>()

    val showMergeConfirmation = MutableLiveData<Boolean>()

    var isNewTask: Boolean = true

    var selectedApplet = MutableLiveData<Applet>()

    val changedApplet = MutableLiveData<Applet>()

    val onRefSelected = MutableLiveData<Boolean>()

    lateinit var doOnCompletion: (Flow) -> Unit

    lateinit var doOnRefSelected: (Applet, Int, String) -> Unit

    lateinit var doSplit: () -> Unit

    lateinit var referenceToSelect: ValueDescriptor

    fun generateDefaultFlow() {
        val root = Flow()
        val whenFlow = appletOptionFactory.flowRegistry.whenFlow.yieldApplet() as When
        whenFlow.add(appletOptionFactory.eventRegistry.contentChanged.yieldApplet())
        root.add(whenFlow)
        root.add(appletOptionFactory.flowRegistry.ifFlow.yieldApplet())
        root.add(appletOptionFactory.flowRegistry.doFlow.yieldApplet())
        flow = root
        notifyFlowChanged()
    }

    private fun multiSelect(applet: Applet) {
        if (selections.isNotEmpty() && selections.first().parent != applet.parent) {
            toast(R.string.prompt_applet_multi_selection_depth)
        } else {
            selectionLiveData.require().add(applet)
            changedApplet.value = applet
            selectionLiveData.notifySelfChanged()
        }
    }

    fun isSelected(applet: Applet): Boolean {
        return selectedApplet.value == applet || isMultiSelected(applet)
    }

    fun toggleMultiSelection(applet: Applet) {
        if (isMultiSelected(applet)) {
            multiUnselect(applet)
        } else {
            multiSelect(applet)
        }
    }

    private fun multiUnselect(applet: Applet) {
        selections.remove(applet)
        changedApplet.value = applet
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
            changedApplet.value = removed
        }
        selectionLiveData.notifySelfChanged()
    }

    private fun flatmapFlow(flow: Flow, depth: Int = 0): List<Applet> {
        val ret = mutableListOf<Applet>()
        flow.forEachIndexed { index, applet ->
            applet.index = index
            applet.parent = flow
            if (applet is Flow && collapsedFlows.contains(applet)) {
                ret.add(applet)
                return@forEachIndexed
            }
            if (applet is ControlFlow) {
                ret.add(applet)
                if (depth < 1)
                    ret.addAll(flatmapFlow(applet, depth + 1))
            } else if (applet is Flow && !applet.isContainer) {
                ret.add(applet)
                applet.forEachIndexed { i, a ->
                    a.index = i
                    a.parent = applet
                    ret.add(a)
                }
            } else {
                ret.add(applet)
            }
        }
        return ret
    }

    override fun flatmapFlow(): List<Applet> {
        return flatmapFlow(flow)
    }

    fun mergeSelectedApplets() {
        if (selections.size == 1) {
            toast(R.string.error_merge_single_applet)
            return
        }
        val first = selections.first()
        if (first.depth == Applet.MAX_FLOW_NESTED_DEPTH) {
            toast(R.string.too_deeply_nested)
            return
        }
        val parent = first.requireParent()
        val insertPosition = selections.minOf { it.index }
        val container = appletOptionFactory.flowRegistry.containerFlow.yieldApplet() as Flow
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
        parent.forEachIndexed { index, applet ->
            applet.index = index
            changedApplet.value = applet
        }
        notifyFlowChanged()
    }

    fun initWith(initialFlow: Flow, readonly: Boolean) {
        isReadyOnly = readonly
        flow = if (readonly) initialFlow else initialFlow.clone(appletOptionFactory)
        flow.parent = initialFlow.parent
        flow.index = initialFlow.index
        isNewTask = false
        notifyFlowChanged()
    }

    fun complete() {
        doOnCompletion.invoke(flow)
    }

    fun notifySplit() {
        doSplit.invoke()
    }

    /**
     * Split a container flow into its parent. This is the reverse operation against [mergeSelectedApplets].
     */
    fun splitContainerFlow(container: Flow) {
        check(container.isContainer)
        val parent = container.requireParent()
        parent.remove(container)
        parent.addAll(container.index, container)
        // Notify indexes changed
        for (i in container.index + container.size..parent.lastIndex) {
            parent[i].index = i
            changedApplet.value = parent[i]
        }
        notifyFlowChanged()
    }

    private fun Applet.hasResultWithDescriptor(): Boolean {
        val option = appletOptionFactory.findOption(this)
        if (option != null && option.results.any { it.type == referenceToSelect.type }) {
            return true
        }
        if (this is Flow) {
            return any {
                it.hasResultWithDescriptor()
            }
        }
        return false
    }

    fun hasCandidateReference(flow: Flow): Boolean {
        return flow.any {
            it.hasResultWithDescriptor()
        }
    }
}