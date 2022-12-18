package top.xjunz.tasker.engine.applet.base

import top.xjunz.shared.utils.runtimeException
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.engine.task.XTask

/**
 * @author xjunz 2022/11/04
 */
abstract class ControlFlow : Flow() {

    final override var isInvertible: Boolean = false

    /**
     * Throw an exception to halt the whole flow. This is regarded as a normal termination.
     */
    protected fun stopship(runtime: TaskRuntime): Nothing {
        throw XTask.FlowFailureException("Stopship at ${runtime.tracker.formatTrace()}!")
    }

    override fun staticCheckMySelf() {
        super.staticCheckMySelf()
        if (requiredSize != -1 && requiredSize != size)
            runtimeException(
                "This flow is expected to contain exactly " +
                        "$requiredSize applets but currently it is ${size}!"
            )
        if (size > MAX_FLOW_CHILD_COUNT)
            runtimeException("Child count overflow!")
    }
}