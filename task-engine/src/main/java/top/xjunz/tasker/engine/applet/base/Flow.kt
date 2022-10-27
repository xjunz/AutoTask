package top.xjunz.tasker.engine.applet.base

import androidx.annotation.CallSuper
import top.xjunz.tasker.engine.AutomatorTask
import top.xjunz.tasker.engine.applet.serialization.AppletValues
import top.xjunz.tasker.engine.runtime.FlowRuntime
import top.xjunz.tasker.engine.runtime.TaskContext
import top.xjunz.tasker.engine.value.Event
import top.xjunz.tasker.util.runtimeException

/**
 * A flow is a set of [applets][Applet] but is also a special applet.
 *
 * @author xjunz 2022/08/04
 */
open class Flow : Applet() {

    companion object {
        fun defaultFlow(): Flow {
            return RootFlow {
                When(Event.EVENT_ON_PACKAGE_ENTERED)
                If {
                    // Nothing here
                }
            }
        }
    }

    var isReferred: Boolean = false

    var remark: String? = null

    val count: Int get() = elements.size

    open val requiredElementCount: Int = -1

    val elements: MutableList<Applet> = ArrayList(1)

    override val valueType: Int = AppletValues.VAL_TYPE_IRRELEVANT

    @CallSuper
    protected open fun doApply(context: TaskContext, runtime: FlowRuntime) {
        for ((index, applet) in elements.withIndex()) {
            context.task.ensureActive()
            // Always execute the first applet in a flow and skip an applet if its relation to
            // previous peer applet does not meet the previous execution result.
            if (index != 0 && applet.isAnd != runtime.isSuccessful)
                continue
            applet.parent = this
            applet.index = index
            runtime.currentApplet = applet
            runtime.moveTo(index)
            applet.apply(context, runtime)
        }
    }

    override fun apply(context: TaskContext, runtime: FlowRuntime) {
        onPreApply(context, runtime)
        runtime.currentFlow = this
        runtime.jumpIn()
        checkElements()
        // Backup the target, because sub-flows may change the target, we don't want the changed
        // value to fall through.
        val backup = runtime.getRawTarget()
        onPrepare(context, runtime)
        doApply(context, runtime)
        // restore the target
        runtime.setTarget(backup)
        runtime.jumpOut()
        onPostApply(context, runtime)
    }

    /**
     * Throw an exception to halt the whole flow. This is regarded as a normal termination.
     */
    protected fun stopship(runtime: FlowRuntime): Nothing {
        throw AutomatorTask.FlowFailureException(runtime)
    }

    protected open fun onPrepare(ctx: TaskContext, runtime: FlowRuntime) {
    }

    /**
     * If the return value is `true`, all [elements] in this flow will simply be dropped and the task head
     * will move to the next flow.
     */
    protected open fun shouldDropFlow(ctx: TaskContext, runtime: FlowRuntime): Boolean {
        return false
    }

    /**
     * Do something before the flow is started. At this time, [FlowRuntime.currentFlow] is
     * not yet assigned to this flow.
     */
    protected open fun onPreApply(ctx: TaskContext, runtime: FlowRuntime) {}

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
    protected open fun onPostApply(ctx: TaskContext, runtime: FlowRuntime) {
        // do nothing by default
    }
}