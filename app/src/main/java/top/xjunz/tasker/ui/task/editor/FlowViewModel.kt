package top.xjunz.tasker.ui.task.editor

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.ktx.require
import top.xjunz.tasker.task.applet.depthInAncestor
import top.xjunz.tasker.ui.base.SavedStateViewModel

/**
 * @author xjunz 2022/11/10
 */
abstract class FlowViewModel(states: SavedStateHandle) : SavedStateViewModel(states) {

    lateinit var flow: Flow

    var isReadyOnly: Boolean = false

    val applets = MutableLiveData(emptyList<Applet>())

    val onAppletChanged = MutableLiveData<Applet>()

    val onAppletLongClicked = MutableLiveData<Applet>()

    protected val collapsedFlows = mutableSetOf<Flow>()

    abstract fun flatmapFlow(): List<Applet>

    fun notifyFlowChanged() {
        applets.value = flatmapFlow()
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

    fun regenerateApplets() {
        val ref = applets.require() as MutableList<Applet>
        ref.clear()
        ref.addAll(flatmapFlow())
    }

    fun updateChildrenIndexesIfNeeded(flow: Flow): Boolean {
        if (flow.depthInAncestor(this.flow) <= 2) {
            flow.forEachIndexed { index, applet ->
                applet.index = index
                onAppletChanged.value = applet
            }
            return true
        }
        return false
    }
}