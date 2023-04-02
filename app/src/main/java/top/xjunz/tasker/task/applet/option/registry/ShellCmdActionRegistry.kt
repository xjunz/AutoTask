/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry

import top.xjunz.tasker.R
import top.xjunz.tasker.task.applet.action.ShellCmdAction
import top.xjunz.tasker.task.applet.anno.AppletOrdinal
import top.xjunz.tasker.task.applet.value.VariantType

/**
 * @author xjunz 2023/04/03
 */
class ShellCmdActionRegistry(id: Int) : AppletOptionRegistry(id) {

    @AppletOrdinal(0x0001)
    val executeShellCmd = appletOption(R.string.execute_shell_cmd) {
        ShellCmdAction(false)
    }.withValueArgument<String>(R.string.shell_cmd)
        .shizukuOnly()
        .premiumOnly()

    @AppletOrdinal(0x0002)
    val executeShFile = appletOption(R.string.execute_sh_file) {
        ShellCmdAction(true)
    }.withValueArgument<String>(R.string.file_path, VariantType.TEXT_FILE_PATH)
        .shizukuOnly()
        .premiumOnly()
}