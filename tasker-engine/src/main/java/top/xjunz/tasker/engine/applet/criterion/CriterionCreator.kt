/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.criterion

import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2023/09/24
 */

fun <T, V> createCriterion(matcher: (T, V) -> AppletResult): Criterion<T, V> {
    return object : Criterion<T, V>() {
        override fun matchTarget(target: T, value: V): AppletResult {
            return matcher(target, value)
        }
    }
}

fun <T> propertyCriterion(matcher: (target: T) -> Boolean): Criterion<T, Boolean> {
    return object : Criterion<T, Boolean>() {

        override fun getDefaultMatchValue(runtime: TaskRuntime): Boolean {
            return true
        }

        override fun matchTarget(target: T, value: Boolean): AppletResult {
            return AppletResult.emptyResult(matcher(target) == value)
        }
    }
}

/**
 * Create a criterion whose match value may be a reference.
 */
fun <T, V> referenceValueCriterion(matcher: (T, V) -> AppletResult): Criterion<T, V> {
    return object : Criterion<T, V>() {

        override fun getDefaultMatchValue(runtime: TaskRuntime): V {
            return requireNotNull(runtime.getReferenceArgument(this, if (isScoped) 0 else 1)) {
                "The reference match value if not found!"
            }.casted()
        }

        override fun matchTarget(target: T, value: V): AppletResult {
            return matcher(target, value)
        }
    }
}

fun booleanCriterion(checker: () -> Boolean): Criterion<Unit, Boolean> {
    return propertyCriterion {
        checker()
    }
}

/**
 * Create a new criterion that maps its match target to the same type as the match value's
 * and simply compare whether they are equal.
 */
fun <T, V> equalCriterion(mapper: (T) -> V?): Criterion<T, V> {
    return createCriterion { t, v ->
        AppletResult.resultOf(mapper(t)) {
            it == v
        }
    }
}

fun <V> unaryEqualCriterion(getter: () -> V?): Criterion<V, V> {
    return equalCriterion { getter() }
}

fun <T, V> collectionCriterion(mapper: (T) -> V?): Criterion<T, List<V>> {
    return createCriterion { t, v ->
        AppletResult.resultOf(mapper(t)) {
            v.contains(it)
        }
    }
}