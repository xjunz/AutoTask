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

    companion object {
        const val LABEL_AUTO_INVERTED = 0
        const val LABEL_NONE = -1
    }

    @get:StringRes
    abstract val label: Int

    abstract val name: String

    abstract val categoryNames: IntArray

    val appletOptions: List<AppletOption> by lazy {
        javaClass.declaredFields.mapNotNull m@{
            val anno = it.getDeclaredAnnotation(AppletCategory::class.java) ?: return@m null
            it.isAccessible = true
            val option = it.get(this) as AppletOption
            option.factoryId = this.id
            option.categoryId = anno.categoryId
            it.isAccessible = false
            return@m option
        }.sorted()
    }

    val categorizedAppletOptions: List<AppletOption> by lazy {
        val ret = ArrayList<AppletOption>()
        var previousCategory = -1
        appletOptions.forEach {
            if (it.categoryIndex != previousCategory) {
                ret.add(AppletCategoryOption(categoryNames[it.categoryIndex]))
            }
            ret.add(it)
            previousCategory = it.categoryIndex
        }
        return@lazy ret
    }

    /**
     * Create an [Applet] from an [id].
     */
    fun createApplet(id: Int): Applet {
        return appletOptions.first { it.appletId == id }.createApplet()
    }

}