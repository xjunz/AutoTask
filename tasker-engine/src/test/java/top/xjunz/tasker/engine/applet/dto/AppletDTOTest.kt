/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.dto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.DslFlow
import top.xjunz.tasker.engine.applet.base.If
import top.xjunz.tasker.engine.applet.base.UnaryCriterion
import top.xjunz.tasker.engine.dto.AppletDTO.Serializer.toDTO

/**
 * @author xjunz 2022/10/28
 */
internal class AppletDTOTest {

    @Test
    fun testFlowApply() {
        val rootFlow = DslFlow {
            id = 1
            comment = "RootFlow"
            If {
                id = 3
                UnaryCriterion<String> {
                    value = "hello"
                    valueType = Applet.VAL_TYPE_TEXT
                    id = 4
                }
                UnaryCriterion<Boolean> {
                    value = 1.28f
                    valueType = Applet.VAL_TYPE_FLOAT
                }
                UnaryCriterion<Any> {
                    value = "?"
                    valueType = Applet.VAL_TYPE_TEXT
                }
                UnaryCriterion<Any> {
                    value = listOf("1", "alpha", "xjunz")
                    valueType = Applet.MASK_VAL_TYPE_COLLECTION or Applet.VAL_TYPE_TEXT
                }
                UnaryCriterion<Int> {
                    value = 1
                    valueType = Applet.VAL_TYPE_INT
                    id = 5
                    isInverted = true
                }
            }
        }
        val json = Json {
            prettyPrint = true
        }
        println(json.encodeToString(rootFlow.toDTO()))
    }
}