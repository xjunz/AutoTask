package top.xjunz.tasker.ui.task.selector.option

import android.content.ComponentName
import android.content.pm.ActivityInfo
import top.xjunz.tasker.app

/**
 * @author xjunz 2022/10/09
 */
class ActivityInfoWrapper(val source: ActivityInfo, private val entranceName: String?) :
    Comparable<ActivityInfoWrapper> {

    val isEntrance get() = entranceName == source.name

    val label: CharSequence by lazy {
        if (source.labelRes == 0) {
            source.name.substringAfterLast('.')
        } else {
            source.loadLabel(app.packageManager)
        }
    }

    val componentName by lazy {
        ComponentName(source.packageName, source.name)
    }

    private val hasLabel get() = source.labelRes != 0

    private val nonAscii by lazy {
        if (!hasLabel) return@lazy 0
        label.count {
            it !in '0'..'z'
        }
    }

    override fun compareTo(other: ActivityInfoWrapper): Int {
        var c = -isEntrance.compareTo(other.isEntrance)
        if (c == 0) {
            c = -hasLabel.compareTo(hasLabel)
        }
        if (c == 0) {
            c =
                -((nonAscii / label.length.toFloat()).compareTo(other.nonAscii / other.label.length.toFloat()))
        }
        if (c == 0) {
            c = label.toString().compareTo(other.label.toString())
        }
        return c
    }
}