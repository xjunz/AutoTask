/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option

import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.factory.AppletFactory
import top.xjunz.tasker.task.applet.option.registry.*


/**
 * The factory manufacturing all registered [AppletOption]s.
 *
 * @author xjunz 2022/08/09
 */
object AppletOptionFactory : AppletFactory {

    private var preloaded = false

    val flowRegistry = FlowOptionRegistry()

    val eventRegistry = EventCriterionRegistry(FlowOptionRegistry.ID_EVENT_FILTER_REGISTRY)

    val applicationRegistry = ApplicationCriterionRegistry(FlowOptionRegistry.ID_APP_OPTION_REGISTRY)

    val uiObjectRegistry =
        UiObjectCriterionRegistry(FlowOptionRegistry.ID_UI_OBJECT_OPTION_REGISTRY)

    val timeRegistry = TimeCriterionRegistry(FlowOptionRegistry.ID_TIME_OPTION_REGISTRY)

    private val globalInfoRegistry =
        GlobalCriterionRegistry(FlowOptionRegistry.ID_GLOBAL_OPTION_REGISTRY)

    val notificationRegistry =
        NotificationCriterionRegistry(FlowOptionRegistry.ID_NOTIFICATION_OPTION_REGISTRY)

    private val globalActionRegistry =
        GlobalActionRegistry(FlowOptionRegistry.ID_GLOBAL_ACTION_REGISTRY)

    private val uiObjectActionRegistry =
        UiObjectActionRegistry(FlowOptionRegistry.ID_UI_OBJECT_ACTION_REGISTRY)

    private val gestureActionRegistry =
        GestureActionRegistry(FlowOptionRegistry.ID_GESTURE_ACTION_REGISTRY)

    private val textActionRegistry = TextActionRegistry(FlowOptionRegistry.ID_TEXT_ACTION_REGISTRY)

    private val appActionRegistry =
        ApplicationActionRegistry(FlowOptionRegistry.ID_APP_ACTION_REGISTRY)

    val controlActionRegistry =
        ControlActionRegistry(FlowOptionRegistry.ID_CONTROL_ACTION_REGISTRY)

    private val allRegistries = arrayOf(
        // meta
        flowRegistry,
        // criterion
        eventRegistry,
        applicationRegistry,
        uiObjectRegistry,
        timeRegistry,
        globalInfoRegistry,
        notificationRegistry,
        // action
        globalActionRegistry,
        uiObjectActionRegistry,
        gestureActionRegistry,
        textActionRegistry,
        appActionRegistry,
        controlActionRegistry
    )

    fun requireOption(applet: Applet): AppletOption {
        return requireNotNull(findOption(applet)) {
            "Option for applet[$applet] not found!"
        }
    }

    fun findOption(applet: Applet): AppletOption? {
        return requireRegistryById(applet.registryId).findAppletOptionById(applet.appletId)
    }

    override fun createAppletById(id: Int): Applet {
        val registryId = id ushr 16
        val appletId = id and 0xFFFF
        return requireRegistryById(registryId).createAppletFromId(appletId)
    }

    fun preloadIfNeeded() {
        if (!preloaded) {
            allRegistries.forEach {
                it.parseDeclaredOptions()
            }
            preloaded = true
        }
    }

    fun resetAll() {
        allRegistries.forEach {
            it.reset()
        }
    }

    /**
     * Registry option is the applet option for a registry. Because a registry stores options of
     * a flow, registry option also means the option of the flow.
     *
     * @param registryId id of the registry, see [Applet.registryId].
     */
    fun requireRegistryOption(registryId: Int): AppletOption {
        return flowRegistry.findAppletOptionById(registryId)!!
    }

    fun requireRegistryById(registryId: Int): AppletOptionRegistry {
        return allRegistries.first {
            it.id == registryId
        }
    }
}

