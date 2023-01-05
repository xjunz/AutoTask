/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry

import top.xjunz.shared.utils.unsupportedOperation
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.task.applet.anno.AppletCategory
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.task.applet.option.AppletOption.Companion.TITLE_NONE

/**
 * The abstract registry storing [AppletOption]s.
 *
 * @author xjunz 2022/08/11
 */
abstract class AppletOptionRegistry(val id: Int) {

    protected open val categoryNames: IntArray? = null

    fun parseDeclaredOptions() {
        val registeredId = mutableSetOf<Int>()
        allOptions = javaClass.declaredFields.mapNotNull m@{
            val anno = it.getDeclaredAnnotation(AppletCategory::class.java) ?: return@m null
            val accessible = it.isAccessible
            it.isAccessible = true
            val option = it.get(this) as AppletOption
            // There may be preset appletId
            val appletId = if (option.appletId != -1) option.appletId
            else it.name.hashCode() and 0xFFFF
            check(registeredId.add(appletId))
            option.appletId = appletId
            option.categoryId = anno.categoryId
            it.isAccessible = accessible
            return@m option
        }.sorted()
    }

    fun reset() {
        allOptions.forEach {
            it.isInverted = false
            it.value = null
        }
    }

    private fun appletCategoryOption(label: Int): AppletOption {
        return AppletOption(id, label, TITLE_NONE) {
            unsupportedOperation()
        }
    }

    protected fun invertibleAppletOption(title: Int, creator: () -> Applet): AppletOption {
        return AppletOption(id, title, AppletOption.TITLE_AUTO_INVERTED, creator)
    }

    protected fun appletOption(title: Int, creator: () -> Applet): AppletOption {
        return AppletOption(id, title, TITLE_NONE, creator)
    }

    lateinit var allOptions: List<AppletOption>

    val categorizedOptions: List<AppletOption> by lazy {
        val ret = ArrayList<AppletOption>()
        var previousCategory = -1
        allOptions.forEach {
            if (it.categoryIndex != previousCategory) {
                val name = categoryNames?.getOrNull(it.categoryIndex)
                if (name != null && name != TITLE_NONE) {
                    ret.add(appletCategoryOption(name))
                }
            }
            ret.add(it)
            previousCategory = it.categoryIndex
        }
        return@lazy ret
    }

    fun createAppletFromId(id: Int): Applet {
        return allOptions.first { it.appletId == id }.yield()
    }

    fun findAppletOptionById(id: Int): AppletOption? {
        return allOptions.firstOrNull { it.appletId == id }
    }
}