package top.xjunz.tasker.engine.flow

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import top.xjunz.tasker.engine.AppletContext
import top.xjunz.tasker.engine.AppletResult
import top.xjunz.tasker.engine.AutomatorTask
import top.xjunz.tasker.engine.Event
import top.xjunz.tasker.util.runtimeException
import java.util.*

/**
 * A flow is a set of [applets][Applet].
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
    open val requiredElementCount: Int = -1

    val applets: MutableList<Applet> = ArrayList(1)

    override fun apply(context: AppletContext, sharedResult: AppletResult) {
        if (shouldDropFlow(context, sharedResult)) return
        onPreApply(context, sharedResult)
        // Backup the value, because processor actions in this flow may change the value, we don't
        // want the processed value to fall through.
        val storedValue = sharedResult.getRawValue()
        sharedResult.depth++
        println(
            Collections.nCopies(sharedResult.depth, "-").joinToString(separator = "") + ": $name"
        )
        context.currentFlow = this
        prepare(context, sharedResult)
        applets.forEachIndexed { _, applet ->
            context.task.ensureActive()
            context.currentApplet = applet
            applet.apply(context, sharedResult)
            println(
                Collections.nCopies(sharedResult.depth, "-")
                    .joinToString(separator = "") + ": ${applet.name} " + "${sharedResult.isSuccessful}"
            )
        }
        // Restore the value
        sharedResult.setValue(storedValue)
        sharedResult.depth--
        onPostApply(context, sharedResult)
    }

    /**
     * Throw an exception to halt the whole flow. This is regarded as a normal termination.
     */
    protected fun stopship(ctx: AppletContext): Nothing {
        throw AutomatorTask.FlowFailureException(ctx)
    }

    protected open fun prepare(ctx: AppletContext, result: AppletResult) {

    }

    /**
     * If the return value is `true`, all [applets] in this flow will simply be dropped and the task head
     * will move to the next flow.
     */
    protected open fun shouldDropFlow(ctx: AppletContext, result: AppletResult): Boolean {
        return false
    }

    /**
     * Do something before the flow is started. At this time, [AppletContext.currentFlow] is
     * not yet assigned to this flow.
     */
    protected open fun onPreApply(ctx: AppletContext, result: AppletResult) {}

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
    protected open fun onPostApply(ctx: AppletContext, result: AppletResult) {
        // do nothing by default
    }
}