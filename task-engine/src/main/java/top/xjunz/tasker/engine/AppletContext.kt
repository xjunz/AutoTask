package top.xjunz.tasker.engine

import android.content.pm.ApplicationInfo
import top.xjunz.tasker.engine.flow.Applet
import top.xjunz.tasker.engine.flow.Flow

/**
 * The context which is needed when executing an [Applet].
 *
 * @author xjunz 2022/08/04
 */
class AppletContext(
    /**
     * The task where the applet is defined. The applet is expected to return when [AutomatorTask.isActive]
     * is `false`.
     */
    val task: AutomatorTask,

    /**
     * Events that are received by the task.
     */
    val events: Array<Event>,

    /**
     * The application info of current target package.
     */
    val applicationInfo: ApplicationInfo
) {
    lateinit var currentApplet: Applet

    lateinit var currentFlow: Flow
}