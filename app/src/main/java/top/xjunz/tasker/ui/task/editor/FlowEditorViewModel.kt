package top.xjunz.tasker.ui.task.editor

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.ktx.eq
import top.xjunz.tasker.ktx.format
import top.xjunz.tasker.ktx.notifySelfChanged
import top.xjunz.tasker.ktx.toast
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.task.applet.option.AppletOptionFactory
import top.xjunz.tasker.task.inspector.FloatingInspector
import top.xjunz.tasker.ui.base.SavedStateViewModel

/**
 * @author xjunz 2022/10/22
 */
class FlowEditorViewModel(states: SavedStateHandle) : SavedStateViewModel(states) {

    var animateItems = true

    var previousSelectedFactory = -1

    val appletOptionFactory = AppletOptionFactory()

    val selectedFactory = MutableLiveData<Int>()

    val showClearDialog = MutableLiveData<Boolean>()

    val options = mutableListOf<AppletOption>()

    val onAppletAdded = MutableLiveData<Pair<Int, Applet>>()

    private val _candidates = mutableListOf<Flow>()

    val candidates = MutableLiveData<List<Flow>>(_candidates)

    lateinit var title: CharSequence

    private val collapsedFlows = mutableSetOf<Flow>()

    fun getAppletsCount(): Int {
        return _candidates.sumOf { it.count }
    }

    fun isCollapsed(flow: Flow): Boolean {
        return collapsedFlows.contains(flow)
    }

    fun removeCandidate(flow: Flow) {
        _candidates.remove(flow)
    }

    fun toggleCollapse(flow: Flow): Boolean {
        return if (collapsedFlows.contains(flow)) {
            collapsedFlows.remove(flow)
            false
        } else {
            collapsedFlows.add(flow)
            true
        }
    }

    fun singleSelectFactory(index: Int) {
        if (selectedFactory eq index) return
        options.clear()
        options.addAll(
            appletOptionFactory.findFactoryById(
                appletOptionFactory.flowFactory.options[index].appletId
            ).categorizedOptions
        )
        previousSelectedFactory = selectedFactory.value ?: 0
        selectedFactory.value = index
    }

    private fun appendOption(option: AppletOption) {
        appendApplet(option.yieldApplet())
    }

    fun appendApplet(applet: Applet) {
        val flowOption = appletOptionFactory.findFlowOption(applet.factoryId)
        if (_candidates.isEmpty() || _candidates.last().appletId != flowOption.appletId) {
            _candidates.add(flowOption.yieldApplet() as Flow)
        }
        val last = _candidates.last()
        if (last.elements.isNotEmpty()) {
            val prev = last.elements.last()
            if (prev.id == applet.id)
            // Auto OR relation if the previous applet is of the same id
                applet.isAnd = false
        }
        last.elements.add(applet)
    }

    fun acceptAppletsFromInspector() {
        val options = FloatingInspector.require().getSelectedOptions()
        val flowOption = appletOptionFactory.findFlowOption(options.first().factoryId)
        val initialIndex = _candidates.size
        _candidates.add(flowOption.yieldApplet() as Flow)
        options.forEach {
            appendOption(it)
        }
        // Collapse by default
        for (i in initialIndex.._candidates.lastIndex) {
            collapsedFlows.add(_candidates[i])
        }
        candidates.notifySelfChanged()
        toast(R.string.options_from_inspector_added.format(options.size))
    }

    fun clearAllCandidates() {
        _candidates.clear()
        candidates.notifySelfChanged()
    }

}