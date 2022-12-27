/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.flow

import top.xjunz.tasker.engine.applet.base.ScopedFlow
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2022/09/03
 */
class PackageFlow : ScopedFlow<PackageInfoContext>() {

    override fun initializeTarget(runtime: TaskRuntime): PackageInfoContext {
        val comp = runtime.hitEvent.componentInfo
        return PackageInfoContext(comp.pkgName, comp.actName, comp.paneTitle)
    }

}