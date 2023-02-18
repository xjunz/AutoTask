/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.flow

import android.annotation.SuppressLint
import top.xjunz.shared.utils.unsupportedOperation
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * A flow which is intended to be merged into its parent flow. This flow only exists in edition time.
 *
 * @author xjunz 2022/11/10
 */
class PhantomFlow : Flow() {

    override fun onPrepareApply(runtime: TaskRuntime) {
        unsupportedOperation("PhantomFlow is not expected to be present in runtime!")
    }

    @SuppressLint("MissingSuperCall")
    override fun staticCheckMyself(): Int {
        unsupportedOperation("PhantomFlow should be merged!")
    }

}