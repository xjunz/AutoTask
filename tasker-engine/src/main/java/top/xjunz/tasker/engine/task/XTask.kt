/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.task

import android.os.Parcel
import android.os.Parcelable
import android.os.SystemClock
import android.util.SparseArray
import kotlinx.coroutines.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import top.xjunz.shared.ktx.md5
import top.xjunz.shared.trace.logcat
import top.xjunz.shared.trace.logcatStackTrace
import top.xjunz.tasker.engine.applet.base.*
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

    inline val isPreload get() = metadata.isPreload

    internal val snapshots = ConcurrentLinkedDeque<TaskSnapshot>()

    var flow: RootFlow? = null

    var previousLaunchTime = -1L

    var previousArgumentHash = -1

    lateinit var metadata: Metadata

    internal var registry = SparseArray<Any>()

    private var listener: TaskStateListener? = null

    private var currentRuntime: TaskRuntime? = null

    fun requireFlow(): RootFlow = requireNotNull(flow) {
        "RootFlow is not initialized!"
    }

    fun setListener(listener: TaskStateListener?) {
        this.listener = listener
    }

    /**
     * Halt the runtime if running.
     */
    fun halt() {
        currentRuntime?.halt()
    }

    private inner class SnapshotObserver(private val snapshot: TaskSnapshot) :
        TaskRuntime.Observer {

        private var eventHit: Boolean? = null

        override fun onAppletStarted(victim: Applet, runtime: TaskRuntime) {
            if (victim is Flow) {
                logcat(indent(runtime.tracker.depth) + victim.relationToString() + victim)
            }
            snapshot.current = runtime.tracker.getCurrentHierarchy()
        }

        override fun onAppletTerminated(victim: Applet, runtime: TaskRuntime) {
            val indents = indent(runtime.tracker.depth)
            logcat(indents + victim.relationToString() + "$victim -> ${runtime.isSuccessful}")
            if (!runtime.isSuccessful) {
                if (runtime.result.actual != null) {
                    logcat(indents + "actual: ${runtime.result.actual}")
                }
                if (runtime.result.throwable != null) {
                    logcat(indents + "error: ${runtime.result.throwable}")
                }
            }
            snapshot.current = -1
            if (eventHit == null && victim is When) {
                if (runtime.isSuccessful) {
                    eventHit = true
                    // Only when the event is hit, we offer the snapshot
                    snapshots.offerFirst(snapshot)
                } else {
                    eventHit = false
                }
            }
            if (eventHit != false) {
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
            logcat(indent(runtime.tracker.depth) + victim.relationToString() + "$victim -> skipped")
        }

        private fun indent(count: Int): String {
            return Collections.nCopies(count, '-').joinToString("")
        }

        private fun Applet.relationToString(): String {
            if (index == 0) return ""
            return if (isAnd) "And " else if (isOr) "Or " else "Anyway"
        }

    }

    /**
     * Launch the task.
     *
     * @return `true` if the task starts executed and `false` otherwise
     */
    suspend fun launch(runtime: TaskRuntime) {
        val snapshot = TaskSnapshot(checksum, System.currentTimeMillis())
        try {
            previousLaunchTime = SystemClock.uptimeMillis()
            runtime.observer = SnapshotObserver(snapshot)
            currentRuntime = runtime
            listener?.onTaskStarted(runtime)
            runtime.isSuccessful = requireFlow().apply(runtime).isSuccessful
            if (runtime.isSuccessful) {
                listener?.onTaskSuccess(runtime)
            } else {
                listener?.onTaskFailure(runtime)
            }
        } catch (t: Throwable) {
            runtime.isSuccessful = false
            when (t) {
                is CancellationException -> {
                    listener?.onTaskCancelled(runtime)
                    snapshots.remove(snapshot)
                }
                else -> listener?.onTaskError(runtime, t)
            }
            t.logcatStackTrace()
        } finally {
            snapshot.endTimestamp = System.currentTimeMillis()
            snapshot.isSuccessful = runtime.isSuccessful
            snapshots.removeIf {
                it.contentEquals(snapshot)
            }
            if (snapshots.size > MAX_SNAPSHOT_COUNT) {
                snapshots.pollLast()
            }
            currentRuntime = null
            listener = null
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
        return argumentHash == previousArgumentHash && previousLaunchTime != -1L
                && SystemClock.uptimeMillis() - previousLaunchTime <= RATE_LIMIT
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

        @SerialName("p") var isPreload: Boolean = false
    ) : Parcelable {

        inline val identifier get() = checksum.toString().md5.substring(0, 7)

        constructor(parcel: Parcel) : this(
            parcel.readString()!!,
            parcel.readInt(),
            parcel.readString(),
            parcel.readLong(),
            parcel.readLong(),
            parcel.readLong(),
            parcel.readString(),
            parcel.readByte() != 0.toByte()
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