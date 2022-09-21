package top.xjunz.tasker.engine.criterion

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * @author xjunz 2022/08/14
 */
@Serializable
@SerialName("BaseCriterion")
class BaseCriterion<T : Any, V : Any> : Criterion<T, V>() {

    @Transient
    lateinit var matcher: ((T, V) -> Boolean)

    override fun matchTarget(target: T, value: V): Boolean {
        return matcher(target, value)
    }
}

fun <T : Any, V : Any> BaseCriterion(matcher: ((T, V) -> Boolean)): BaseCriterion<T, V> {
    return BaseCriterion<T, V>().apply { this.matcher = matcher }
}