/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.flow

import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.uiautomator.CoroutineUiObject

/**
 * @author xjunz 2023/09/13
 */
class ClickUiObjectIfExists : PerformActionIfUiObjectExistsInCurrentWindow() {

    override val isCriterionSpecified: Boolean = false

    override suspend fun performActionIfFound(runtime: TaskRuntime, target: CoroutineUiObject) {
        target.click()
    }

}