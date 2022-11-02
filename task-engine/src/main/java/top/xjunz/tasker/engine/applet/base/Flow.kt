package top.xjunz.tasker.engine.applet.base

import androidx.annotation.CallSuper
import top.xjunz.shared.utils.runtimeException
import top.xjunz.tasker.engine.AutomatorTask
import top.xjunz.tasker.engine.applet.serialization.AppletValues
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.engine.runtime.FlowRuntime

/**
 * A flow is a set of [applets][Applet] but is also a special applet.
 *
 * @author xjunz 2022/08/04
 */
open class Flow : Applet() {

    companion object {
        fun defaultFlow(): Flow {
            return DslFlow {
                When(Event.EVENT_ON_PACKAGE_ENTERED)
                If {}
            }
        }
    }

    var isReferred: Boolean = false

    var remark: String? = null

    val count: Int get() = elements.size

    open val requiredElementCount: Int = -1

    val elements: MutableList<Applet> = ArrayList(1)

    override var valueType: Int = AppletValues.VAL_TYPE_IRRELEVANT

    @CallSuper
    protected open fun doApply(task: AutomatorTask, runtime: FlowRuntime) {
        for ((index, applet) in elements.withIndex()) {
            task.ensureActive()
            // Always execute the first applet in a flow and skip an applet if its relation to
            // previous peer applet does not meet the previous execution result.
            if (index != 0 && applet.isAnd != runtime.isSuccessful) {
                runtime.observer?.onSkipped(applet, runtime)
                continue
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

    override fun apply(task: AutomatorTask, runtime: FlowRuntime) {
        onPreApply(task, runtime)
        runtime.currentFlow = this
        runtime.tracker.jumpIn()
        checkElements()
        // Backup the target, because sub-flows may change the target, we don't want the changed
        // value to fall through.
        val backup = runtime.getRawTarget()
        onPrepare(task, runtime)
        doApply(task, runtime)
        // restore the target
        runtime.setTarget(backup)
        runtime.tracker.jumpOut()
        onPostApply(task, runtime)
    }

    /**
     * Throw an exception to halt the whole flow. This is regarded as a normal termination.
     */
    protected fun stopship(runtime: FlowRuntime): Nothing {
        throw AutomatorTask.FlowFailureException(runtime)
    }

    protected open fun onPrepare(task: AutomatorTask, runtime: FlowRuntime) {
    }

    /**
     * Do something before the flow is started. At this time, [FlowRuntime.currentFlow] is
     * not yet assigned to this flow.
     */
    protected open fun onPreApply(task: AutomatorTask, runtime: FlowRuntime) {}

    open fun checkElements() {
        if (requiredElementCount != -1 && requiredElementCount != count) {
            runtimeException(
                "This flow is expected to contain exactly $requiredElementCount applets" +
                        " but currently it is ${count}!"
            )
        }
    }

    /**
     * Do something after all [elements] are completed.
     */
    protected open fun onPostApply(task: AutomatorTask, runtime: FlowRuntime) {
        // do nothing by default
    }
}