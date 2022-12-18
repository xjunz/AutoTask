package top.xjunz.tasker.engine.applet.base

import androidx.annotation.CallSuper
import top.xjunz.tasker.engine.applet.dto.AppletValues
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * A flow is a set of [applets][Applet] but is also a special applet.
 *
 * @author xjunz 2022/08/04
 */
open class Flow(private val elements: MutableList<Applet> = ArrayList()) : Applet(),
    MutableList<Applet> by elements {

    open val minSize = 1

    open val maxSize = MAX_FLOW_CHILD_COUNT

    inline val requiredSize get() = if (minSize == maxSize) minSize else -1

    override val valueType: Int = AppletValues.VAL_TYPE_IRRELEVANT

    protected open fun shouldSkipAll(runtime: TaskRuntime): Boolean = false

    @CallSuper
    protected open suspend fun doApply(runtime: TaskRuntime) {
        forEachIndexed `continue`@{ index, applet ->
            if (!applet.isEnabled) {
                return@`continue`
            }
            runtime.ensureActive()
            // Always execute the first applet in a flow and skip an applet if its relation to
            // previous peer applet does not meet the previous execution result.
            if (index != 0 && applet.isAnd != runtime.isSuccessful) {
                runtime.observer?.onSkipped(applet, runtime)
                return@`continue`
            }
            applet.parent = this
            applet.index = index
            runtime.currentApplet = applet
            runtime.tracker.moveTo(index)
            runtime.observer?.onStarted(applet, runtime)
            applet.apply(runtime)
            runtime.observer?.onTerminated(applet, runtime)
        }
    }

    fun performStaticCheck() {
        staticCheckMySelf()
        forEachIndexed { index, applet ->
            applet.parent = this
            applet.index = index
            if (applet is Flow) applet.performStaticCheck()
        }
    }

    protected open fun staticCheckMySelf() {
        /* no-op */
    }

    override suspend fun apply(runtime: TaskRuntime) {
        onPreApply(runtime)
        runtime.currentFlow = this
        runtime.tracker.jumpIn()
        if (shouldSkipAll(runtime)) {
            runtime.observer?.onSkipped(this, runtime)
        } else {
            onPrepare(runtime)
            // Backup the target, because sub-flows may change the target, we don't want the changed
            // value to fall through.
            val backup = runtime.getRawTarget()
            doApply(runtime)
            onPostApply(runtime)
            // restore the target
            runtime.setTarget(backup)
        }
        runtime.tracker.jumpOut()
    }

    /**
     * Just before the flow execute its elements.
     */
    protected open fun onPrepare(runtime: TaskRuntime) {
        /* no-op */
    }

    /**
     * Do something before the flow is started. At this time, [TaskRuntime.currentFlow] is
     * not yet assigned to this flow.
     */
    protected open fun onPreApply(runtime: TaskRuntime) {}

    /**
     * Do something after all [elements] are completed.
     */
    protected open fun onPostApply(runtime: TaskRuntime) {
        /* no-op */
    }
}