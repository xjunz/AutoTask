package top.xjunz.tasker.engine.base

import top.xjunz.tasker.engine.Event
import top.xjunz.tasker.engine.criterion.BaseCriterion

/**
 * @author xjunz 2022/08/13
 */
@DslMarker
internal annotation class FlowDsl

@FlowDsl
fun RootFlow(init: Flow.() -> Unit): Flow {
    return Flow().apply(init)
}

@FlowDsl
fun Flow.If(block: If.() -> Unit) {
    applets.add(If().apply(block))
}

@FlowDsl
fun Flow.When(@Event.EventType eventType: Int) {
    applets.add(When.ofEvent(eventType))
}

@FlowDsl
fun <T : Any, V : Any> Flow.BaseCriterion(block: BaseCriterion<T, V>.() -> Unit) {
    applets.add(BaseCriterion<T, V>().apply(block))
}

@FlowDsl
fun <T : Any> Flow.UnaryCriterion(block: BaseCriterion<T, T>.() -> Unit) {
    BaseCriterion(block)
}

@FlowDsl
fun <T : Any, V : Any> BaseCriterion<T, V>.Matcher(block: (T, V) -> Boolean) {
    matcher = block
}

@FlowDsl
fun <T : Any, V : Any> BaseCriterion<T, V>.Value(what: V, isInverted: Boolean = false) {
    value = what
    this.isInverted = isInverted
}

@FlowDsl
fun Flow.And(block: And.() -> Unit) {
    applets.add(And().apply(block))
}

@FlowDsl
fun Flow.Or(block: Or.() -> Unit) {
    applets.add(Or().apply(block))
}