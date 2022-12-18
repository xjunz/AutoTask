package top.xjunz.tasker.task

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.invoke
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import top.xjunz.shared.ktx.md5
import top.xjunz.tasker.engine.applet.factory.AppletFactory
import top.xjunz.tasker.engine.task.XTask
import top.xjunz.tasker.engine.task.dto.XTaskDTO
import top.xjunz.tasker.engine.task.dto.XTaskDTO.Serializer.toDTO
import java.io.File
import java.io.FileFilter

/**
 * @author xjunz 2022/08/05
 */
class TaskStorage(storageDirPath: String) {

    companion object {
        private const val X_TASK_FILE_SUFFIX = ".xtsk"
    }

    private val allTasks = mutableSetOf<XTask>()

    private val storageDir: File = File(storageDirPath)

    private val XTask.fileOnStorage: File
        get() {
            val flag = if (isEnabled) "1" else "0"
            return File(storageDir, metadata.checksum.toString().md5.substring(0, 7) + flag)
        }

    fun removeTask(task: XTask): Boolean {
        val file = task.fileOnStorage
        return file.exists() && file.delete()
    }

    suspend fun persistTask(task: XTask): Boolean {
        return withContext(Dispatchers.IO) {
            val dto = task.toDTO()
            val file = task.fileOnStorage
            if (file.exists() || file.createNewFile()) {
                file.outputStream().bufferedWriter().use {
                    it.write(Json.encodeToString(dto))
                }
                allTasks.add(task)
                return@withContext true
            }
            return@withContext false
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun loadAllTasks(factory: AppletFactory) {
        if (!storageDir.isDirectory) return
        if (!storageDir.exists()) return
        val uppercaseSuffix = X_TASK_FILE_SUFFIX.uppercase()
        Dispatchers.IO.invoke {
            storageDir.listFiles(
                FileFilter filter@{
                    if (it.name.length == 13
                        && (it.name.endsWith(X_TASK_FILE_SUFFIX) || it.name.endsWith(uppercaseSuffix))
                    ) {
                        val flag = it.name[7].digitToIntOrNull()
                        return@filter flag == 0 || flag == 1
                    }
                    return@filter false
                }
            )?.forEach { file ->
                file.inputStream().use {
                    runCatching {
                        val task = Json.decodeFromStream<XTaskDTO>(it).apply {
                            check(verifyChecksum()) {
                                "Checksum failure to xtsk file $file?!"
                            }
                        }.toAutomatorTask(factory)
                        if (file.name[7].digitToInt() == 1) {
                            TaskManager.addNewEnabledResidentTask(task)
                        }
                        allTasks.add(task)
                    }.onFailure {
                        it.printStackTrace()
                    }
                }
            }
        }
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