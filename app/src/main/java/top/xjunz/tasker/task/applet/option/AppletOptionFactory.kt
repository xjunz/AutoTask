/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option

import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.factory.AppletFactory
import top.xjunz.tasker.task.applet.option.registry.AppletOptionRegistry
import top.xjunz.tasker.task.applet.option.registry.ApplicationActionRegistry
import top.xjunz.tasker.task.applet.option.registry.ApplicationCriterionRegistry
import top.xjunz.tasker.task.applet.option.registry.BootstrapOptionRegistry
import top.xjunz.tasker.task.applet.option.registry.ControlActionRegistry
import top.xjunz.tasker.task.applet.option.registry.EventCriterionRegistry
import top.xjunz.tasker.task.applet.option.registry.FileActionRegistry
import top.xjunz.tasker.task.applet.option.registry.GestureActionRegistry
import top.xjunz.tasker.task.applet.option.registry.GlobalActionRegistry
import top.xjunz.tasker.task.applet.option.registry.GlobalCriterionRegistry
import top.xjunz.tasker.task.applet.option.registry.NetworkCriterionRegistry
import top.xjunz.tasker.task.applet.option.registry.ShellCmdActionRegistry
import top.xjunz.tasker.task.applet.option.registry.TextActionRegistry
import top.xjunz.tasker.task.applet.option.registry.TextCriterionRegistry
import top.xjunz.tasker.task.applet.option.registry.TimeCriterionRegistry
import top.xjunz.tasker.task.applet.option.registry.UiObjectActionRegistry
import top.xjunz.tasker.task.applet.option.registry.UiObjectCriterionRegistry
import top.xjunz.tasker.task.applet.option.registry.UiObjectFlowRegistry


/**
 * The factory manufacturing all registered [AppletOption]s.
 *
 * @author xjunz 2022/08/09
 */
object AppletOptionFactory : AppletFactory {

    private var preloaded = false

    val flowRegistry = BootstrapOptionRegistry()

    val eventRegistry = EventCriterionRegistry(BootstrapOptionRegistry.ID_EVENT_FILTER_REGISTRY)

    val applicationRegistry =
        ApplicationCriterionRegistry(BootstrapOptionRegistry.ID_APP_CRITERION_REGISTRY)

    val uiObjectRegistry =
        UiObjectCriterionRegistry(BootstrapOptionRegistry.ID_UI_OBJECT_CRITERION_REGISTRY)

    private val timeRegistry =
        TimeCriterionRegistry(BootstrapOptionRegistry.ID_TIME_CRITERION_REGISTRY)

    private val globalInfoRegistry =
        GlobalCriterionRegistry(BootstrapOptionRegistry.ID_GLOBAL_CRITERION_REGISTRY)

    private val textRegistry =
        TextCriterionRegistry(BootstrapOptionRegistry.ID_TEXT_CRITERION_REGISTRY)

    private val networkRegistry =
        NetworkCriterionRegistry(BootstrapOptionRegistry.ID_NETWORK_CRITERION_REGISTRY)

    private val globalActionRegistry =
        GlobalActionRegistry(BootstrapOptionRegistry.ID_GLOBAL_ACTION_REGISTRY)

    val uiObjectActionRegistry =
        UiObjectActionRegistry(BootstrapOptionRegistry.ID_UI_OBJECT_ACTION_REGISTRY)

    val gestureActionRegistry =
        GestureActionRegistry(BootstrapOptionRegistry.ID_GESTURE_ACTION_REGISTRY)

    private val textActionRegistry =
        TextActionRegistry(BootstrapOptionRegistry.ID_TEXT_ACTION_REGISTRY)

    private val appActionRegistry =
        ApplicationActionRegistry(BootstrapOptionRegistry.ID_APP_ACTION_REGISTRY)

    val controlActionRegistry =
        ControlActionRegistry(BootstrapOptionRegistry.ID_CONTROL_ACTION_REGISTRY)

    private val shellCmdActionRegistry =
        ShellCmdActionRegistry(BootstrapOptionRegistry.ID_SHELL_CMD_ACTION_REGISTRY)

    private val fileActionRegistry =
        FileActionRegistry(BootstrapOptionRegistry.ID_FILE_ACTION_REGISTRY)

    val uiObjectFlowRegistry =
        UiObjectFlowRegistry(BootstrapOptionRegistry.ID_UI_OBJECT_FLOW_REGISTRY)

    private val allRegistries = arrayOf(
        /* meta */
        flowRegistry,
        /* criterion */
        eventRegistry,
        applicationRegistry,
        uiObjectRegistry,
        timeRegistry,
        globalInfoRegistry,
        textRegistry,
        networkRegistry,
        /* action */
        controlActionRegistry,
        globalActionRegistry,
        uiObjectActionRegistry,
        gestureActionRegistry,
        textActionRegistry,
        appActionRegistry,
        shellCmdActionRegistry,
        fileActionRegistry,
        /* control */
        uiObjectFlowRegistry,
    )

    fun requireOption(applet: Applet): AppletOption {
        return requireNotNull(findOption(applet)) {
            "Option for applet[$applet] not found!"
        }
    }

    fun findOption(applet: Applet): AppletOption? {
        return requireRegistryById(applet.registryId).findAppletOptionById(applet.appletId)
    }

    override fun createAppletById(id: Int, compatMode: Boolean): Applet {
        val registryId = id ushr 16
        val originId = id and 0xFFFF
        var appletId = originId
        if (compatMode) appletId = AppletFactoryUpdateHelper.checkUpdate(registryId, originId)
        val ret = requireRegistryById(registryId).createAppletFromId(appletId)
        if (originId != appletId) {
            ret.obsoletedId = originId
        }
        return ret
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

