/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.runtime

import android.util.ArrayMap
import androidx.core.util.Pools.SimplePool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.task.XTask
import java.util.zip.CRC32


private typealias Referred = () -> Any?

/**
 * The structure storing runtime information of a running [XTask].
 *
 * @author xjunz 2022/08/09
 */
class TaskRuntime private constructor() {

    private object Pool : SimplePool<TaskRuntime>(25)

    interface Observer {

        fun onAppletStarted(victim: Applet, runtime: TaskRuntime) {}

        fun onAppletTerminated(victim: Applet, runtime: TaskRuntime) {}

        fun onAppletSkipped(victim: Applet, runtime: TaskRuntime) {}
    }

    companion object {

        fun XTask.obtainRuntime(
            eventScope: EventScope,
            coroutineScope: CoroutineScope,
            events: Array<out Event>
        ): TaskRuntime {
            val instance = Pool.acquire() ?: TaskRuntime()
            instance.task = this
            instance.runtimeScope = coroutineScope
            instance.target = events
            instance._events = events
            instance._eventScope = eventScope
            return instance
        }

        fun drainPool() {
            while (Pool.acquire() != null) {
                /* no-op */
            }
            Event.drainPool()
        }
    }

    val isActive get() = runtimeScope?.isActive == true

    var isSuspending = false

    lateinit var task: XTask

    private var _eventScope: EventScope? = null

    private val eventScope: EventScope get() = _eventScope!!

    private var _events: Array<out Event>? = null

    val events: Array<out Event> get() = _events!!

    private var runtimeScope: CoroutineScope? = null

    /**
     * Target is for applets in a specific flow to use in runtime.
     */
    private var target: Any? = null

    val tracker = AppletIndexer()

    var observer: Observer? = null

    lateinit var hitEvent: Event

    private var _result: AppletResult? = null

    val result: AppletResult get() = _result!!

    private val crc = CRC32()

    /**
     * Identify all arguments applied to the task runtime.
     */
    val fingerprint: Long get() = crc.value

    fun updateFingerprint(any: Any?) {
        crc.update(any.hashCode())
    }

    fun ensureActive() {
        runtimeScope?.ensureActive()
    }

    fun halt() {
        runtimeScope?.cancel()
    }

    /**
     * Get or put a global variable if absent. The variable can be shared across tasks. More specific,
     * within an [EventScope].
     */
    fun <V : Any> getGlobal(key: Long, initializer: (() -> V)? = null): V {
        if (initializer == null) {
            return eventScope.registry.getValue(key).casted()
        }
        return eventScope.registry.getOrPut(key, initializer).casted()
    }

    private val referents = ArrayMap<String, Any?>()

    lateinit var currentApplet: Applet

    lateinit var currentFlow: Flow

    /**
     * Whether the applying of current applet is successful.
     */
    var isSuccessful = true

    fun getArgument(applet: Applet, which: Int): Any? {
        val name = applet.references[which]
        if (name != null) {
            return getReferentByName(name)
        }
        return null
    }

    /**
     * Get all arguments from registered results, which were registered by [registerResult].
     */
    fun getArguments(applet: Applet): Array<Any?> {
        return if (applet.references.isEmpty()) {
            emptyArray()
        } else {
            Array(applet.references.size) {
                getReferentByName(applet.references[it])
            }
        }
    }

    /**
     * Register all results of an applet.
     */
    fun registerResult(applet: Applet, result: AppletResult) {
        _result = result
        if (result.isSuccessful && result.returns != null) {
            registerAllReferents(applet, *result.returns!!)
        }
    }

    fun registerAllReferents(applet: Applet, vararg values: Any?) {
        applet.referents.forEach { (which, name) ->
            referents[name] = values[which]
        }
    }

    fun registerReferent(applet: Applet, which: Int, referred: Referred) {
        referents[applet.referents[which]] = referred
    }

    private fun getReferentByName(name: String?): Any? {
        if (name == null) return null
        val referent = referents[name]
        return if (referent is Referred) referent.invoke() else referent
    }

    fun setTarget(any: Any?) {
        target = any
    }

    fun getRawTarget(): Any? {
        return target
    }

    fun <T> getTarget(): T {
        return requireNotNull(target) {
            "Target is not set!"
        }.casted()
    }

    fun recycle() {
        _eventScope = null
        _events = null
        runtimeScope = null
        referents.clear()
        tracker.reset()
        isSuspending = false
        observer = null
        _result = null
        target = null
        isSuccessful = true
        crc.reset()
        Pool.release(this)
    }

    override fun toString(): String {
        return "TaskRuntime@${hashCode().toString(16)}(${task.title})"
    }
}