package top.xjunz.tasker.engine.applet.base

import top.xjunz.tasker.engine.applet.criterion.DslCriterion
import top.xjunz.tasker.engine.value.Event

/**
 * @author xjunz 2022/08/13
 */
@DslMarker
internal annotation class FlowDsl

@FlowDsl
internal fun RootFlow(init: Flow.() -> Unit): Flow {
    return Flow().apply(init)
}

@FlowDsl
internal fun Flow.If(block: If.() -> Unit) {
    elements.add(If().apply(block))
}

@FlowDsl
internal fun Flow.When(@Event.EventType eventType: Int) {
    elements.add(When.ofEvent(eventType))
}

@FlowDsl
internal fun <T : Any, V : Any> Flow.DslCriterion(block: DslCriterion<T, V>.() -> Unit) {
    elements.add(DslCriterion<T, V>().apply(block))
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

@FlowDsl
internal fun Flow.And(block: And.() -> Unit) {
    elements.add(And().apply(block))
}

@FlowDsl
internal fun Flow.Or(block: Or.() -> Unit) {
    elements.add(Or().apply(block))
}