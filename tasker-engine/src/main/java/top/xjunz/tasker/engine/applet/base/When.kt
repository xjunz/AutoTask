package top.xjunz.tasker.engine.applet.base

import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2022/08/11
 */
class When : ControlFlow() {

    override val maxSize: Int = 1

    override val minSize: Int = 1

    override val requiredIndex: Int = 0

    override fun onPostApply(runtime: TaskRuntime) {
        super.onPostApply(runtime)
        if (!runtime.isSuccessful) {
            stopship(runtime)
        }
    }

    override fun staticCheckMySelf() {
        super.staticCheckMySelf()
        check(index == 0)
        check(isEnabled)
    }
}