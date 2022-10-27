package top.xjunz.tasker.task.applet.option.registry

import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.criterion.CollectionCriterion
import top.xjunz.tasker.engine.applet.criterion.NumberRangeCriterion
import top.xjunz.tasker.engine.applet.serialization.AppletValues
import top.xjunz.tasker.ktx.array
import top.xjunz.tasker.ktx.format
import top.xjunz.tasker.ktx.str
import top.xjunz.tasker.task.applet.anno.AppletCategory
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.task.applet.option.NotInvertibleAppletOption
import top.xjunz.tasker.util.formatTime
import java.util.*

/**
 * @author xjunz 2022/10/01
 */
class TimeOptionRegistry(id: Int) : AppletOptionRegistry(id) {

    private inline fun TimeCollectionCriterion(crossinline block: (Calendar) -> Int): CollectionCriterion<Calendar, Int> {
        return CollectionCriterion(AppletValues.VAL_TYPE_INT, block)
    }

    @AppletCategory(0x00_00)
    val timeRange = NotInvertibleAppletOption(0, R.string.time_range) {
        NumberRangeCriterion<Calendar, Long> {
            it.timeInMillis
        }
    }.withDescriber<Collection<Long>> {
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

    /* @AppletCategory(0x00_01)
     private val year = AppletOption(0x10, R.string.in_year_range) {
         RangeCriterion<Calendar, Int> {
             it.get(Calendar.YEAR)
         }
     }*/

    @AppletCategory(0x00_02)
    val month = AppletOption(0x11, R.string.in_months) {
        TimeCollectionCriterion {
            it.get(Calendar.MONTH)
        }
    }.withDescriber<Collection<Int>> {
        val arrays = R.array.months.array
        it.joinToString { month ->
            arrays[month]
        }
    }

    @AppletCategory(0x00_03)
    val dayOfMonth = AppletOption(0x12, R.string.in_day_of_month) {
        NumberRangeCriterion<Calendar, Int> {
            it.get(Calendar.DAY_OF_MONTH)
        }
    }.withDescriber<Collection<Int>> {
        val days = Array<CharSequence>(31) { i ->
            (i + 1).toString()
        }
        it.joinToString { day -> days[day] }
    }

    @AppletCategory(0x00_03)
    val dayOfWeek = AppletOption(0x13, R.string.in_day_of_week) {
        TimeCollectionCriterion {
            it.get(Calendar.DAY_OF_WEEK)
        }
    }.withDescriber<Collection<Int>> {
        val arrays = R.array.days_in_week.array
        it.joinToString { day ->
            arrays[day]
        }
    }

    @AppletCategory(0x00_04)
    val hourMinSec = AppletOption(0x20, R.string.in_hour_min_sec_range) {
        NumberRangeCriterion<Calendar, Int> {
            it.get(Calendar.HOUR) shl 16 or it.get(Calendar.MINUTE) shl 8 or it.get(Calendar.SECOND)
        }
    }.withDescriber<Collection<Int>> {
        fun format(time: Int): String {
            return "%02d:%02d:%02d".format(
                time shr 16 and 0xFF, time shr 8 and 0xFF, time and 0xFF
            )
        }
        val start = it.firstOrNull()
        val stop = it.lastOrNull()
        when {
            start == null && stop != null -> format(stop)
            start != null && stop == null -> format(start)
            start != null && stop != null -> R.string.format_range.format(
                format(start), format(stop)
            )
            else -> R.string.no_limit.str
        }
    }

    override val title: Int = R.string.current_time_matches

    override val categoryNames: IntArray = intArrayOf(AppletOption.TITLE_NONE)
}