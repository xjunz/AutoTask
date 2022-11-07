package top.xjunz.tasker.ui.task.selector

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.ktx.eq
import top.xjunz.tasker.ktx.format
import top.xjunz.tasker.ktx.require
import top.xjunz.tasker.ktx.toast
import top.xjunz.tasker.service.floatingInspector
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.task.applet.option.AppletOptionFactory
import top.xjunz.tasker.ui.base.SavedStateViewModel
import java.util.*

/**
 * @author xjunz 2022/10/22
 */
class AppletSelectorViewModel(states: SavedStateHandle) : SavedStateViewModel(states) {

    var animateItems = true

    var previousSelectedFactory = -1

    val appletOptionFactory = AppletOptionFactory()

    val selectedFlowRegistry = MutableLiveData<Int>()

    val showClearDialog = MutableLiveData<Boolean>()

    val options = mutableListOf<AppletOption>()

    val onAppletAdded = MutableLiveData<Pair<Int, Applet>>()

    lateinit var onCompletion: (List<Flow>) -> Unit

    val candidates = Flow()

    val applets = MutableLiveData<MutableList<Applet>>(mutableListOf())

    lateinit var title: CharSequence

    private val collapsedFlows = mutableSetOf<Flow>()

    fun getAppletsCount(): Int {
        return candidates.sumOf { (it as Flow).size }
    }

    fun isCollapsed(flow: Flow): Boolean {
        return collapsedFlows.contains(flow)
    }

    fun removeCandidate(flow: Flow) {
        candidates.remove(flow)
    }

    fun toggleCollapse(flow: Flow) {
        if (collapsedFlows.contains(flow)) {
            collapsedFlows.remove(flow)
        } else {
            collapsedFlows.add(flow)
        }
        notifyCandidatesChanged()
    }

    fun selectFlowRegistry(index: Int) {
        if (selectedFlowRegistry eq index) return
        options.clear()
        options.addAll(
            appletOptionFactory.findRegistryById(
                appletOptionFactory.flowRegistry.appletFlowOptions[index].appletId
            ).categorizedOptions
        )
        previousSelectedFactory = selectedFlowRegistry.value ?: 0
        selectedFlowRegistry.value = index
    }

    private fun appendOption(option: AppletOption) {
        appendApplet(option.yieldApplet())
    }

    fun appendApplet(applet: Applet) {
        val flowOption = appletOptionFactory.findFlowOption(applet.registryId)
        if (candidates.isEmpty() || candidates.last().appletId != flowOption.appletId) {
            candidates.add(flowOption.yieldApplet() as Flow)
        }
        val last = candidates.last() as Flow
        if (last.isNotEmpty()) {
            val prev = last.last()
            if (prev.id == applet.id)
            // Auto OR relation if the previous applet is of the same id
                applet.isAnd = false
        }
        last.add(applet)
        notifyCandidatesChanged()
    }

    fun acceptAppletsFromInspector() {
        val options = floatingInspector.getSelectedOptions()
        val flowOption = appletOptionFactory.findFlowOption(options.first().registryId)
        val initialIndex = candidates.size
        candidates.add(flowOption.yieldApplet() as Flow)
        options.forEach {
            appendOption(it)
        }
        // Collapse by default
        for (i in initialIndex..candidates.lastIndex) {
            collapsedFlows.add(candidates[i].casted())
        }
        toast(R.string.options_from_inspector_added.format(options.size))
        notifyCandidatesChanged()
    }

    fun clearAllCandidates() {
        candidates.clear()
        notifyCandidatesChanged()
    }

    fun swapFlows(from: Flow, to: Flow) {
        Collections.swap(candidates, candidates.indexOf(from), candidates.indexOf(to))
    }

    fun complete() {
        if (candidates.isEmpty()) {
            toast(R.string.no_rule_added)
        } else {
            onCompletion.invoke(candidates.map { it as Flow })
        }
    }

    fun notifyCandidatesChanged() {
        val ret = applets.require()
        ret.clear()
        candidates.forEachIndexed { index, flow ->
            flow as Flow
            flow.index = index
            flow.parent = candidates
            ret.add(flow)
            if (!isCollapsed(flow))
                flow.forEachIndexed { i, applet ->
                    applet.index = i
                    applet.parent = flow
                    ret.add(applet)
                }
        }
        applets.value = ret
    }
}