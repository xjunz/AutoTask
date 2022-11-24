package top.xjunz.tasker.task.applet.flow

import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2022/09/03
 */
class PackageFlow : Flow() {

    override fun onPrepare(runtime: TaskRuntime) {
        val info = runtime.getOrPutCrossTaskVariable(id) {
            val comp = runtime.hitEvent.componentInfo
            PackageInfoContext(comp.pkgName, comp.actName, comp.paneTitle)
        }
        runtime.setTarget(info)
    }

}