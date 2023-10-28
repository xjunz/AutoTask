/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.bridge

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RequiresApi
import top.xjunz.tasker.task.applet.action.Vibrate

/**
 * @author xjunz 2023/10/28
 */
object VibratorBridge {

    private val vibrator by lazy {
        ContextBridge.getContext().getSystemService(Vibrator::class.java)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun performVibrate(waveForm: Vibrate.VibrationWaveForm) {
        vibrator.vibrate(VibrationEffect.createWaveform(waveForm.durations, waveForm.strengths, -1))
    }
}