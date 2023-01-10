/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.Test
import top.xjunz.tasker.engine.applet.base.*
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.engine.runtime.Snapshot
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.engine.task.XTask
import java.util.*


/**
 * @author xjunz 2022/08/13
 */
internal class FlowTest {

    private val mockTask = XTask()

    @Test
    fun testFlowApply() {
        val rootFlow = DslFlow("abc") {
            comment = "RootFlow"
            When(Event.EVENT_ON_PACKAGE_ENTERED)
            If {
                UnaryCriterion {
                    comment = "notEqual"
                    Matcher { s, s2 -> s == s2 }
                    value = "a"
                    isInverted = true
                    Value("a", true)
                }

                UnaryCriterion {
                    isAnd = false
                    comment = "equal"
                    Matcher { s, s2 -> s == s2 }
                    Value("b")
                }

                UnaryCriterion {
                    isAnd = false
                    comment = "startsWith"
                    Value("x")
                    Matcher { s: String, s2: String -> s.startsWith(s2) }
                }

                UnaryCriterion {
                    comment = "contains2"
                    Value("task")
                    Matcher { s: String, s2: String -> s.contains(s2) }
                }
            }
        }
        mockTask.enable(object : XTask.OnStateChangedListener {
            override fun onStarted(runtime: TaskRuntime) {
                println("---- onTaskStarted ----")
            }

            override fun onError(runtime: TaskRuntime, t: Throwable) {
                println("onAppletError: ${runtime.currentApplet}")
                t.printStackTrace()
            }

            override fun onFailure(runtime: TaskRuntime) {
                println("---- onAppletFailure: ${runtime.currentApplet} ----")
            }

            override fun onCancelled(runtime: TaskRuntime) {
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
        GlobalScope.launch {
            assert(mockTask.launch(Snapshot(), this, events, observer))
        }
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