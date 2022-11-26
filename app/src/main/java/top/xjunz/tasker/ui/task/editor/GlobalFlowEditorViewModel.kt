package top.xjunz.tasker.ui.task.editor

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.applet.base.When
import top.xjunz.tasker.task.applet.findChildrenReferringRefid
import top.xjunz.tasker.task.applet.option.AppletOptionFactory

/**
 * A global view model serving all [FlowEditorDialog]s, this view model is expected to
 * be hosted in Activity lifecycle.
 *
 * @author xjunz 2022/11/26
 */
class GlobalFlowEditorViewModel : ViewModel() {

    private var _root: Flow? = null

    val root: Flow get() = _root!!

    val factory = AppletOptionFactory()

    val onReferenceSelected = MutableLiveData<Boolean>()

    val onAppletChanged = MutableLiveData<Applet>()

    fun renameRefid(prev: String, cur: String) {
        root.findChildrenReferringRefid(prev).forEach {
            it.referring.forEach { (which, refid) ->
                if (refid == prev) (it.referring as MutableMap)[which] = cur
            }
        }
    }

    fun notifyRefidChanged(changed: String) {
        root.findChildrenReferringRefid(changed).forEach {
            onAppletChanged.value = it
        }
    }

    fun generateDefaultFlow(): Flow {
        val root = factory.flowRegistry.containerFlow.yieldApplet() as Flow
        val whenFlow = factory.flowRegistry.whenFlow.yieldApplet() as When
        whenFlow.add(factory.eventRegistry.contentChanged.yieldApplet())
        root.add(whenFlow)
        root.add(factory.flowRegistry.ifFlow.yieldApplet())
        root.add(factory.flowRegistry.doFlow.yieldApplet())
        return root
    }

    fun setRootFlowIfAbsent(flow: Flow): Boolean {
        if (_root == null) {
            _root = flow
            return true
        }
        return false
    }

    fun clearRootFlow() {
        _root = null
    }
}