/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.base

/**
 * @author xjunz 2022/12/14
 */
class ContainerFlow : Flow() {

    override var maxSize: Int = MAX_FLOW_CHILD_COUNT

    override var minSize: Int = 1
}