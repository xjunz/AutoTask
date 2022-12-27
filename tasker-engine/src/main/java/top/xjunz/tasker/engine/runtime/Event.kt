package top.xjunz.tasker.engine.runtime

import androidx.annotation.IntDef
import androidx.core.util.Pools.SynchronizedPool

/**
 * @author xjunz 2022/10/30
 */
class Event private constructor() {

    var type: Int = EVENT_UNDEFINED

    val componentInfo = ComponentInfo()

    private object Pool : SynchronizedPool<Event>(5)

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
         * The event when the current package is left. Always be accompanied with [EVENT_ON_PACKAGE_ENTERED].
         */
        const val EVENT_ON_PACKAGE_EXITED = 1 shl 1

        /**
         * The event when any content is changed in current window.
         */
        const val EVENT_ON_CONTENT_CHANGED = 1 shl 2

        /**
         * When a status bar notification is received.
         */
        const val EVENT_ON_NOTIFICATION_RECEIVED = 1 shl 3

        fun obtain(
            t: Int,
            pkgName: String,
            actName: String? = null,
            paneTitle: String? = null
        ): Event {
            return (Pool.acquire() ?: Event()).apply {
                type = t
                componentInfo.pkgName = pkgName
                componentInfo.actName = actName
                componentInfo.paneTitle = paneTitle
            }
        }

        internal fun drainPool() {
            while (Pool.acquire() != null) {
                /* no-op */
            }
        }
    }

    @IntDef(
        EVENT_UNDEFINED, EVENT_ON_PACKAGE_ENTERED, EVENT_ON_PACKAGE_EXITED, EVENT_ON_CONTENT_CHANGED
    )
    annotation class EventType

    fun recycle() {
        Pool.release(this)
    }

    override fun toString(): String {
        val typeName = when (type) {
            EVENT_ON_CONTENT_CHANGED -> "contentChanged"
            EVENT_ON_PACKAGE_ENTERED -> "pkgEntered"
            EVENT_ON_PACKAGE_EXITED -> "pkgExited"
            else -> "undefined"
        }
        return "Event(type=$typeName, compInfo=$componentInfo)"
    }


}