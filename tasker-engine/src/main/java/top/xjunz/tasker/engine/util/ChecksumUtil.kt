package top.xjunz.tasker.engine.util

import top.xjunz.tasker.engine.applet.dto.AppletDTO
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.engine.task.dto.XTaskDTO
import java.util.zip.CRC32

/**
 * @author xjunz 2022/12/22
 */
object ChecksumUtil {

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