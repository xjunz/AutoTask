package top.xjunz.tasker

import org.junit.Test
import top.xjunz.tasker.engine.AppletContext
import top.xjunz.tasker.engine.AutomatorTask
import top.xjunz.tasker.engine.Event
import top.xjunz.tasker.engine.flow.*


/**
 * @author xjunz 2022/08/13
 */
internal class FlowTest {

    object MockTask : AutomatorTask("mock-task")

    @Test
    fun testFlowApply() {
        val rootFlow = RootFlow {
            name = "RootFlow"
            When(Event.EVENT_ON_PACKAGE_ENTERED)
            If {
                UnaryCriterion {
                    name = "notEqual"
                    Matcher { s, s2 -> s == s2 }
                    Value("a", true)
                }
                And {
                    name = "And 1"
                    UnaryCriterion {
                        name = "equal"
                        Matcher { s, s2 -> s == s2 }
                        Value("b")
                    }
                }
                Or {
                    UnaryCriterion {
                        name = "startsWith"
                        Value("x")
                        Matcher { s: String, s2: String -> s.startsWith(s2) }
                    }
                    And {
                        name = "And 2"
                        UnaryCriterion {
                            name = "contains"
                            Value("auto")
                            Matcher { s: String, s2: String -> s.contains(s2) }
                        }
                        Or {
                            UnaryCriterion {
                                name = "contains2"
                                Value("task")
                                Matcher { s: String, s2: String -> s.contains(s2) }
                            }
                        }
                    }
                }
            }
        }
        MockTask.activate(object : AutomatorTask.OnStateChangedListener {
            override fun onTaskStarted() {
                println("onTaskStarted")
            }

            override fun onAppletError(ctx: AppletContext, t: Throwable) {
                println("onAppletError: ${ctx.currentApplet}")
                t.printStackTrace()
            }

            override fun onAppletFailure(ctx: AppletContext) {
                println("onAppletFailure: ${ctx.currentApplet}")
            }

            override fun onTaskStopped() {
                println("onTaskStopped")
            }
        })
        val events = arrayOf(
            Event.obtain(Event.EVENT_ON_PACKAGE_ENTERED, "top.xjunz.tasker"),
            Event.obtain(Event.EVENT_ON_PACKAGE_EXITED, "com.tencent.mm")
        )
        MockTask.rootFlow = rootFlow
        assert(MockTask.onEvent(events))
    }
}