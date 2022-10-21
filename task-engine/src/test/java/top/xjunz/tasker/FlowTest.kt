package top.xjunz.tasker

import org.junit.Test
import top.xjunz.tasker.engine.AutomatorTask
import top.xjunz.tasker.engine.Event
import top.xjunz.tasker.engine.FlowRuntime
import top.xjunz.tasker.engine.base.*


/**
 * @author xjunz 2022/08/13
 */
internal class FlowTest {

    object MockTask : AutomatorTask("mock-task")

    @Test
    fun testFlowApply() {
        val rootFlow = RootFlow {
            label = "RootFlow"
            When(Event.EVENT_ON_PACKAGE_ENTERED)
            If {
                UnaryCriterion {
                    label = "notEqual"
                    Matcher { s, s2 -> s == s2 }
                    Value("a", true)
                }
                And {
                    label = "And 1"
                    UnaryCriterion {
                        label = "equal"
                        Matcher { s, s2 -> s == s2 }
                        Value("b")
                    }
                }
                Or {
                    UnaryCriterion {
                        label = "startsWith"
                        Value("x")
                        Matcher { s: String, s2: String -> s.startsWith(s2) }
                    }
                    And {
                        label = "And 2"
                        UnaryCriterion {
                            label = "contains"
                            Value("auto")
                            Matcher { s: String, s2: String -> s.contains(s2) }
                        }
                        Or {
                            UnaryCriterion {
                                label = "contains2"
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

            override fun onAppletError(runtime: FlowRuntime, t: Throwable) {
                println("onAppletError: ${runtime.currentApplet}")
                t.printStackTrace()
            }

            override fun onAppletFailure(runtime: FlowRuntime) {
                println("onAppletFailure: ${runtime.currentApplet}")
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
      //  assert(MockTask.onEvent(events))
    }
}