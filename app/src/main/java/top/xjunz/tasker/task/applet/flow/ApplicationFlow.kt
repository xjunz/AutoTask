/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.flow

import top.xjunz.tasker.engine.applet.base.ScopedFlow
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.service.currentService

/**
 * @author xjunz 2022/09/03
 */
class ApplicationFlow : ScopedFlow<ComponentInfoWrapper>() {

    /**
     * Every flow has a distinct key, because component info might have changed.
     */
    override fun generateTargetKey(): Long {
        return generateUniqueKey(hashCode())
    }

    override fun initializeTarget(runtime: TaskRuntime): ComponentInfoWrapper {
        return currentService.a11yEventDispatcher.getCurrentComponentInfo()
    }
}