/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.action

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import top.xjunz.tasker.bridge.VibratorBridge
import top.xjunz.tasker.engine.applet.action.SingleArgAction
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.dto.XTaskJson
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2023/10/26
 */
class Vibrate : SingleArgAction<Vibrate.VibrationWaveForm>() {

    @Serializable
    class VibrationWaveForm(
        @SerialName("ds")
        val durations: LongArray,
        @SerialName("ss")
        val strengths: IntArray
    )

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun doAction(arg: VibrationWaveForm?, runtime: TaskRuntime): AppletResult {
        VibratorBridge.performVibrate(arg!!)
        return AppletResult.EMPTY_SUCCESS
    }

    override fun serializeArgumentToString(which: Int, rawType: Int, arg: Any): String {
        return XTaskJson.encodeToString(values[0] as VibrationWaveForm)
    }

    override fun deserializeArgumentFromString(which: Int, rawType: Int, src: String): Any {
        return XTaskJson.decodeFromString<VibrationWaveForm>(src)
    }
}