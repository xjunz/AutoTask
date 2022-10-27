package top.xjunz.tasker.engine.applet.serialization

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import top.xjunz.tasker.engine.applet.base.*
import top.xjunz.tasker.engine.applet.serialization.SerializableApplet.Companion.toSerializable
import top.xjunz.tasker.engine.value.Event

/**
 * @author xjunz 2022/10/28
 */
internal class SerializableAppletTest {

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
                    }
                }
            }
        }
       val json = Json {
            prettyPrint = true
        }
        println(json.encodeToString(rootFlow.toSerializable()))
    }
}