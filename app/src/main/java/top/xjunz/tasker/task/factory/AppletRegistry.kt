package top.xjunz.tasker.task.factory

import top.xjunz.tasker.engine.flow.Applet


/**
 * The registry storing all of the registered [Applet], which can be created by a unique id.
 *
 * @author xjunz 2022/08/09
 */
class AppletRegistry {

    companion object {

        inline val Applet.factoryId get() = id ushr 16

        inline val Applet.appletId get() = id and 0xFFFF

    }

    val flowFactory = FlowFactory()

    private val eventFilterFactory = EventFilterFactory(FlowFactory.ID_EVENT_FILTER_FACTORY)

    private val packageAppletFactory = PackageAppletFactory(FlowFactory.ID_PKG_APPLET_FACTORY)

    private val uiObjectAppletFactory =
        UiObjectAppletFactory(FlowFactory.ID_UI_OBJECT_APPLET_FACTORY)

    private val timeAppletFactory = TimeAppletFactory(FlowFactory.ID_TIME_APPLET_FACTORY)

    val allFactories = arrayOf(
        flowFactory,
        eventFilterFactory,
        packageAppletFactory,
        uiObjectAppletFactory,
        timeAppletFactory
    )

    val appletFactories = arrayOf(
        eventFilterFactory, packageAppletFactory, uiObjectAppletFactory, timeAppletFactory
    )

    fun findOption(applet: Applet): AppletOption {
        return findFactoryById(applet.factoryId).findAppletOptionById(applet.appletId)
    }

    fun createAppletById(id: Int, isInverted: Boolean): Applet {
        val factoryId = id ushr 16
        val appletId = id and 0xFFFF
        return findFactoryById(factoryId).createAppletFromId(appletId).also {
            it.isInverted = isInverted
        }
    }

    fun findFlowOption(factoryId: Int): AppletOption {
        return flowFactory.findAppletOptionById(factoryId)
    }

    fun findFactoryById(factoryId: Int): AppletFactory {
        return allFactories.first { it.id == factoryId }
    }
}

