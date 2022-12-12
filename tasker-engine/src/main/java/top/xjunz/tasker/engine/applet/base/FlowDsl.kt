package top.xjunz.tasker.engine.applet.base

import top.xjunz.tasker.engine.applet.criterion.DslCriterion
import top.xjunz.tasker.engine.applet.criterion.EventCriterion
import top.xjunz.tasker.engine.runtime.Event

/**
 * @author xjunz 2022/08/13
 */
@DslMarker
internal annotation class FlowDsl

@FlowDsl
internal fun DslFlow(initialTarget: Any? = null, init: Flow.() -> Unit): Flow {
    return DslFlow(initialTarget).apply(init)
}

@FlowDsl
internal fun Flow.If(block: If.() -> Unit) {
    add(If().apply(block))
}

@FlowDsl
internal fun Flow.When(@Event.EventType eventType: Int) {
    add(When().apply {
        if (isEmpty()) {
            add(EventCriterion(eventType))
        } else {
            this[0] = EventCriterion(eventType)
        }
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
