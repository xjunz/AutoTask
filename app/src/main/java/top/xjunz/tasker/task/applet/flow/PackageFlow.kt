/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.flow

import android.content.ComponentName
import top.xjunz.shared.utils.illegalArgument
import top.xjunz.tasker.engine.applet.base.ScopedFlow
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.service.currentService

/**
 * @author xjunz 2022/09/03
 */
class PackageFlow : ScopedFlow<ComponentInfoContext>() {

    /**
     * Every flow has a distinct key, because component info might have changed.
     */
    override fun generateTargetKey(): Long {
        return generateUniqueKey(hashCode())
    }

    override fun initializeTarget(runtime: TaskRuntime): ComponentInfoContext {
        return ComponentInfoContext.from(currentService.residentTaskScheduler.getCurrentComponentInfo())
    }

    override fun deriveResultByRefid(which: Int, ret: Any): Any? {
        ret as ComponentInfoContext
        return when (which) {
            0 -> ret.packageName
            1 -> if (ret.activityName == null) null
            else ComponentName(ret.packageName, ret.activityName).flattenToString()
            else -> illegalArgument("result index")
        }
    }
}