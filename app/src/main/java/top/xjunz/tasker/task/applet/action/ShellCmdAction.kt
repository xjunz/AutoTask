/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.action

import android.app.UiAutomationHidden
import android.os.ParcelFileDescriptor.AutoCloseInputStream
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.applet.action.Action
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.service.uiAutomation

/**
 * @author xjunz 2023/04/03
 */
class ShellCmdAction(private val isFile: Boolean) : Action<String>(VAL_TYPE_TEXT) {

    override suspend fun doAction(value: String?, runtime: TaskRuntime): AppletResult {
        check(value != null) {
            "Shell cmd is empty!"
        }
        val cmd = if (isFile) "sh $value" else value
        val out = uiAutomation.casted<UiAutomationHidden>().executeShellCommandRwe(cmd)
        try {
            val stdErr = out[2]
            val err = AutoCloseInputStream(stdErr).bufferedReader().readText()
            return if (err.isEmpty()) {
                AppletResult.EMPTY_SUCCESS
            } else {
                AppletResult.failed(err)
            }
        } finally {
            out.forEach {
                it.close()
            }
        }
    }
}