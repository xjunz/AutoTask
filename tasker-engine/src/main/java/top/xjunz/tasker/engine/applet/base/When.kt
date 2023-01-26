/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.base

/**
 * @author xjunz 2022/08/11
 */
class When : ControlFlow() {

    override val maxSize: Int = 1

    override val minSize: Int = 1

    override val requiredIndex: Int = 1

    override fun staticCheckMyself(): Int {
        if (requireParent().getOrNull(index + 1) == null) {
            return StaticError.ERR_WHEN_NO_FELLOW
        }
        return super.staticCheckMyself()
    }
}