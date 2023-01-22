/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.base

import top.xjunz.tasker.engine.applet.action.LambdaAction
import top.xjunz.tasker.engine.applet.criterion.DslCriterion

/**
 * @author xjunz 2022/08/13
 */
@DslMarker
internal annotation class FlowDsl

@FlowDsl
internal fun DslFlow(initialTarget: Any? = null, init: RootFlow.() -> Unit): RootFlow {
    return DslFlow(initialTarget).apply(init)
}

@FlowDsl
internal fun Flow.If(block: If.() -> Unit) {
    add(If().apply(block))
}

@FlowDsl
internal fun Flow.Then(block: Do.() -> Unit) {
    add(Do().apply(block))
}

@FlowDsl
internal fun Do.Action(block: () -> Boolean) {
    add(LambdaAction<Any>(Applet.VAL_TYPE_IRRELEVANT) { _, _ ->
        block()
    })
}

@FlowDsl
internal fun <T : Any, V : Any> Flow.DslCriterion(block: DslCriterion<T, V>.() -> Unit) {
    add(DslCriterion<T, V>().apply(block))
}

@FlowDsl
internal fun <T : Any> Flow.UnaryCriterion(block: DslCriterion<T, T>.() -> Unit) {
    DslCriterion(block)
}

@FlowDsl
internal fun <T : Any, V : Any> DslCriterion<T, V>.Matcher(block: (T, V) -> Boolean) {
    matcher = block
}

@FlowDsl
internal fun <T : Any, V : Any> DslCriterion<T, V>.Value(what: V, isInverted: Boolean = false) {
    value = what
    this.isInverted = isInverted
}
