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
import top.xjunz.tasker.task.applet.controlFlow
import top.xjunz.tasker.task.applet.flow.PhantomFlow
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.task.applet.option.AppletOptionFactory
import top.xjunz.tasker.task.applet.scopeFlow
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

    val onAppletAdded = MutableLiveData<Int>()

    lateinit var registryOptions: Array<AppletOption>

    var isScoped = false

    var isInCriterionScope = false

    lateinit var onCompletion: (List<Applet>) -> Unit

    var title: CharSequence? = null

    fun setScope(flow: Flow) {
        val scope = flow.scopeFlow
        // Find its control flow, we need its control flow's option title to be shown
        val control = if (scope is ControlFlow) scope else scope.controlFlow
        checkNotNull(control) {
            "ControlFlow not found!"
        }
        isInCriterionScope = control is If
        title = appletOptionFactory.requireOption(control).rawTitle
        if (scope is ScopedFlow<*>) {
            isScoped = true
            registryOptions = arrayOf(appletOptionFactory.requireRegistryOption(scope.appletId))
            // If scoped, do not show extra options from other registry, like showing component
            // options while showing ui object options.
            if (isFloatingInspectorShown) floatingInspector.viewModel.showExtraOptions = false
        } else {
            isScoped = false
            if (control is If) {
                registryOptions = appletOptionFactory.flowRegistry.criterionFlowOptions
            } else if (control is Do) {
                registryOptions = appletOptionFactory.flowRegistry.actionFlowOptions
            }
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
        appendApplet(option.yield())
    }

    fun appendApplet(applet: Applet) {
        val flowOption = appletOptionFactory.requireRegistryOption(applet.registryId)
        val last = flow.lastOrNull()
        if (last !is Flow || (flowOption.appletId != last.appletId)) {
            val newFlow = flowOption.yield() as Flow
            if (newFlow !is PhantomFlow) {
                flow.add(newFlow)
                newFlow.add(applet)
            } else {
                flow.add(applet)
            }
        } else {
            last.add(applet)
            // Divider changed
            onAppletChanged.value = last.getOrNull(last.lastIndex - 1)
        }
        notifyFlowChanged()
    }

    fun acceptAppletsFromInspector() {
        val options = floatingInspector.getSelectedOptions()
        val flowOption = appletOptionFactory.requireRegistryOption(options.first().registryId)

        flow.add(flowOption.yield() as Flow)
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
        flow.forEachIndexed { index, child ->
            child.index = index
            child.parent = this.flow
            ret.add(child)
            if (child is Flow && !isCollapsed(child))
                child.forEachIndexed { i, applet ->
                    applet.index = i
                    applet.parent = child
                    ret.add(applet)
                }
        }
        ret.trimToSize()
        return ret
    }
}