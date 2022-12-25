package top.xjunz.tasker.task.applet.option.registry

import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.criterion.CollectionCriterion
import top.xjunz.tasker.engine.applet.criterion.NumberRangeCriterion
import top.xjunz.tasker.engine.applet.criterion.collectionCriterion
import top.xjunz.tasker.engine.applet.criterion.newNumberRangeCriterion
import top.xjunz.tasker.ktx.array
import top.xjunz.tasker.ktx.format
import top.xjunz.tasker.ktx.str
import top.xjunz.tasker.task.applet.anno.AppletCategory
import top.xjunz.tasker.util.formatTime
import java.util.*

/**
 * @author xjunz 2022/10/01
 */
class TimeOptionRegistry(id: Int) : AppletOptionRegistry(id) {

    private fun timeCollectionCriterion(block: (Calendar) -> Int)
            : CollectionCriterion<Calendar, Int> {
        return collectionCriterion(block)
    }

    @AppletCategory(0x00_00)
    val timeRange = appletOption(0, R.string.time_range) {
        newNumberRangeCriterion<Calendar, Long>{
            it.timeInMillis
        }
    }.withValueDescriber<Collection<Long>> {
        val start = it.firstOrNull()
        val stop = it.lastOrNull()
        when {
            start == null && stop != null -> R.string.format_before.format(stop.formatTime())
            start != null && stop == null -> R.string.format_after.format(start.formatTime())
            start != null && stop != null -> R.string.format_range.format(
                start.formatTime(), stop.formatTime()
            )
            else -> R.string.no_limit.str
        }
    }

    @AppletCategory(0x00_02)
    val month = invertibleAppletOption(0x11, R.string.in_months) {
        timeCollectionCriterion {
            it.get(Calendar.MONTH)
        }
    }.withValueDescriber<Collection<Int>> {
        val arrays = R.array.months.array
        it.joinToString { month ->
            arrays[month]
        }
    }

    @AppletCategory(0x00_03)
    val dayOfMonth = invertibleAppletOption(0x12, R.string.in_day_of_month) {
        NumberRangeCriterion<Calendar, Int> {
            // The first day has value 1
            it.get(Calendar.DAY_OF_MONTH) - 1
        }
    }.withValueDescriber<Collection<Int>> {
        val days = Array<CharSequence>(31) { i ->
            (i + 1).toString()
        }
        it.joinToString { day -> days[day] }
    }

    @AppletCategory(0x00_03)
    val dayOfWeek = invertibleAppletOption(0x13, R.string.in_day_of_week) {
        timeCollectionCriterion {
            it.get(Calendar.DAY_OF_WEEK) - 1
        }
    }.withValueDescriber<Collection<Int>> {
        val arrays = R.array.days_in_week.array
        it.joinToString { day ->
            arrays[day]
        }
    }

    @AppletCategory(0x00_04)
    val hourMinSec = invertibleAppletOption(0x20, R.string.in_hour_min_sec_range) {
        NumberRangeCriterion<Calendar, Int> {
            it.get(Calendar.HOUR) shl 16 or it.get(Calendar.MINUTE) shl 8 or it.get(Calendar.SECOND)
        }
    }.withValueDescriber<Collection<Int>> {
        fun format(time: Int): String {
            return "%02d:%02d:%02d".format(
                time shr 16 and 0xFF, time shr 8 and 0xFF, time and 0xFF
            )
        }

        val start = it.firstOrNull()
        val stop = it.lastOrNull()
        when {
            start == null && stop != null -> R.string.format_before.format(format(stop))
            start != null && stop == null -> R.string.format_after.format(format(start))
            start != null && stop != null -> R.string.format_range.format(
                format(start), format(stop)
            )
            else -> R.string.no_limit.str
        }
    }

    override val categoryNames: IntArray? = null
}