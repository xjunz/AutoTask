package top.xjunz.tasker.task.applet.anno

import androidx.annotation.Keep

/**
 * |0000 0000|│|0000 0000|
 * |  :----: |:----: | :----:  |
 * |id|│|index|
 *
 * The first 8 bits of [categoryId] is the category id and the last 8 bits of [categoryId] is
 * the index in the category.
 *
 * @author xjunz 2022/09/22
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
@Keep
annotation class AppletCategory(val categoryId: Int)