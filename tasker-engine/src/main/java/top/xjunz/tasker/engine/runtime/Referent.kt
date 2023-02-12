/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.runtime

/**
 * @author xjunz 2023/02/12
 */
interface Referent {

    fun getFieldValue(which: Int): Any {
        if (which == 0) {
            return this
        }
        throwIfFieldNotFound(which)
    }

    fun throwIfFieldNotFound(which: Int): Nothing {
        throw NullPointerException("Field $which is not found!")
    }

}