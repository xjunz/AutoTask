/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.base

import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2022/11/03
 */
class Else : Do() {

    // Once the previous result is success, do not execute this flow
    override var relation: Int = REL_OR

    /**
     * If its previous peer is skipped, do not execute it self.
     */
    override fun shouldSkip(runtime: TaskRuntime): Boolean {
        return runtime.ifSuccessful != false
    }

    override fun onPostApply(runtime: TaskRuntime) {
        super.onPostApply(runtime)
        runtime.isSuccessful = runtime.ifSuccessful == true
    }
}