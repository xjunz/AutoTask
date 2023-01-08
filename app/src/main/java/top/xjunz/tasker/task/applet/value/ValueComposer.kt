/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.value

/**
 * @author xjunz 2023/01/08
 */
abstract class ValueComposer<Component, Value> {

    fun compose(vararg components: Component?): Value {
        return composeInternal(components)
    }

    protected abstract fun composeInternal(components: Array<out Component?>): Value

    abstract fun parse(composed: Value): Array<Component?>
}