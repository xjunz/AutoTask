package top.xjunz.tasker.task.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import top.xjunz.shared.utils.runtimeException
import top.xjunz.tasker.app
import top.xjunz.tasker.engine.applet.factory.AppletFactory
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.engine.task.dto.XTaskDTO
import top.xjunz.tasker.engine.task.dto.XTaskDTO.Serializer.toDTO
import java.io.File
import java.io.FileFilter
import java.util.zip.ZipInputStream

/**
 * @author xjunz 2022/08/05
 */
object TaskStorage {

    private const val X_TASK_FILE_SUFFIX = ".xtsk"

    var customTaskLoaded = false

    var preloadTaskLoaded = false

    val allTasks = mutableListOf<XTask>()

    val preloadTasks = mutableListOf<XTask>()

    private val storageDir: File = app.getExternalFilesDir("xtsk")!!

    private fun getTaskFileOnStorage(task: XTask, isEnabled: Boolean): File {
        val flag = if (isEnabled) "1" else "0"
        return File(storageDir, task.metadata.identifier + flag + X_TASK_FILE_SUFFIX)
    }

    private val XTask.fileOnStorage: File
        get() {
            return getTaskFileOnStorage(this, isEnabled)
        }

    fun removeTask(task: XTask) {
        check(allTasks.contains(task))
        check(!task.isEnabled)
        val file = task.fileOnStorage
        if (!file.exists() || file.delete()) {
            allTasks.remove(task)
        } else {
            runtimeException("Failed to delete file!")
        }
    }

    fun toggleTaskFilename(task: XTask): Boolean {
        val file = getTaskFileOnStorage(task, !task.isEnabled)
        if (file.exists()) {
            file.renameTo(getTaskFileOnStorage(task, task.isEnabled))
            return true
        }
        return false
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun preloadTasks(factory: AppletFactory) {
        withContext(Dispatchers.IO) {
            ZipInputStream(app.assets.open("prextsks")).use {
                var entry = it.nextEntry
                while (entry != null) {
                    val task = Json.decodeFromStream<XTaskDTO>(it).toXTask(factory)
                    task.metadata.isPreload = true
                    preloadTasks.add(task)
                    entry = it.nextEntry
                }
            }
        }
    }

    suspend fun persistTask(task: XTask) {
        withContext(Dispatchers.IO) {
            val file = task.fileOnStorage
            if (file.parentFile?.exists() == true || file.parentFile?.mkdirs() == true) {
                if (file.exists() || file.createNewFile()) {
                    file.outputStream().bufferedWriter().use {
                        it.write(Json.encodeToString(task.toDTO()))
                    }
                    allTasks.add(task)
                } else {
                    runtimeException("Failed to create new file!")
                }
            } else {
                runtimeException("Failed to mkdirs!")
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun loadAllTasks(factory: AppletFactory) {
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
                        val task = Json.decodeFromStream<XTaskDTO>(it).apply {
                            check(verifyChecksum()) {
                                "Checksum failure to xtsk file $file?!"
                            }
                        }.toXTask(factory)
                        allTasks.add(task)
                        if (isEnabled) {
                            task.enable()
                        }
                    }.onFailure {
                        it.printStackTrace()
                    }
                }
            }
        }
    }

    private fun findTask(checksum: Long): XTask? {
        return allTasks.find { it.checksum == checksum }
    }

    fun getResidentTasks(): List<XTask> {
        return allTasks.filter {
            it.metadata.taskType == XTask.TYPE_RESIDENT
        }
    }

    fun getOneshotTasks(): List<XTask> {
        return allTasks.filter {
            it.metadata.taskType == XTask.TYPE_ONESHOT
        }
    }

    fun getActiveResidentTasks(): List<XTask> {
        return allTasks.filter {
            it.isEnabled
        }
    }
}