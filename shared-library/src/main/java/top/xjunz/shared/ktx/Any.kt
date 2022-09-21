package top.xjunz.shared.ktx

/**
 * @author xjunz 2022/07/14
 */

@Throws(ClassCastException::class)
@Suppress("UNCHECKED_CAST")
fun <T> Any.unsafeCast(): T {
    return this as T
}