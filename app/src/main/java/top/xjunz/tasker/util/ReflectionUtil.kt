package top.xjunz.tasker.util

import top.xjunz.shared.ktx.casted
import java.lang.reflect.ParameterizedType

/**
 * @author xjunz 2022/07/14
 */
object ReflectionUtil {

    fun <T : Any> Any.requireFieldFromSuperClass(fieldName: String): T {
        return javaClass.superclass.getDeclaredField(fieldName).apply {
            isAccessible = true
        }.get(this)!!.casted()
    }

    fun Class<*>.superClassFirstParameterizedType(): Class<*> {
        return (genericSuperclass as ParameterizedType).actualTypeArguments.first().casted()
    }
}