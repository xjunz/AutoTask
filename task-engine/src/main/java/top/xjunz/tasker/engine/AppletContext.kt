package top.xjunz.tasker.engine

import top.xjunz.shared.ktx.unsafeCast
import top.xjunz.tasker.engine.flow.Applet

/**
 * The context which is needed when executing an [Applet].
 *
 * @author xjunz 2022/08/04
 */
class AppletContext(
    /**
     * The task where the applet is defined.
     */
    val task: AutomatorTask,

    /**
     * Events that are received by the task.
     */
    val events: Array<Event>,
    val currentPackageName: String,
    val currentActivityName: String
) {

    /**
     * All applets with same id share the same argument.
     */
    private val argumentRegistry = mutableMapOf<Int, Any>()


    /**
     * Get the argument from the registry or initialize the argument and store it.
     */
    fun <T : Any> getOrPutArgument(id: Int, defValue: () -> T): T {
        return argumentRegistry.getOrPut(id, defValue).unsafeCast()
    }
}