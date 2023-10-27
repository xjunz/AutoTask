/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.decodeFromStream
import top.xjunz.shared.trace.logcatStackTrace
import top.xjunz.shared.utils.runtimeException
import top.xjunz.tasker.BuildConfig
import top.xjunz.tasker.app
import top.xjunz.tasker.engine.applet.factory.AppletFactory
import top.xjunz.tasker.engine.dto.AppletChecksum.calculateChecksum
import top.xjunz.tasker.engine.dto.XTaskDTO
import top.xjunz.tasker.engine.dto.XTaskJson
import top.xjunz.tasker.engine.dto.toDTO
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.task.applet.option.AppletOptionFactory
import top.xjunz.tasker.task.runtime.LocalTaskManager
import top.xjunz.tasker.task.runtime.LocalTaskManager.isEnabled
import java.io.File
import java.io.FileFilter
import java.util.zip.ZipInputStream

/**
 * @author xjunz 2022/08/05
 */
object TaskStorage {

    const val X_TASK_FILE_SUFFIX = ".xtsk"
    const val X_TASK_FILE_ARCHIVE_SUFFIX = ".xtsks"

    var storageTaskLoaded = false

    var presetTaskLoaded = false
        private set

    var exampleTaskLoaded = false
        private set

    private val all = mutableListOf<XTask>()

    fun getAllTasks(): List<XTask> {
        return all
    }

    private val presets = mutableListOf<XTask>()

    private val examples = mutableListOf<XTask>()

    fun getPresetTasks(): List<XTask> {
        return presets
    }

    fun getExampleTasks(): List<XTask> {
        return examples
    }

    private val storageDir: File = app.getExternalFilesDir("xtsk")!!

    private fun getTaskFileOnStorage(metadata: XTask.Metadata, isEnabled: Boolean): File {
        val flag = if (isEnabled) "1" else "0"
        return File(storageDir, metadata.identifier + flag + X_TASK_FILE_SUFFIX)
    }

    fun XTask.getFileName(isEnabled: Boolean): String {
        return getTaskFileOnStorage(metadata, isEnabled).name
    }

    val XTask.fileOnStorage: File
        get() {
            return getTaskFileOnStorage(metadata, isEnabled)
        }

    fun removeTask(task: XTask) {
        check(all.contains(task))
        val file = task.fileOnStorage
        if (!file.exists() || file.delete()) {
            all.remove(task)
        } else {
            runtimeException("Failed to delete file!")
        }
    }

    fun toggleTaskFilename(task: XTask): Boolean {
        val file = getTaskFileOnStorage(task.metadata, !task.isEnabled)
        if (file.exists()) {
            file.renameTo(getTaskFileOnStorage(task.metadata, task.isEnabled))
            return true
        }
        return false
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun loadAssetTasks(
        factory: AppletFactory,
        assetName: String,
        into: MutableList<XTask>
    ) {
        withContext(Dispatchers.IO) {
            ZipInputStream(app.assets.open(assetName)).use {
                var entry = it.nextEntry
                while (entry != null) {
                    val dto = XTaskJson.decodeFromStream<XTaskDTO>(it)
                    val prevVC16 = dto.metadata.version < 16
                    check(dto.verifyChecksum()) {
                        "Checksum mismatch for xtsk ${dto.metadata.title}"
                    }
                    val task: XTask
                    if (prevVC16) {
                        task = dto.toXTaskPrevVersionCode16(factory)
                        task.updatePrevVersion16()
                    } else {
                        task = dto.toXTask(factory, false)
                    }
                    task.metadata.isPreload = true
                    into.add(task)
                    entry = it.nextEntry
                }
            }
            into.sortBy { it.title }
        }
    }

    suspend fun loadPresetTasks(factory: AppletFactory) {
        loadAssetTasks(factory, "presets.xtsks", presets)
        presetTaskLoaded = true
    }

    suspend fun loadExampleTasks(factory: AppletFactory) {
        loadAssetTasks(factory, "examples.xtsks", examples)
        exampleTaskLoaded = true
    }

    private suspend fun persistTask(dto: XTaskDTO, isEnabled: Boolean) {
        withContext(Dispatchers.IO) {
            val file = getTaskFileOnStorage(dto.metadata, isEnabled)
            if (file.parentFile?.exists() == true || file.parentFile?.mkdirs() == true) {
                if (file.exists() || file.createNewFile()) {
                    file.outputStream().bufferedWriter().use {
                        it.write(XTaskJson.encodeToString(dto))
                    }
                } else {
                    runtimeException("Failed to create new file!")
                }
            } else {
                runtimeException("Failed to mkdirs!")
            }
        }
    }

    fun addTask(task: XTask) {
        all.add(task)
    }

    suspend fun persistTask(task: XTask) {
        persistTask(task.toDTO(), task.isEnabled)
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun loadAllTasks() {
        if (!storageDir.isDirectory) return
        if (!storageDir.exists()) return
        val uppercaseSuffix = X_TASK_FILE_SUFFIX.uppercase()
        withContext(Dispatchers.IO) {
            storageDir.listFiles(
                FileFilter {
                    it.name.length == 13 && (it.name.endsWith(X_TASK_FILE_SUFFIX)
                            || it.name.endsWith(uppercaseSuffix))
                }
            )?.forEach { file ->
                val flag = file.name[7].digitToIntOrNull()
                val isEnabled = if (flag == 0) false else if (flag == 1) true else return@forEach
                file.inputStream().use {
                    runCatching {
                        val dto = XTaskJson.decodeFromStream<XTaskDTO>(it)
                        val prevVC16 = dto.metadata.version < 16
                        check(dto.verifyChecksum()) {
                            "Checksum mismatch for xtsk file ${file.path}(${dto.metadata.title})!"
                        }
                        val task: XTask
                        if (prevVC16) {
                            task = dto.toXTaskPrevVersionCode16(AppletOptionFactory)
                            // Mark outdated task files with '.pv16' suffix
                            file.renameTo(File(file.parentFile, file.name + ".pv16"))
                            // Persist the updated task file
                            persistTask(task.updatePrevVersion16(), isEnabled)
                        } else {
                            task = dto.toXTask(AppletOptionFactory, true)
                        }
                        addTask(task)
                        if (isEnabled) {
                            LocalTaskManager.enableResidentTask(task)
                        }
                    }.onFailure {
                        //file.renameTo(File(file.parentFile, file.name + ".brk"))
                        it.logcatStackTrace()
                    }
                }
            }
            storageTaskLoaded = true
            LocalTaskManager.isInitialized = true
        }
    }

    /**
     * Update the [XTask.Metadata.version] and [XTask.Metadata.checksum] for pv16 tasks.
     */
    private fun XTask.updatePrevVersion16(): XTaskDTO {
        metadata.version = BuildConfig.VERSION_CODE
        val dto = toDTO()
        val newChecksum = dto.calculateChecksum()
        dto.metadata.checksum = newChecksum
        metadata.checksum = newChecksum
        return dto
    }

    fun getResidentTasks(): List<XTask> {
        return all.filter {
            it.isResident
        }
    }

    fun getOneshotTasks(): List<XTask> {
        return all.filter {
            it.isOneshot
        }
    }
}