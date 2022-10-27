package top.xjunz.tasker.engine.value

import androidx.annotation.IntDef
import androidx.core.util.Pools

/**
 * You should obtain an instance by [Event.obtain] and [recycle] the instance
 * when it is no longer used.
 *
 * @author xjunz 2022/08/04
 */
class Event private constructor() {

    private object EventPool : Pools.SimplePool<Event>(13)

    @EventType
    var eventType = EVENT_UNDEFINED

    /**
     * The target package to which the event belongs.
     */
    lateinit var targetPackage: String
        private set

    @IntDef(
        EVENT_UNDEFINED, EVENT_ON_PACKAGE_ENTERED, EVENT_ON_PACKAGE_EXITED, EVENT_ON_CONTENT_CHANGED
    )
    annotation class EventType

    companion object {

        /**
         * The undefined event. Does not match any event even itself.
         */
        const val EVENT_UNDEFINED = -1

        /**
         * The event when a new package is entered. Always be accompanied with [EVENT_ON_PACKAGE_EXITED].
         */
        const val EVENT_ON_PACKAGE_ENTERED = 1

        /**
         * The event when the current package is exited. Always be accompanied with [EVENT_ON_PACKAGE_ENTERED].
         */
        const val EVENT_ON_PACKAGE_EXITED = 1 shl 1

        /**
         * The event when any content is changed in current window.
         */
        const val EVENT_ON_CONTENT_CHANGED = 1 shl 2

        fun obtain(@EventType eventType: Int, packageName: String): Event {
            return (EventPool.acquire() ?: Event()).also {
                it.eventType = eventType
                it.targetPackage = packageName
            }
        }
    }

    /**
     * Recycle the event for further usage.
     */
    fun recycle() {
        EventPool.release(this)
    }


}

