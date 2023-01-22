/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.task

/**
 * @author xjunz 2022/12/04
 */
interface TaskScheduler : EventDispatcher.Callback {

    fun scheduleTasks(dispatcher: EventDispatcher) {
        dispatcher.addCallback(this)
    }

    fun release()
}