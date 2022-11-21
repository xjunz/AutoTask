package top.xjunz.tasker.engine.applet.base

import top.xjunz.shared.utils.runtimeException
import top.xjunz.tasker.engine.AutomatorTask
import top.xjunz.tasker.engine.runtime.FlowRuntime

/**
 * @author xjunz 2022/11/04
 */
abstract class ControlFlow : Flow() {

    open val requiredElementCount: Int = -1

    final override var isInvertible: Boolean = false

    /**
     * Throw an exception to halt the whole flow. This is regarded as a normal termination.
     */
    protected fun stopship(runtime: FlowRuntime): Nothing {
        throw AutomatorTask.FlowFailureException("Stopship at ${runtime.tracker.formatTrace()}!")
    }

    override fun staticCheckMySelf() {
        super.staticCheckMySelf()
        if (requiredElementCount != -1 && requiredElementCount != size)
            runtimeException(
                "This flow is expected to contain exactly " +
                        "$requiredElementCount applets but currently it is ${size}!"
            )
        if (size > MAX_FLOW_CHILD_COUNT)
            runtimeException("Child count overflow!")
    }
}