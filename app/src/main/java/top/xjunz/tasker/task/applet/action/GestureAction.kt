/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.action

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.applet.action.ReferenceAction
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.task.gesture.SerializableInputEvent

/**
 * @author xjunz 2023/02/18
 */
class GestureAction : ReferenceAction<List<SerializableInputEvent>>(VAL_TYPE_TEXT) {

    override suspend fun doWithArgs(
        args: Array<Any?>,
        value: List<SerializableInputEvent>?,
        runtime: TaskRuntime
    ): AppletResult {
        val gestures = args.getOrNull(0) ?: value
        runtime.registerReferent(gestures)
        for (it in gestures as List<*>) {
            if (!(it as SerializableInputEvent).execute()) {
                return AppletResult.EMPTY_FAILURE
            }
        }
        return AppletResult.EMPTY_SUCCESS
    }

    override fun serializeValueToString(value: Any): String {
        return Json.encodeToString<List<SerializableInputEvent>>(value.casted())
    }

    override fun deserializeValueFromString(src: String): Any {
        return Json.decodeFromString<List<SerializableInputEvent>>(src)
    }
}