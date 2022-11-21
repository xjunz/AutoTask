package top.xjunz.tasker.task.applet.flow

import top.xjunz.tasker.engine.AutomatorTask
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.runtime.FlowRuntime

/**
 * @author xjunz 2022/09/03
 */
class PackageFlow : Flow() {

    override fun onPrepare(task: AutomatorTask, runtime: FlowRuntime) {
        val info = task.getOrPutCrossTaskVariable(id) {
            val comp = runtime.hitEvent.componentInfo
            PackageInfoContext(comp.pkgName, comp.actName, comp.paneTitle)
        }
        runtime.setTarget(info)
    }

}