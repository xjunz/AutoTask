package top.xjunz.tasker.engine.applet.base

import androidx.annotation.CallSuper
import top.xjunz.tasker.engine.AutomatorTask
import top.xjunz.tasker.engine.applet.serialization.AppletValues
import top.xjunz.tasker.engine.runtime.FlowRuntime
import java.util.*

/**
 * A flow is a set of [applets][Applet] but is also a special applet.
 *
 * @author xjunz 2022/08/04
 */
open class Flow(private val elements: MutableList<Applet> = ArrayList()) : Applet(),
    MutableList<Applet> by elements {

    override val valueType: Int = AppletValues.VAL_TYPE_IRRELEVANT

    protected open fun shouldSkipAll(task: AutomatorTask, runtime: FlowRuntime): Boolean = false

    @CallSuper
    protected open fun doApply(task: AutomatorTask, runtime: FlowRuntime) {
        forEachIndexed _continue@{ index, applet ->
            task.ensureActive()
            // Always execute the first applet in a flow and skip an applet if its relation to
            // previous peer applet does not meet the previous execution result.
            if (index != 0 && applet.isAnd != runtime.isSuccessful) {
                runtime.observer?.onSkipped(applet, runtime)
                return@_continue
            }
            applet.parent = this
            applet.index = index
            runtime.currentApplet = applet
            runtime.tracker.moveTo(index)
            runtime.observer?.onStarted(applet, runtime)
            applet.apply(task, runtime)
            runtime.observer?.onTerminated(applet, runtime)
        }
    }

    fun performStaticCheck() {
        staticCheckMySelf()
        forEachIndexed { index, applet ->
            applet.parent = this
            applet.index = index
            if (applet is Flow)
                applet.performStaticCheck()
        }
    }

    protected open fun staticCheckMySelf() {
        /* no-op */
    }

    override fun apply(task: AutomatorTask, runtime: FlowRuntime) {
        onPreApply(task, runtime)
        runtime.currentFlow = this
        runtime.tracker.jumpIn()
        if (shouldSkipAll(task, runtime)) {
            runtime.observer?.onSkipped(this, runtime)
        } else {
            onPrepare(task, runtime)
            // Backup the target, because sub-flows may change the target, we don't want the changed
            // value to fall through.
            val backup = runtime.getRawTarget()
            doApply(task, runtime)
            onPostApply(task, runtime)
            // restore the target
            runtime.setTarget(backup)
        }
        runtime.tracker.jumpOut()
    }

    val flatSize: Int
        get() {
            var size = 0
            forEach {
                if (it is Flow) {
                    size += it.flatSize
                } else {
                    size++
                }
            }
            return size
        }

    fun swap(from: Applet, to: Applet) {
        Collections.swap(this, indexOf(from), indexOf(to))
    }

    protected open fun onPrepare(task: AutomatorTask, runtime: FlowRuntime) {
        /* no-op */
    }

    /**
     * Do something before the flow is started. At this time, [FlowRuntime.currentFlow] is
     * not yet assigned to this flow.
     */
    protected open fun onPreApply(task: AutomatorTask, runtime: FlowRuntime) {}

    /**
     * Do something after all [elements] are completed.
     */
    protected open fun onPostApply(task: AutomatorTask, runtime: FlowRuntime) {
        // do nothing by default
    }
}