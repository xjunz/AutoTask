package top.xjunz.tasker.task

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.engine.runtime.Snapshot
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.engine.task.EventDispatcher
import top.xjunz.tasker.engine.task.TaskScheduler
import top.xjunz.tasker.service.uiAutomatorBridge
import top.xjunz.tasker.task.event.A11yEventDispatcher
import kotlin.coroutines.CoroutineContext

/**
 * @author xjunz 2022/08/05
 */
class ResidentTaskScheduler(private val looper: Looper) : EventDispatcher.Callback, TaskScheduler {

    private val eventDispatcher = A11yEventDispatcher(this)

    private val handlerDispatcher = Handler(looper).asCoroutineDispatcher()

    /**
     * Schedule all active tasks, all of which are running in the thread that owns the [looper].
     */
    override fun scheduleTasks() {
        uiAutomatorBridge.addOnAccessibilityEventListener {
            launch {
                eventDispatcher.processAccessibilityEvent(it)
            }
        }
        uiAutomatorBridge.startReceivingEvents()
    }

    /**
     * Destroy the scheduler. After destroyed, you should not use it any more.
     */
    override fun destroy() {
        uiAutomatorBridge.stopReceivingEvents()
        cancel()
        TaskRuntime.drainPool()
    }

    override fun onEvents(events: Array<out Event>) {
        val snapshot = Snapshot()
        for (task in TaskManager.getEnabledResidentTasks()) {
            async {
                task.launch(snapshot, this, events)
            }.invokeOnCompletion {
                for (event in events)
                    event.recycle()
            }
        }
    }

    override val coroutineContext: CoroutineContext = handlerDispatcher + SupervisorJob()
}