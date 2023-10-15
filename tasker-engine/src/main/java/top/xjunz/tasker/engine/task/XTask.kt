/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.task

import android.os.Parcel
import android.os.Parcelable
import android.os.SystemClock
import android.util.SparseArray
import androidx.annotation.IntRange
import androidx.core.text.parseAsHtml
import kotlinx.coroutines.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import top.xjunz.shared.ktx.md5
import top.xjunz.shared.trace.debugLogcat
import top.xjunz.shared.trace.logcatStackTrace
import top.xjunz.tasker.engine.applet.base.*
import top.xjunz.tasker.engine.applet.factory.AppletFactory
import top.xjunz.tasker.engine.dto.toDTO
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.engine.runtime.ValueRegistry
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * The abstraction of an automation task.
 *
 * **XTask** is the abbr. of "XJUNZ-TASK", rather cool isn't it? :)
 *
 * @author xjunz 2022/07/12
 */
class XTask : ValueRegistry() {

    companion object {

        const val TYPE_RESIDENT = 0
        const val TYPE_ONESHOT = 1
        const val RATE_LIMIT = 100
        const val MAX_SNAPSHOT_COUNT = 10

    }

    /**
     * Whether the task is traversing its [flow].
     */
    val isExecuting get() = currentRuntime?.isActive == true

    /**
     * Whether the task is in suspension state. The suspension can be interrupted.
     */
    val isSuspending get() = currentRuntime?.isSuspending == true

    inline val title get() = metadata.title

    inline val checksum get() = metadata.checksum

    internal val snapshots = ConcurrentLinkedDeque<TaskSnapshot>()

    inline val isOneshot get() = metadata.taskType == TYPE_ONESHOT

    inline val isResident get() = metadata.taskType == TYPE_RESIDENT

    var flow: RootFlow? = null

    var previousArgumentHash = -1

    lateinit var metadata: Metadata

    internal var registry = SparseArray<Any>()

    private var stateListener: TaskStateListener? = null

    private var currentRuntime: TaskRuntime? = null

    var pauseFor: Long = -1

    var pauseStartTime: Long = -1

    private var previousLaunchTime = -1L

    private var onPausedListener: OnTaskPausedStateChangedListener? = null

    private val isPaused: Boolean
        get() {
            if (pauseFor == -1L || pauseStartTime == -1L) {
                return false
            }
            if (System.currentTimeMillis() - pauseStartTime > pauseFor) {
                pauseFor = -1
                pauseStartTime = -1
                onPausedListener?.onTaskPauseStateChanged(checksum)
                return false
            }
            return true
        }

    fun pause(@IntRange(from = 1) duration: Long) {
        check(duration > 0) {
            "The pause duration must be positive!"
        }
        check(!isPaused) {
            "The task is already paused!"
        }
        pauseFor = duration
        pauseStartTime = System.currentTimeMillis()
        onPausedListener?.onTaskPauseStateChanged(checksum)
    }

    fun requireFlow(): RootFlow = requireNotNull(flow) {
        "RootFlow is not initialized!"
    }

    fun setStateListener(listener: TaskStateListener?) {
        stateListener = listener
    }

    fun setOnPausedStateChangedListener(listener: OnTaskPausedStateChangedListener?) {
        onPausedListener = listener
    }

    /**
     * Halt the runtime if running.
     *
     * @param reset Reset the task cache.
     */
    fun halt(reset: Boolean) {
        currentRuntime?.halt()
        if (reset) {
            resetCache()
        }
    }

    private fun resetCache() {
        pauseFor = -1L
        pauseStartTime = -1L
        onPausedListener?.onTaskPauseStateChanged(checksum)
        previousArgumentHash = -1
        previousLaunchTime = -1L
    }

    private inner class SnapshotObserver(private val snapshot: TaskSnapshot) :
        TaskRuntime.Observer {

        private var snapshotAdded: Boolean? = null

        override fun onAppletStarted(victim: Applet, runtime: TaskRuntime) {
            if (victim is Flow) {
                debugLogcat(indent(runtime.tracker.depth) + victim.relationToString() + victim)
            }
            snapshot.current = runtime.tracker.getCurrentHierarchy()
        }

        override fun onAppletTerminated(victim: Applet, runtime: TaskRuntime) {
            val indents = indent(runtime.tracker.depth)
            debugLogcat(indents + victim.relationToString() + "$victim -> ${runtime.isSuccessful}")
            if (!runtime.isSuccessful) {
                if (runtime.result.actual != null) {
                    debugLogcat(indents + "actual: ${runtime.result.actual}")
                }
                if (runtime.result.throwable != null) {
                    debugLogcat(indents + "error: ${runtime.result.throwable}")
                }
            }
            snapshot.current = -1
            if (isResident && snapshotAdded == null && victim is When) {
                if (runtime.isSuccessful) {
                    snapshotAdded = true
                    // As for resident task, only when the event is hit, we offer the snapshot
                    snapshots.offerFirst(snapshot)
                    runtime.snapshot = snapshot
                } else {
                    snapshotAdded = false
                }
            } else if (isOneshot && snapshotAdded != true) {
                snapshotAdded = true
                snapshots.offerFirst(snapshot)
                runtime.snapshot = snapshot
            }
            if (snapshotAdded != false) {
                val hierarchy = runtime.tracker.getCurrentHierarchy()
                if (runtime.isSuccessful) {
                    snapshot.successes.add(hierarchy)
                } else {
                    snapshot.failures.add(
                        TaskSnapshot.Failure.fromAppletResult(hierarchy, runtime.result)
                    )
                }
            }
        }

        override fun onAppletSkipped(victim: Applet, runtime: TaskRuntime) {
            debugLogcat(indent(runtime.tracker.depth) + victim.relationToString() + "$victim -> skipped")
        }

        private fun indent(count: Int): String {
            return Collections.nCopies(count, '-').joinToString("")
        }

        private fun Applet.relationToString(): String {
            if (index == 0) return ""
            return if (isAnd) "And " else if (isOr) "Or " else "Anyway "
        }

    }

