package top.xjunz.tasker.task.applet.option

import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.factory.AppletFactory
import top.xjunz.tasker.task.applet.option.registry.*


/**
 * The factory manufacturing all registered [AppletOption]s.
 *
 * @author xjunz 2022/08/09
 */
class AppletOptionFactory : AppletFactory {

    val flowRegistry = FlowOptionRegistry()

    val eventRegistry =
        EventFilterOptionRegistry(FlowOptionRegistry.ID_EVENT_FILTER_REGISTRY)

    val packageRegistry = PackageOptionRegistry(FlowOptionRegistry.ID_PKG_OPTION_REGISTRY)

    private val uiObjectRegistry =
        UiObjectOptionRegistry(FlowOptionRegistry.ID_UI_OBJECT_OPTION_REGISTRY)

    val timeAppletFactory = TimeOptionRegistry(FlowOptionRegistry.ID_TIME_OPTION_REGISTRY)

    private val allRegistries = arrayOf(
        flowRegistry, eventRegistry, packageRegistry, uiObjectRegistry, timeAppletFactory
    )

    fun findOption(applet: Applet): AppletOption {
        return findRegistryById(applet.registryId).findAppletOptionById(applet.appletId)
    }

    override fun createAppletById(id: Int): Applet {
        val registryId = id ushr 16
        val appletId = id and 0xFFFF
        return findRegistryById(registryId).createAppletFromId(appletId)
    }

    fun findFlowOption(registryId: Int): AppletOption {
        return flowRegistry.findAppletOptionById(registryId)
    }

    fun findRegistryById(registryId: Int): AppletOptionRegistry {
        return allRegistries.first { it.id == registryId }
    }
}

