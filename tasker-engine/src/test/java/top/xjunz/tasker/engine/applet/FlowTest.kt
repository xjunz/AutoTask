package top.xjunz.tasker.engine.applet

import org.junit.Test
import top.xjunz.tasker.engine.AutomatorTask
import top.xjunz.tasker.engine.applet.base.*
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.engine.runtime.TaskRuntime
import java.util.*


/**
 * @author xjunz 2022/08/13
 */
internal class FlowTest {

    private val mockTask = AutomatorTask("mock-task")

    @Test
    fun testFlowApply() {
        val rootFlow = DslFlow("abc") {
            label = "RootFlow"
            When(Event.EVENT_ON_PACKAGE_ENTERED)
            If {
                UnaryCriterion {
                    label = "notEqual"
                    Matcher { s, s2 -> s == s2 }
                    value = "a"
                    isInverted = true
                    Value("a", true)
                }

                UnaryCriterion {
                    isAnd = false
                    label = "equal"
                    Matcher { s, s2 -> s == s2 }
                    Value("b")
                }

                UnaryCriterion {
                    isAnd = false
                    label = "startsWith"
                    Value("x")
                    Matcher { s: String, s2: String -> s.startsWith(s2) }
                }

                UnaryCriterion {
                    label = "contains2"
                    Value("task")
                    Matcher { s: String, s2: String -> s.contains(s2) }
                }
            }
        }
        mockTask.activate(object : AutomatorTask.OnStateChangedListener {
            override fun onStarted() {
                println("---- onTaskStarted ----")
            }

            override fun onError(runtime: TaskRuntime, t: Throwable) {
                println("onAppletError: ${runtime.currentApplet}")
                t.printStackTrace()
            }

            override fun onFailure(runtime: TaskRuntime) {
                println("---- onAppletFailure: ${runtime.currentApplet} ----")
            }

            override fun onCancelled() {
                println("onTaskStopped")
            }
        })
        val events = arrayOf(
            Event.obtain(Event.EVENT_ON_PACKAGE_ENTERED, pkgName = "top.xjunz.tasker"),
            Event.obtain(Event.EVENT_ON_PACKAGE_EXITED, pkgName = "com.tencent.mm")
        )
        mockTask.flow = rootFlow
        val observer = object : TaskRuntime.Observer {
            override fun onStarted(victim: Applet, runtime: TaskRuntime) {
                if (victim is Flow)
                    println(indent(runtime.tracker.depth) + victim.isAndToString() + "$victim")
            }

            override fun onTerminated(victim: Applet, runtime: TaskRuntime) {
                println(indent(runtime.tracker.depth) + victim.isAndToString() + "$victim -> ${runtime.isSuccessful}")
            }

            override fun onSkipped(victim: Applet, runtime: TaskRuntime) {
                println(indent(runtime.tracker.depth) + victim.isAndToString() + "$victim -> skipped")
            }
        }
        TaskRuntime.clearGlobalVariables()
        assert(mockTask.launch(events, observer))
    }

    private fun Applet.isAndToString(): String {
        if (this is ControlFlow) return ""
        if (index == 0) return ""
        return if (isAnd) "And " else "Or "
    }

    private fun indent(count: Int): String {
        return Collections.nCopies(count, '-').joinToString("")
    }
}