    /**
     * Launch the task.
     *
     * @return `true` if the task starts executed and `false` otherwise
     */
    suspend fun launch(runtime: TaskRuntime) {
        if (isPaused) {
            debugLogcat("Current task(${metadata.title}) is paused!")
            return
        }
        val snapshot =
            TaskSnapshot(UUID.randomUUID().toString(), checksum, System.currentTimeMillis())
        try {
            previousLaunchTime = SystemClock.uptimeMillis()
            runtime.observer = SnapshotObserver(snapshot)
            currentRuntime = runtime
            stateListener?.onTaskStarted(runtime)
            runtime.isSuccessful = requireFlow().apply(runtime).isSuccessful
            if (runtime.isSuccessful) {
                stateListener?.onTaskSuccess(runtime)
            } else {
                stateListener?.onTaskFailure(runtime)
            }
        } catch (t: Throwable) {
            runtime.isSuccessful = false
            when (t) {
                is CancellationException -> {
                    stateListener?.onTaskCancelled(runtime)
                    if (isResident) {
                        snapshots.remove(snapshot)
                    }
                }

                is TaskRuntime.StopshipException -> {
                    stateListener?.onTaskCancelled(runtime)
                }

                else -> stateListener?.onTaskError(runtime, t)
            }
            t.logcatStackTrace()
        } finally {
            snapshot.endTimestamp = System.currentTimeMillis()
            snapshot.isSuccessful = runtime.isSuccessful
            snapshot.closeLog()
            snapshots.removeIf {
                it.contentEquals(snapshot)
            }
            if (snapshots.size > MAX_SNAPSHOT_COUNT) {
                snapshots.pollLast()
            }
            currentRuntime = null
            stateListener = null
            clearValues()
            runtime.recycle()
        }
    }

    fun getRuntime(): TaskRuntime? {
        return currentRuntime
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as XTask

        if (checksum != other.checksum) return false

        return true
    }

    override fun hashCode(): Int {
        return checksum.hashCode()
    }

    fun isOverheat(argumentHash: Int): Boolean {
        return argumentHash == previousArgumentHash && previousLaunchTime != -1L && SystemClock.uptimeMillis() - previousLaunchTime <= RATE_LIMIT
    }

    fun interface OnTaskPausedStateChangedListener {
        fun onTaskPauseStateChanged(checksum: Long)
    }

    fun clone(factory: AppletFactory): XTask {
        return toDTO().toXTask(factory, false)
    }

    interface TaskStateListener {

        fun onTaskStarted(runtime: TaskRuntime) {}

        /**
         * When the task completes due to an unexpected error.
         *
         * **Note**: It's the caller's duty to recycle the [runtime].
         */
        fun onTaskError(runtime: TaskRuntime, t: Throwable) {
            onTaskFinished(runtime)
        }

        /**
         * When the flow completes failed.
         *
         * **Note**: It's the caller's duty to recycle the [runtime].
         */
        fun onTaskFailure(runtime: TaskRuntime) {
            onTaskFinished(runtime)
        }

        /**
         * When the task completes successful.
         *
         * **Note**: It's the caller's duty to recycle the [runtime].
         */
        fun onTaskSuccess(runtime: TaskRuntime) {
            onTaskFinished(runtime)
        }

        /**
         * When the task is cancelled.
         */
        fun onTaskCancelled(runtime: TaskRuntime) {
            onTaskFinished(runtime)
        }

        /**
         * When the task is finished, for no matter what.
         */
        fun onTaskFinished(runtime: TaskRuntime) {}
    }

    @Serializable
    data class Metadata(
        @SerialName("ti") var title: String,

        @SerialName("ty") var taskType: Int = TYPE_RESIDENT,

        @SerialName("d") var description: String? = null,

        @SerialName("c") var creationTimestamp: Long = -1,

        @SerialName("m") var modificationTimestamp: Long = -1,

        @SerialName("s") var checksum: Long = -1,

        @SerialName("a") var author: String? = null,

        @SerialName("p") var isPreload: Boolean = false,

        /**
         * Do not set the default version to BuildConfig.VERSION_CODE, because old version
         * task may not have this field. Setting to zero helps us to tell whether it is a
         * legacy task.
         */
        @SerialName("v") var version: Int = 0,
    ) : Parcelable {

        inline val identifier get() = checksum.toString().md5.substring(0, 7)

        val spannedDescription get() = description?.replace("\n", "<br>")?.parseAsHtml()

        constructor(parcel: Parcel) : this(
            parcel.readString()!!,
            parcel.readInt(),
            parcel.readString(),
            parcel.readLong(),
            parcel.readLong(),
            parcel.readLong(),
            parcel.readString(),
            parcel.readByte() != 0.toByte(),
            parcel.readInt()
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(title)
            parcel.writeInt(taskType)
            parcel.writeString(description)
            parcel.writeLong(creationTimestamp)
            parcel.writeLong(modificationTimestamp)
            parcel.writeLong(checksum)
            parcel.writeString(author)
            parcel.writeByte(if (isPreload) 1 else 0)
            parcel.writeInt(version)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<Metadata> {
            override fun createFromParcel(parcel: Parcel): Metadata {
                return Metadata(parcel)
            }

            override fun newArray(size: Int): Array<Metadata?> {
                return arrayOfNulls(size)
            }
        }

    }
}