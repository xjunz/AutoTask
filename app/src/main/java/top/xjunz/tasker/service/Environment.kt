/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.service

import android.app.UiAutomation
import top.xjunz.tasker.annotation.Local
import top.xjunz.tasker.isAppProcess
import top.xjunz.tasker.ktx.isTrue
import top.xjunz.tasker.premium.PremiumMixin
import top.xjunz.tasker.uiautomator.CoroutineUiDevice

/**
 * @author xjunz 2022/10/10
 */
inline val serviceController get() = OperatingMode.CURRENT.serviceController

/**
 * Service in each process, may be a remote delegate if in the app process.
 */
@Local
inline val currentService: AutomatorService
    get() = if (isAppProcess) serviceController.requireService() else ShizukuAutomatorService.require()

inline val isFloatingInspectorShown get() = A11yAutomatorService.get()?.isInspectorShown == true

inline val a11yAutomatorService get() = A11yAutomatorService.require()

inline val floatingInspector get() = a11yAutomatorService.inspector

inline val uiAutomatorBridge get() = currentService.uiAutomatorBridge

inline val uiAutomation: UiAutomation get() = uiAutomatorBridge.uiAutomation

inline val uiDevice: CoroutineUiDevice get() = uiAutomatorBridge.uiDevice

inline val premiumContext get() = PremiumMixin.premiumContext

inline val isPremium get() = PremiumMixin.premiumStatusLiveData.isTrue

fun ensurePremium() {
    PremiumMixin.ensurePremium()
}