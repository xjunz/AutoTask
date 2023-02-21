/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.task

import android.os.Parcel
import android.os.Parcelable
import android.util.SparseArray
import kotlinx.coroutines.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import top.xjunz.shared.ktx.md5
import top.xjunz.shared.trace.logcat
import top.xjunz.tasker.engine.applet.base.*
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.engine.runtime.Event.Companion.lockAll
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.engine.runtime.TaskRuntime.Companion.obtainRuntime
import top.xjunz.tasker.engine.runtime.ValueRegistry
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * The abstraction of an automator task.
 *
 * **XTask** is the abbr. of "XJUNZ-TASK", rather cool isn't it? :)
 *
 * @author xjunz 2022/07/12
 */
class XTask : ValueRegistry() {

    companion object {
        const val TYPE_RESIDENT = 0
        const val TYPE_ONESHOT = 1
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

    var flow: RootFlow? = null

    var taskStateListener: TaskStateListener? = null

    lateinit var metadata: Metadata

    internal var registry = SparseArray<Any>()

    internal val snapshots = ConcurrentLinkedDeque<TaskSnapshot>()

    private var currentRuntime: TaskRuntime? = null

    fun requireFlow(): RootFlow = requireNotNull(flow) {
        "RootFlow is not initialized!"
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
    suspend fun launch(registry: ValueRegistry, scope: CoroutineScope, events: Array<out Event>) {
        val snapshot = TaskSnapshot(checksum, System.currentTimeMillis())
        val runtime = obtainRuntime(registry, scope, events)
        runtime.observer = SnapshotObserver(snapshot)
        try {
            currentRuntime = runtime
            events.lockAll(runtime)
            taskStateListener?.onStarted(runtime)
            runtime.isSuccessful = requireFlow().apply(runtime).isSuccessful
            if (runtime.isSuccessful) {
                taskStateListener?.onSuccess(runtime)
            } else {
                taskStateListener?.onFailure(runtime)
            }
        } catch (t: Throwable) {
            runtime.isSuccessful = false
            when (t) {
                is FlowFailureException -> taskStateListener?.onFailure(runtime)
                is CancellationException -> {
                    taskStateListener?.onCancelled(runtime)
                    snapshots.remove(snapshot)
                }
                else -> taskStateListener?.onError(runtime, t)
            }
        } finally {
            snapshot.endTimestamp = System.currentTimeMillis()
            snapshot.isSuccessful = runtime.isSuccessful
            snapshots.removeIf {
                it.isRedundantTo(snapshot)
            }
            if (snapshots.size > 10) {
                snapshots.pollLast()
            }
            currentRuntime = null
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

    class FlowFailureException(reason: String) : RuntimeException(reason)

    interface TaskStateListener {

        fun onStarted(runtime: TaskRuntime) {}

        /**
         * When the task completes due to an unexpected error.
         *
         * **Note**: It's the caller's duty to recycle the [runtime].
         */
        fun onError(runtime: TaskRuntime, t: Throwable) {}

        /**
         * When the flow completes failed.
         *
         * **Note**: It's the caller's duty to recycle the [runtime].
         */
        fun onFailure(runtime: TaskRuntime) {}

        /**
         * When the task completes successful.
         *
         * **Note**: It's the caller's duty to recycle the [runtime].
         */
        fun onSuccess(runtime: TaskRuntime) {}

        /**
         * When the task is cancelled.
         */
        fun onCancelled(runtime: TaskRuntime) {}
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