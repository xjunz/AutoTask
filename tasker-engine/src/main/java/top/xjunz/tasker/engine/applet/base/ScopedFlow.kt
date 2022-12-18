package top.xjunz.tasker.engine.applet.base

import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * Scoped flow initializes its target at [Flow.onPrepare]. The target will be used by all of its
 * elements.
 *
 * @author xjunz 2022/12/04
 */
abstract class ScopedFlow<T : Any> : Flow() {

    /**
     * Generate the overall target. The result will be registered to the snapshot in runtime
     * and can be accessed across tasks with a key. This is done in [onPrepare].
     */
    abstract fun initializeTarget(runtime: TaskRuntime): T

    /**
     * Get the initialized target, which is generated by [initializeTarget].
     */
    protected inline val TaskRuntime.target: T get() = getTarget()

    /**
     * By default, the target is stored with [id] as the key. If you want to store more
     * variables, you can call this to generate a unique key without conflict with other
     * potential keys.
     */
    fun generateUniqueKey(seed: Int): Int {
        check(seed in 1..0xFF) {
            "Seed out of range[1 to 0xFF]!"
        }
        return seed shl 24 or id
    }

    override fun onPrepare(runtime: TaskRuntime) {
        super.onPrepare(runtime)
        runtime.setTarget(
            runtime.getEnvironmentVariable(id) {
                initializeTarget(runtime)
            }
        )
    }
}