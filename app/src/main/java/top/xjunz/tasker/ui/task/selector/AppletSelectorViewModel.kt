package top.xjunz.tasker.ui.task.selector

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.ktx.eq
import top.xjunz.tasker.ktx.format
import top.xjunz.tasker.ktx.toast
import top.xjunz.tasker.service.floatingInspector
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.task.applet.option.AppletOptionFactory
import top.xjunz.tasker.ui.base.SavedStateViewModel

/**
 * @author xjunz 2022/10/22
 */
class AppletSelectorViewModel(states: SavedStateHandle) : SavedStateViewModel(states) {

    var animateItems = true

    val appletOptionFactory = AppletOptionFactory()

    var scopedRegistryOption: AppletOption? = null

    val selectedFlowRegistry = MutableLiveData<Int>()

    val showClearDialog = MutableLiveData<Boolean>()

    val options = mutableListOf<AppletOption>()

    val onAppletAdded = MutableLiveData<Pair<Int, Applet>>()

    val candidates = Flow()

    val applets = MutableLiveData(emptyList<Applet>())

    lateinit var onCompletion: (List<Applet>) -> Unit

    lateinit var title: CharSequence

    private val collapsedFlows = mutableSetOf<Flow>()

    fun isCollapsed(flow: Flow): Boolean {
        return collapsedFlows.contains(flow)
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
            appletOptionFactory.findRegistryById(getRegistryOptions()[index].appletId).categorizedOptions
        )
        selectedFlowRegistry.value = index
    }

    private fun appendOption(option: AppletOption) {
        appendApplet(option.yieldApplet())
    }

    fun appendApplet(applet: Applet) {
        val flowOption = appletOptionFactory.findRegistryOption(applet.registryId)
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
        val flowOption = appletOptionFactory.findRegistryOption(options.first().registryId)

        candidates.add(flowOption.yieldApplet() as Flow)
        options.forEach {
            appendOption(it)
        }

        toast(R.string.options_from_inspector_added.format(options.size))
        notifyCandidatesChanged()
    }

    fun clearAllCandidates() {
        candidates.clear()
        notifyCandidatesChanged()
    }

    fun complete() {
        if (candidates.isEmpty()) {
            toast(R.string.no_rule_added)
        } else {
            if (scopedRegistryOption == null) {
                onCompletion.invoke(candidates)
            } else {
                onCompletion.invoke(candidates.single() as Flow)
            }
        }
    }

    fun notifyCandidatesChanged() {
        val ret = ArrayList<Applet>()
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

    fun getRegistryOptions(): Array<AppletOption> {
        return if (scopedRegistryOption == null) {
            appletOptionFactory.flowRegistry.appletFlowOptions
        } else {
            arrayOf(scopedRegistryOption!!)
        }
    }
}