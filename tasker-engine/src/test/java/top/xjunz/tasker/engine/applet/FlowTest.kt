/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet

import kotlinx.coroutines.DelicateCoroutinesApi
import org.junit.Test
import top.xjunz.tasker.engine.applet.base.*
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.engine.task.XTask
import java.util.*


/**
 * @author xjunz 2022/08/13
 */
internal class FlowTest {

    private val mockTask = XTask()

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun testFlowApply() {
        val rootFlow = DslFlow("abc") {
            comment = "RootFlow"
            If {
                UnaryCriterion {
                    comment = "notEqual"
                    value = "a"
                    isInverted = true
                    Value("a", true)
                    Matcher { s, s2 -> s == s2 }
                }

                UnaryCriterion {
                    isAnd = true
                    comment = "equal"
                    Value("b")
                    Matcher { s, s2 -> s == s2 }
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
            Then {
                Action {
                    println("Action!")
                    true
                }
            }
        }

        val events = arrayOf(
            Event.obtain(Event.EVENT_ON_PACKAGE_ENTERED, pkgName = "top.xjunz.tasker"),
            Event.obtain(Event.EVENT_ON_PACKAGE_EXITED, pkgName = "com.tencent.mm")
        )
        mockTask.flow = rootFlow
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