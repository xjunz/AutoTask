/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry

import androidx.core.text.buildSpannedString
import top.xjunz.tasker.R
import top.xjunz.tasker.ktx.foreColored
import top.xjunz.tasker.ktx.text
import top.xjunz.tasker.task.applet.action.FileAction
import top.xjunz.tasker.task.applet.anno.AppletOrdinal
import top.xjunz.tasker.task.applet.value.VariantArgType

/**
 * @author xjunz 2023/09/21
 */
class FileActionRegistry(id: Int) : AppletOptionRegistry(id) {

    @AppletOrdinal(0x0001)
    val deleteFile = appletOption(R.string.format_delete) {
        FileAction(FileAction.ACTION_DELETE)
    }.withUnaryArgument<String>(
        R.string.file_path, R.string.file, variantValueType = VariantArgType.TEXT_FILE_PATH
    ).thisArgAsResult()
        .shizukuOnly()
        .hasCompositeTitle()

    @AppletOrdinal(0x0002)
    val copyFile = appletOption(R.string.format_copy_file) {
        FileAction(FileAction.ACTION_COPY)
    }.withUnaryArgument<String>(
        R.string.source_file_path,
        R.string.file,
        VariantArgType.TEXT_FILE_PATH
    ).thisArgAsResult().withUnaryArgument<String>(
        R.string.destination_file_path,
        R.string.specified_destination,
        VariantArgType.TEXT_FILE_PATH
    ).thisArgAsResult().withValuesDescriber { _, map ->
        buildSpannedString {
            val src = map[0] as? String
            if (src != null) {
                append(R.string.source_file_path.text)
                append(": ")
                append(src.foreColored())
            }
            val des = map[1] as? String
            if (des != null) {
                if (src != null) {
                    append('\n')
                }
                append(R.string.destination_file_path.text)
                append(": ")
                append(des.foreColored())
            }
        }
    }.withHelperText(R.string.tip_copy_file)
        .hasCompositeTitle()
        .shizukuOnly()

    @AppletOrdinal(0x0003)
    val moveFile = appletOption(R.string.format_move_file) {
        FileAction(FileAction.ACTION_RENAME)
    }.withUnaryArgument<String>(
        R.string.source_file_path,
        R.string.file,
        VariantArgType.TEXT_FILE_PATH
    ).thisArgAsResult().withUnaryArgument<String>(
        R.string.destination_file_path,
        R.string.specified_destination,
        VariantArgType.TEXT_FILE_PATH
    ).thisArgAsResult().withValuesDescriber { _, map ->
        buildSpannedString {
            val src = map[0] as? String
            if (src != null) {
                append(R.string.source_file_path.text)
                append(": ")
                append(src.foreColored())
            }
            val des = map[1] as? String
            if (des != null) {
                if (src != null) {
                    append('\n')
                }
                append(R.string.destination_file_path.text)
                append(": ")
                append(des.foreColored())
            }
        }
    }.withHelperText(R.string.tip_move_file)
        .hasCompositeTitle()
        .shizukuOnly()
}