/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.base

/**
 * @author xjunz 2022/11/03
 */
open class Do : ControlFlow() {

    override var relation: Int = REL_AND
}