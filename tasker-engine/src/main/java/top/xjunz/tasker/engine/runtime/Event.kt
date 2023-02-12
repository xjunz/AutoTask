/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.runtime

import android.util.SparseArray
import androidx.annotation.IntDef
import top.xjunz.shared.ktx.casted

/**
 * @author xjunz 2022/10/30
 */
class Event(
    val type: Int,
    pkgName: String,
    actName: String? = null,
    paneTitle: String? = null
) {

    val componentInfo = ComponentInfo().also {
        it.packageName = pkgName
        it.activityName = actName
        it.paneTitle = paneTitle
    }

    private val extras = SparseArray<Any>()

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
        const val EVENT_ON_PACKAGE_EXITED = 2

        /**
         * The event when any content is changed in current window.
         */
        const val EVENT_ON_CONTENT_CHANGED = 3

        /**
         * When a status bar notification is received.
         */
        const val EVENT_ON_NOTIFICATION_RECEIVED = 4

        const val EVENT_ON_NEW_WINDOW = 5
    }

    fun <V> getExtra(key: Int): V {
        return extras[key].casted()
    }

    @IntDef(
        EVENT_UNDEFINED,
        EVENT_ON_PACKAGE_ENTERED,
        EVENT_ON_PACKAGE_EXITED,
        EVENT_ON_CONTENT_CHANGED,
        EVENT_ON_NOTIFICATION_RECEIVED,
        EVENT_ON_NEW_WINDOW
    )
    annotation class EventType

    override fun toString(): String {
        val typeName = when (type) {
            EVENT_ON_CONTENT_CHANGED -> "contentChanged"
            EVENT_ON_PACKAGE_ENTERED -> "pkgEntered"
            EVENT_ON_PACKAGE_EXITED -> "pkgExited"
            EVENT_ON_NOTIFICATION_RECEIVED -> "notificationReceived"
            EVENT_ON_NEW_WINDOW -> "newWindow"
            else -> "undefined"
        }
        return "Event(type=$typeName, compInfo=$componentInfo)"
    }

    fun putExtra(key: Int, value: Any) {
        extras.put(key, value)
    }


}