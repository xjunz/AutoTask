package top.xjunz.tasker.task.applet.option.registry

import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.task.applet.anno.AppletCategory
import top.xjunz.tasker.task.applet.option.AppletCategoryOption
import top.xjunz.tasker.task.applet.option.AppletOption

/**
 * The abstract registry storing [AppletOption].
 *
 * @author xjunz 2022/08/11
 */
abstract class AppletOptionRegistry(val id: Int) {

    abstract val title: Int

    abstract val categoryNames: IntArray

    private fun parseDeclaredOptions(): List<AppletOption> {
        return javaClass.declaredFields.mapNotNull m@{
            val anno = it.getDeclaredAnnotation(AppletCategory::class.java) ?: return@m null
            val accessible = it.isAccessible
            it.isAccessible = true
            val option = it.get(this) as AppletOption
            option.factoryId = id
            option.categoryId = anno.categoryId
            it.isAccessible = accessible
            return@m option
        }.sorted()
    }

    val options: List<AppletOption> by lazy {
        parseDeclaredOptions()
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
     * Brand all options with my factory id. If you access options via [AppletOptionRegistry.options] or
     * [AppletOptionRegistry.categorizedOptions], calling this method is not necessary.
     */
    fun brandAll() {
        for (field in javaClass.declaredFields) {
            field.getDeclaredAnnotation(AppletCategory::class.java) ?: continue
            val accessible = field.isAccessible
            field.isAccessible = true
            val option = field.get(this) as AppletOption
            option.factoryId = id
            field.isAccessible = accessible
        }
    }

    fun createAppletFromId(id: Int): Applet {
        return options.first { it.appletId == id }.yieldApplet()
    }

    fun findAppletOptionById(id: Int): AppletOption {
        return options.first { it.appletId == id }
    }
}