package top.xjunz.tasker.ui.task.editor

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.ControlFlow
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.applet.base.When
import top.xjunz.tasker.ktx.eq
import top.xjunz.tasker.task.applet.option.AppletOptionFactory

/**
 * @author xjunz 2022/09/10
 */
class TaskEditorViewModel : ViewModel() {

    private lateinit var rootFlow: Flow

    private val collapsedFlows = mutableSetOf<Flow>()

    val appletOptionFactory = AppletOptionFactory()

    var isNewTask: Boolean = true

    val flatMappedApplets = MutableLiveData<List<Applet>>()

    val singleSelectionIndex = MutableLiveData(-1)

    var selectedApplet: Applet? = null

    val currentPage = MutableLiveData<Int>()

    fun generateDefaultFlow() {
        val root = Flow()
        val whenFlow = appletOptionFactory.flowRegistry.whenFlow.yieldApplet() as When
        whenFlow.add(appletOptionFactory.eventRegistry.contentChanged.yieldApplet())
        root.add(whenFlow)
        root.add(appletOptionFactory.flowRegistry.ifFlow.yieldApplet())
        rootFlow = root
        notifyFlowChanged()
    }

    fun isCollapsed(applet: Applet): Boolean {
        if (applet !is Flow) return false
        return collapsedFlows.contains(applet)
    }

    fun toggleCollapse(applet: Applet) {
        if (applet !is Flow) return
        if (isCollapsed(applet)) {
            collapsedFlows.remove(applet)
        } else {
            collapsedFlows.add(applet)
        }
        notifyFlowChanged()
    }

    fun setFlow(flow: Flow) {
        rootFlow = flow
        notifyFlowChanged()
    }

    fun notifyFlowChanged() {
        flatMappedApplets.value = flatmapFlow(rootFlow)
    }

    fun singleSelect(index: Int) {
        if (index != -1 && singleSelectionIndex eq index) {
            selectedApplet = null
            singleSelectionIndex.value = -1
        } else {
            selectedApplet = flatMappedApplets.value?.getOrNull(index)
            singleSelectionIndex.value = index
        }
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
}