package top.xjunz.tasker.service

import top.xjunz.tasker.isInHostProcess

/**
 * @author xjunz 2022/10/10
 */
val serviceController get() = OperatingMode.CURRENT.serviceController

val currentService: AutomatorService
    get() = if (isInHostProcess) {
        serviceController.requireService()
    } else {
        ShizukuAutomatorService.require()
    }
