/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry

import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.criterion.CollectionCriterion
import top.xjunz.tasker.engine.applet.criterion.CollectionCriterion.Companion.collectionCriterion
import top.xjunz.tasker.engine.applet.criterion.LambdaCriterion
import top.xjunz.tasker.ktx.array
import top.xjunz.tasker.ktx.format
import top.xjunz.tasker.ktx.str
import top.xjunz.tasker.task.applet.anno.AppletOrdinal
import top.xjunz.tasker.task.applet.criterion.NumberRangeCriterion.Companion.numberRangeCriterion
import top.xjunz.tasker.task.applet.util.IntValueUtil
import top.xjunz.tasker.task.applet.value.VariantType
import top.xjunz.tasker.util.formatTime
import java.util.Calendar

/**
 * @author xjunz 2022/10/01
 */
class TimeCriterionRegistry(id: Int) : AppletOptionRegistry(id) {

    private fun timeCollectionCriterion(block: (Calendar) -> Int)
            : CollectionCriterion<Calendar, Int> {
        return collectionCriterion(block)
    }

    @AppletOrdinal(0x00_00)
    val timeRange = invertibleAppletOption(R.string.in_time_range) {
        numberRangeCriterion<Calendar, Long> {
            it.timeInMillis
        }
    }.withValueDescriber<List<Long>> {
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

    @AppletOrdinal(0x00_01)
    val month = invertibleAppletOption(R.string.in_months) {
        timeCollectionCriterion {
            it.get(Calendar.MONTH)
        }
    }.withValueDescriber<List<Int>> {
        val arrays = R.array.months.array
        it.joinToString { month ->
            arrays[month]
        }
    }

    @AppletOrdinal(0x00_02)
    val dayOfMonth = invertibleAppletOption(R.string.in_day_of_month) {
        numberRangeCriterion<Calendar, Int> {
            // The first day has value 1
            it.get(Calendar.DAY_OF_MONTH) - 1
        }
    }.withValueDescriber<Collection<Int>> {
        val days = Array<CharSequence>(31) { i ->
            (i + 1).toString()
        }
        it.joinToString { day -> days[day] }
    }

    @AppletOrdinal(0x00_03)
    val dayOfWeek = invertibleAppletOption(R.string.in_day_of_week) {
        timeCollectionCriterion {
            it.get(Calendar.DAY_OF_WEEK) - 1
        }
    }.withValueDescriber<Collection<Int>> {
        val arrays = R.array.days_in_week.array
        it.joinToString { day ->
            arrays[day]
        }
    }

    private fun formatTimeInDay(time: Int): String {
        return "%02d:%02d:%02d".format(*IntValueUtil.parseTime(time))
    }

    @AppletOrdinal(0x00_04)
    val hourMinSec = invertibleAppletOption(R.string.in_hour_min_sec_range) {
        numberRangeCriterion<Calendar, Int> {
            IntValueUtil.composeTime(
                it.get(Calendar.HOUR_OF_DAY),
                it.get(Calendar.MINUTE),
                it.get(Calendar.SECOND)
            )
        }
    }.withValueDescriber<List<Int>> {
        val start = it.firstOrNull()
        val stop = it.lastOrNull()
        when {
            start == null && stop != null -> R.string.format_before.format(formatTimeInDay(stop))
            start != null && stop == null -> R.string.format_after.format(formatTimeInDay(start))
            start != null && stop != null ->
                if (start != stop) {
                    R.string.format_range.format(formatTimeInDay(start), formatTimeInDay(stop))
                } else {
                    formatTimeInDay(start)
                }

            else -> R.string.no_limit.str
        }
    }

    @AppletOrdinal(0x00_05)
    val isSpecifiedTime = appletOption(R.string.is_specified_time) {
        LambdaCriterion.equalCriterion<Calendar, Long> { calendar ->
            calendar.timeInMillis
        }
    }.withValueArgument<Long>(R.string.specified_time, VariantType.LONG_TIME)
        .withValueDescriber<Long> {
            it.formatTime()
        }


    @AppletOrdinal(0x00_06)
    val isSpecifiedTimeInDay = appletOption(R.string.is_specified_time_in_day) {
        LambdaCriterion.equalCriterion<Calendar, Int> {
            IntValueUtil.composeTime(
                it.get(Calendar.HOUR_OF_DAY),
                it.get(Calendar.MINUTE),
                it.get(Calendar.SECOND)
            )
        }
    }.withValueArgument<Int>(R.string.specified_time, VariantType.INT_TIME_IN_DAY)
        .withValueDescriber<Int> {
            formatTimeInDay(it)
        }
}