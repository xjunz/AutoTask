package top.xjunz.tasker.ui.task.selector

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.base.*
import top.xjunz.tasker.ktx.eq
import top.xjunz.tasker.ktx.format
import top.xjunz.tasker.ktx.toast
import top.xjunz.tasker.service.floatingInspector
import top.xjunz.tasker.service.isFloatingInspectorShown
import top.xjunz.tasker.task.applet.controlFlowParent
import top.xjunz.tasker.task.applet.flow.PhantomFlow
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.task.applet.option.AppletOptionFactory
import top.xjunz.tasker.ui.task.editor.FlowViewModel

/**
 * @author xjunz 2022/10/22
 */
class AppletSelectorViewModel(states: SavedStateHandle) : FlowViewModel(states) {

    val appletOptionFactory = AppletOptionFactory()

    var animateItems = true

    val selectedFlowRegistry = MutableLiveData<Int>()

    val showClearDialog = MutableLiveData<Boolean>()

    val options = mutableListOf<AppletOption>()

    val onAppletAdded = MutableLiveData<Pair<Int, Applet>>()

    lateinit var registryOptions: Array<AppletOption>

    var isScoped = false

    var isInCriterionScope = false

    lateinit var onCompletion: (List<Applet>) -> Unit

    var title: CharSequence? = null

    fun setScope(flow: Flow) {
        // Find its control flow, we need its control flow's option title to be shown
        val controlFlow = if (flow is ControlFlow) flow else flow.controlFlowParent
        checkNotNull(controlFlow) {
            "ControlFlow not found!"
        }
        isInCriterionScope = controlFlow is If
        title = appletOptionFactory.requireOption(controlFlow).rawTitle
        if (flow is ControlFlow) {
            isScoped = false
            if (flow is If) {
                registryOptions = appletOptionFactory.flowRegistry.criterionFlowOptions
            } else if (flow is Do) {
                registryOptions = appletOptionFactory.flowRegistry.actionFlowOptions
            }
        } else {
            isScoped = true
            registryOptions = arrayOf(appletOptionFactory.requireRegistryOption(flow.appletId))
            // If scoped, do not show extra options from other registry, like showing component
            // options while showing ui object options.
            if (isFloatingInspectorShown) floatingInspector.viewModel.showExtraOptions = false
        }
    }

    fun selectFlowRegistry(index: Int) {
        if (selectedFlowRegistry eq index) return
        options.clear()
        options.addAll(
            appletOptionFactory.requireRegistryById(registryOptions[index].appletId).categorizedOptions
        )
        selectedFlowRegistry.value = index
    }

    private fun appendOption(option: AppletOption) {
        appendApplet(option.yieldApplet())
    }

    fun appendApplet(applet: Applet) {
        val flowOption = appletOptionFactory.requireRegistryOption(applet.registryId)
        if (flow.isEmpty() || flow.last().appletId != flowOption.appletId) {
            flow.add(flowOption.yieldApplet() as Flow)
        }
        val last = flow.last() as Flow
        if (last.isNotEmpty()) {
            val prev = last.last()
            if (prev.id == applet.id)
            // Auto OR relation if the previous applet is of the same id
                applet.isAnd = false
        }
        last.add(applet)
        notifyFlowChanged()
    }

    fun acceptAppletsFromInspector() {
        val options = floatingInspector.getSelectedOptions()
        val flowOption = appletOptionFactory.requireRegistryOption(options.first().registryId)

        flow.add(flowOption.yieldApplet() as Flow)
        options.forEach {
            appendOption(it)
        }

        toast(R.string.options_from_inspector_added.format(options.size))
        notifyFlowChanged()
    }

    fun clearAllCandidates() {
        flow.clear()
        notifyFlowChanged()
    }

    fun complete() {
        if (flow.isEmpty()) {
            toast(R.string.no_rule_added)
        } else {
            if (isScoped) {
                onCompletion.invoke(flow.single() as Flow)
            } else {
                onCompletion.invoke(mergeCandidates())
            }
        }
    }

    private fun mergeCandidates(): List<Applet> {
        val ret = mutableListOf<Applet>()
        flow.forEach {
            if (it is PhantomFlow) ret.addAll(it) else ret.add(it)
        }
        return ret
    }

    override fun flatmapFlow(): List<Applet> {
        val ret = ArrayList<Applet>()
        flow.forEachIndexed { index, flow ->
            flow as Flow
            flow.index = index
            flow.parent = this.flow
            ret.add(flow)
            if (!isCollapsed(flow))
                flow.forEachIndexed { i, applet ->
                    applet.index = i
                    applet.parent = flow
                    ret.add(applet)
                }
        }
        ret.trimToSize()
        return ret
    }
}