/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.dto

import top.xjunz.tasker.engine.task.XTask
import java.util.zip.CRC32

/**
 * @author xjunz 2022/12/22
 */
object AppletChecksum {

    fun calculateChecksum(appletDto: AppletDTO, metadata: XTask.Metadata): Long {
        val crc32 = CRC32()
        appletDto.calculateChecksum(crc32)
        crc32.update(metadata.title.toByteArray())
        metadata.description?.let {
            crc32.update(it.toByteArray())
        }
        crc32.update(metadata.taskType)
        return crc32.value
    }

    fun XTaskDTO.calculateChecksum(): Long {
        return calculateChecksum(flow, metadata)
    }
}