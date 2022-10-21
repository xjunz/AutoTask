package top.xjunz.shared.ktx

/**
 * @author xjunz 2022/07/14
 */

@Throws(ClassCastException::class)
@Suppress("UNCHECKED_CAST")
fun <T> Any.casted(): T {
    return this as T
}