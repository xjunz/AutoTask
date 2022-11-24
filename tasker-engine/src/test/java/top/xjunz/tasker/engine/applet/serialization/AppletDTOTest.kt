package top.xjunz.tasker.engine.applet.serialization

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import top.xjunz.tasker.engine.applet.base.DslFlow
import top.xjunz.tasker.engine.applet.base.If
import top.xjunz.tasker.engine.applet.base.UnaryCriterion
import top.xjunz.tasker.engine.applet.base.When
import top.xjunz.tasker.engine.applet.serialization.AppletDTO.Companion.toDTO
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.engine.value.Distance

/**
 * @author xjunz 2022/10/28
 */
internal class AppletDTOTest {

    @Test
    fun testFlowApply() {
        val rootFlow = DslFlow {
            id = 1
            label = "RootFlow"
            When(Event.EVENT_ON_PACKAGE_ENTERED)
            If {
                referred = "If"
                id = 3
                UnaryCriterion<String> {
                    value = "hello"
                    valueType = AppletValues.VAL_TYPE_TEXT
                    id = 4
                }
                UnaryCriterion<Boolean> {
                    value = 1.28f
                    valueType = AppletValues.VAL_TYPE_FLOAT
                }
                UnaryCriterion<Any> {
                    value = Distance.exactDpInParent(1.23F)
                    valueType = AppletValues.VAL_TYPE_DISTANCE
                }
                UnaryCriterion<Any> {
                    value = listOf("1", "alpha", "xjunz")
                    valueType = AppletValues.MASK_VAL_TYPE_COLLECTION or AppletValues.VAL_TYPE_TEXT
                }
                UnaryCriterion<Int> {
                    value = 1
                    valueType = AppletValues.VAL_TYPE_INT
                    id = 5
                    isInverted = true
                    isAnd = false
                }
            }
        }
        val json = Json {
            prettyPrint = true
        }
        println(json.encodeToString(rootFlow.toDTO()))
    }
}