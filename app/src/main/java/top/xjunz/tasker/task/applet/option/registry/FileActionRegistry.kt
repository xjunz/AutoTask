/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry

import androidx.core.text.buildSpannedString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.action.doubleArgsAction
import top.xjunz.tasker.engine.applet.action.singleNonNullArgAction
import top.xjunz.tasker.ktx.foreColored
import top.xjunz.tasker.ktx.text
import top.xjunz.tasker.task.applet.anno.AppletOrdinal
import top.xjunz.tasker.task.applet.value.VariantArgType
import java.io.File

/**
 * @author xjunz 2023/09/21
 */
class FileActionRegistry(id: Int) : AppletOptionRegistry(id) {

    @AppletOrdinal(0x0001)
    val deleteFile = appletOption(R.string.delete_file) {
        singleNonNullArgAction<String> {
            File(it).deleteRecursively()
        }
    }.withUnaryArgument<String>(R.string.file_path, variantType = VariantArgType.TEXT_FILE_PATH)
        .shizukuOnly()

    @AppletOrdinal(0x0002)
    val copyFile = appletOption(R.string.format_copy_file) {
        doubleArgsAction<String, String> { arg1, arg2, _ ->
            requireNotNull(arg1)
            requireNotNull(arg2)
            withContext(Dispatchers.IO) {
                File(arg1).copyRecursively(File(arg2))
            }
            true
        }
    }.withUnaryArgument<String>(
        R.string.source_file_path,
        R.string.file,
        VariantArgType.TEXT_FILE_PATH
    ).withUnaryArgument<String>(
        R.string.destination_file_path,
        R.string.specified_destination,
        VariantArgType.TEXT_FILE_PATH
    ).withValuesDescriber { _, map ->
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
        doubleArgsAction<String, String> { arg1, arg2, _ ->
            requireNotNull(arg1)
            requireNotNull(arg2)
            withContext(Dispatchers.IO) {
                val destFile = File(arg2)
                var dest = File(arg2)
                if (destFile.exists() && destFile.isDirectory) {
                    dest = File(destFile, File(arg1).name)
                }
                File(arg1).renameTo(dest)
            }
            true
        }
    }.withUnaryArgument<String>(
        R.string.source_file_path,
        R.string.file,
        VariantArgType.TEXT_FILE_PATH
    ).withUnaryArgument<String>(
        R.string.destination_file_path,
        R.string.specified_destination,
        VariantArgType.TEXT_FILE_PATH
    ).withValuesDescriber { _, map ->
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