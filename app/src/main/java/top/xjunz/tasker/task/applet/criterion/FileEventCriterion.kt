/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.criterion

import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.engine.runtime.TaskRuntime
import java.io.File

/**
 * @author xjunz 2023/10/11
 */
class FileEventCriterion(private val eventType: Int) : Applet() {

    private var isExistent: Boolean? = null

    override var relation: Int = REL_OR

    private val targetFile: File by lazy {
        File(singleValue as String)
    }

    override suspend fun apply(runtime: TaskRuntime): AppletResult {
        var distinct = false
        if (isExistent == null) {
            isExistent = targetFile.exists()
        } else {
            val currentlyIsExistent = targetFile.exists()
            if (eventType == Event.EVENT_ON_FILE_CREATED) {
                distinct = isExistent == false && currentlyIsExistent
            } else if (eventType == Event.EVENT_ON_FILE_DELETED) {
                distinct = isExistent == true && !currentlyIsExistent
            }
            isExistent = currentlyIsExistent
        }
        return if (distinct) {
            AppletResult.succeeded(targetFile.path)
        } else {
            AppletResult.EMPTY_FAILURE
        }
    }
}