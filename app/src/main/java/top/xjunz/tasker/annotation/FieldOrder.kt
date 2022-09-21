package top.xjunz.tasker.annotation

/**
 * @author xjunz 2022/08/09
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class FieldOrder(val ordinal: Int)