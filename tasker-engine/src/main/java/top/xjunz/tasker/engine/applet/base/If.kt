/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.base

/**
 * @author xjunz 2022/08/11
 */
open class If : ControlFlow() {

    override fun staticCheckMyself(): Int {
        if (requireParent().getOrNull(index + 1)?.javaClass != Do::class.java) {
            return StaticError.ERR_IF_NOT_FOLLOWED_BY_DO
        }
        return super.staticCheckMyself()
    }
}