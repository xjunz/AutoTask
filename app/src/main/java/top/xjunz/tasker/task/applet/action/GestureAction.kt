/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.action

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.applet.action.SingleArgAction
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.task.gesture.SerializableInputEvent

/**
 * @author xjunz 2023/02/18
 */
class GestureAction : SingleArgAction<List<SerializableInputEvent>>() {

    override suspend fun doAction(arg: List<SerializableInputEvent>?, runtime: TaskRuntime)
            : AppletResult {
        runtime.registerReferent(arg)
        for (it in arg as List<*>) {
            if (!(it as SerializableInputEvent).execute()) {
                return AppletResult.EMPTY_FAILURE
            }
        }
        return AppletResult.EMPTY_SUCCESS
    }

    override fun serializeArgumentToString(which: Int, rawType: Int, arg: Any): String {
        return Json.encodeToString<List<SerializableInputEvent>>(arg.casted())
    }

    override fun deserializeArgumentFromString(which: Int, rawType: Int, src: String): Any {
        return Json.decodeFromString<List<SerializableInputEvent>>(src)
    }
}