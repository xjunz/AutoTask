package top.xjunz.tasker.engine

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
)