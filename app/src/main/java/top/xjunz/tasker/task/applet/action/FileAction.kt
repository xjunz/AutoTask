/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.action

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.xjunz.tasker.engine.applet.action.Action
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.runtime.Referent
import top.xjunz.tasker.engine.runtime.TaskRuntime
import java.io.File

/**
 * @author xjunz 2023/10/15
 */
class FileAction(val action: Int) : Action(), Referent {

    companion object {
        const val ACTION_DELETE = 0
        const val ACTION_COPY = 1
        const val ACTION_RENAME = 2
        const val ACTION_WRITE = 3
    }

    private lateinit var firstFilePath: String

    private lateinit var secondFilePath: String

    override fun onPreApply(runtime: TaskRuntime) {
        super.onPreApply(runtime)
        firstFilePath = getArgument(0, runtime) as String
        if (action == ACTION_COPY || action == ACTION_RENAME) {
            secondFilePath = getArgument(1, runtime) as String
        }
        runtime.registerReferent(this)
    }

    override suspend fun apply(runtime: TaskRuntime): AppletResult {
        when (action) {
            ACTION_DELETE -> withContext(Dispatchers.IO) {
                File(firstFilePath).deleteRecursively()
            }

            ACTION_COPY -> withContext(Dispatchers.IO) {
                File(firstFilePath).copyRecursively(File(secondFilePath))
            }

            ACTION_RENAME -> withContext(Dispatchers.IO) {
                val destFile = File(secondFilePath)
                var dest = destFile
                if (destFile.exists() && destFile.isDirectory) {
                    dest = File(destFile, File(firstFilePath).name)
                }
                File(firstFilePath).renameTo(dest)
            }

            ACTION_WRITE -> withContext(Dispatchers.IO) {

            }
        }
        return AppletResult.EMPTY_SUCCESS
    }


    override fun getReferredValue(which: Int, runtime: TaskRuntime) {
        when (which) {
            0 -> firstFilePath
            1 -> secondFilePath
        }
    }

}