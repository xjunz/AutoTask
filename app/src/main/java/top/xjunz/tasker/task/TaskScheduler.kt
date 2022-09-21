package top.xjunz.tasker.task

import android.content.pm.ApplicationInfo
import android.content.pm.IPackageManager
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.system.Os
import rikka.shizuku.SystemServiceHelper
import top.xjunz.shared.ktx.unsafeCast
import top.xjunz.tasker.annotation.LocalAndRemote
import top.xjunz.tasker.app
import top.xjunz.tasker.engine.AppletContext
import top.xjunz.tasker.engine.AutomatorTask
import top.xjunz.tasker.engine.Event
import top.xjunz.tasker.isInHostProcess
import top.xjunz.tasker.service.AutomatorService

/**
 * @author xjunz 2022/08/05
 */
@LocalAndRemote
class TaskScheduler(private val service: AutomatorService, private val looper: Looper) {

    private val handler = object : Handler(looper) {

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val ctx = msg.obj.unsafeCast<AppletContext>()
            try {
                ctx.task.onEvent(ctx)
            } finally {
                msg.recycle()
                for (event in ctx.events) {
                    event.recycle()
                }
            }
        }

    }

    private fun getApplicationInfo(packageName: String): ApplicationInfo {
        return if (isInHostProcess) {
            app.packageManager.getApplicationInfo(packageName, 0)
        } else {
            IPackageManager.Stub.asInterface(SystemServiceHelper.getSystemService("package"))
                .getApplicationInfo(packageName, 0, Os.getuid())
        }
    }

    /**
     * Schedule all active tasks, all of which are running in the thread that owns the [looper].
     */
    fun scheduleTasks() {
        var previousPackage: String? = null
        var applicationInfo: ApplicationInfo? = null
        service.uiAutomation.setOnAccessibilityEventListener listener@{
            try {
                val currentPackage = it.packageName?.toString() ?: return@listener
                if (previousPackage == null) {
                    previousPackage = currentPackage
                    return@listener
                }
                val events: Array<Event>
                if (currentPackage != previousPackage) {
                    applicationInfo = getApplicationInfo(currentPackage)
                    events = arrayOf(
                        Event.obtain(Event.EVENT_ON_PACKAGE_ENTERED, currentPackage),
                        Event.obtain(Event.EVENT_ON_PACKAGE_EXITED, previousPackage!!)
                    )
                    previousPackage = currentPackage
                } else {
                    events = arrayOf(Event.obtain(Event.EVENT_ON_CONTENT_CHANGED, currentPackage))
                }
                TaskManager.getActiveTasks().forEach { task ->
                    val ctx = AppletContext(task, events, applicationInfo!!)
                    handler.obtainMessage(task.id, ctx).sendToTarget()
                }
            } finally {
                it.recycle()
            }
        }
    }

    fun stopTask(task: AutomatorTask) {
        handler.removeMessages(task.id)
        task.deactivate()
    }

    /**
     * Destroy the scheduler. After destroyed, you should not use it any more.
     */
    fun destroy() {
        service.uiAutomation.setOnAccessibilityEventListener(null)
        handler.removeCallbacksAndMessages(null)
    }

}