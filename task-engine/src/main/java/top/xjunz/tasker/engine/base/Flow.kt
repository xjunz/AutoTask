package top.xjunz.tasker.engine.base

import androidx.annotation.CallSuper
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import top.xjunz.tasker.engine.AppletContext
import top.xjunz.tasker.engine.AutomatorTask
import top.xjunz.tasker.engine.Event
import top.xjunz.tasker.engine.FlowRuntime
import top.xjunz.tasker.util.runtimeException

/**
 * A flow is a set of [applets][Applet] but is also a special applet.
 *
 * @author xjunz 2022/08/04
 */
@Serializable
@SerialName("Flow")
open class Flow : Applet() {

    companion object {
        fun defaultFlow(): Flow {
            return RootFlow {
                When(Event.EVENT_ON_PACKAGE_ENTERED)
                If {
                    // nothing here
                }
            }
        }
    }

    @Transient
    var isReferred: Boolean = false

    var remark: String? = null

    @Transient
    open val requiredElementCount: Int = -1

    val applets: MutableList<Applet> = ArrayList(1)

    @CallSuper
    protected open fun doApply(context: AppletContext, runtime: FlowRuntime) {
        for ((index, applet) in applets.withIndex()) {
            context.task.ensureActive()
            // Always execute the first applet in a flow and skip an applet if its relation to
            // previous peer applet does not meet the previous execution result.
            if (index != 0 && applet.isAnd != runtime.isSuccessful)
                continue
            runtime.currentApplet = applet
            runtime.moveTo(index)
            applet.apply(context, runtime)
        }
    }

    override fun apply(context: AppletContext, runtime: FlowRuntime) {
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

    protected open fun onPrepare(ctx: AppletContext, runtime: FlowRuntime) {
    }

    /**
     * If the return value is `true`, all [applets] in this flow will simply be dropped and the task head
     * will move to the next flow.
     */
    protected open fun shouldDropFlow(ctx: AppletContext, runtime: FlowRuntime): Boolean {
        return false
    }

    /**
     * Do something before the flow is started. At this time, [FlowRuntime.currentFlow] is
     * not yet assigned to this flow.
     */
    protected open fun onPreApply(ctx: AppletContext, runtime: FlowRuntime) {}

    open fun checkElements() {
        if (requiredElementCount != -1 && requiredElementCount != applets.size) {
            runtimeException(
                "This flow is expected to contain exactly $requiredElementCount applets" +
                        " but currently it is ${applets.size}!"
            )
        }
    }

    /**
     * Do something after all [applets] are completed.
     */
    protected open fun onPostApply(ctx: AppletContext, runtime: FlowRuntime) {
        // do nothing by default
    }
}