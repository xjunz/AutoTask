package top.xjunz.tasker.task.factory

import androidx.annotation.StringRes
import top.xjunz.tasker.engine.flow.Applet
import top.xjunz.tasker.task.anno.AppletCategory

/**
 * The abstract factory manufacturing [applets][Applet].
 *
 * @author xjunz 2022/08/11
 */
abstract class AppletFactory(val id: Int) {

    @get:StringRes
    abstract val title: Int

    abstract val categoryNames: IntArray

    fun findAppletOptionById(id: Int): AppletOption {
        return options.first { it.appletId == id }
    }

    val options: List<AppletOption> by lazy {
        javaClass.declaredFields.mapNotNull m@{
            val anno = it.getDeclaredAnnotation(AppletCategory::class.java) ?: return@m null
            it.isAccessible = true
            val option = it.get(this) as AppletOption
            option.factoryId = id
            option.categoryId = anno.categoryId
            it.isAccessible = false
            return@m option
        }.sorted()
    }

    val categorizedOptions: List<AppletOption> by lazy {
        val ret = ArrayList<AppletOption>()
        var previousCategory = -1
        options.forEach {
            if (it.categoryIndex != previousCategory) {
                if (categoryNames[it.categoryIndex] != AppletOption.TITLE_NONE) {
                    ret.add(AppletCategoryOption(categoryNames[it.categoryIndex]))
                }
            }
            ret.add(it)
            previousCategory = it.categoryIndex
        }
        return@lazy ret
    }

    /**
     * Create an [Applet] from an [id].
     */
    fun createAppletFromId(id: Int): Applet {
        return options.first { it.appletId == id }.createApplet()
    }
}