package top.xjunz.tasker.task.applet.option

import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.factory.AppletFactory
import top.xjunz.tasker.task.applet.option.registry.*


/**
 * The factory manufacturing all registered [AppletOption]s and [Applet]s.
 *
 * @author xjunz 2022/08/09
 */
class AppletOptionFactory : AppletFactory {

    val flowFactory = FlowOptionRegistry()

    private val eventFilterFactory =
        EventFilterOptionRegistry(FlowOptionRegistry.ID_EVENT_FILTER_FACTORY)

    val packageAppletFactory = PackageOptionRegistry(FlowOptionRegistry.ID_PKG_APPLET_FACTORY)

    private val uiObjectAppletFactory =
        UiObjectOptionRegistry(FlowOptionRegistry.ID_UI_OBJECT_APPLET_FACTORY)

    val timeAppletFactory = TimeOptionRegistry(FlowOptionRegistry.ID_TIME_APPLET_FACTORY)

    private val allFactories = arrayOf(
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

    override fun createAppletById(id: Int): Applet {
        val factoryId = id ushr 16
        val appletId = id and 0xFFFF
        return findFactoryById(factoryId).createAppletFromId(appletId)
    }

    fun findFlowOption(factoryId: Int): AppletOption {
        return flowFactory.findAppletOptionById(factoryId)
    }

    fun findFactoryById(factoryId: Int): AppletOptionRegistry {
        return allFactories.first { it.id == factoryId }
    }
}

