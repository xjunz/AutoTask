package top.xjunz.tasker.task.factory

import top.xjunz.tasker.engine.flow.Applet


/**
 * The registry storing all of the registered [Applet], which can be created by a unique id.
 *
 * @author xjunz 2022/08/09
 */
class AppletRegistry {

    companion object {
        const val ID_EVENT_FILTER_FACTORY = 0
        const val ID_PKG_APPLET_FACTORY = 1
    }

    val allFactories = arrayOf(PackageCriteriaFactory())

    private inline val Applet.factoryId get() = id ushr 16

    private inline val Applet.appletId get() = id and 0xFFFF

    fun createAppletById(id: Int, isInverted: Boolean): Applet {
        val factoryId = id ushr 16
        val appletId = id and 0xFFFF
        return allFactories.single {
            it.id == factoryId
        }.createApplet(appletId).also {
            it.isInverted = isInverted
        }
    }
}

