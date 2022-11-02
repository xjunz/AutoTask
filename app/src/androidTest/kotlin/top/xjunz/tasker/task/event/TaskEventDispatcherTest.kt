package top.xjunz.tasker.task.event

import android.os.Looper
import org.junit.Test
import top.xjunz.tasker.UiAutomationRegistry

/**
 * @author xjunz 2022/11/01
 */
internal class TaskEventDispatcherTest {

    @Test
    fun processAccessibilityEvent() {
        val dispatcher = TaskEventDispatcher(Looper.getMainLooper()) {
            println(it.joinToString())
        }
        UiAutomationRegistry.getUiAutomation().setOnAccessibilityEventListener {
            dispatcher.processAccessibilityEvent(it)
        }
    }
}