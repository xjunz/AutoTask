/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry

import androidx.annotation.StringRes
import top.xjunz.shared.utils.illegalArgument
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.criterion.collectionCriterion
import top.xjunz.tasker.engine.applet.criterion.equalCriterion
import top.xjunz.tasker.ktx.array
import top.xjunz.tasker.ktx.format
import top.xjunz.tasker.ktx.str
import top.xjunz.tasker.task.applet.anno.AppletOrdinal
import top.xjunz.tasker.task.applet.criterion.NumberRangeCriterion.Companion.numberRangeCriterion
import top.xjunz.tasker.task.applet.util.IntValueUtil
import top.xjunz.tasker.task.applet.value.VariantArgType
import top.xjunz.tasker.util.formatTime
import java.util.Calendar

/**
 * @author xjunz 2022/10/01
 */
class TimeCriterionRegistry(id: Int) : AppletOptionRegistry(id) {

    private fun timeCollectionCriterion(block: (Calendar) -> Int): Applet {
        return collectionCriterion(block)
    }

    @AppletOrdinal(0x00_00)
    val timeRange = invertibleAppletOption(R.string.in_time_range) {
        numberRangeCriterion<Calendar, Long> {
            it.timeInMillis
        }
    }.withValueArgument<Long>(-1, VariantArgType.LONG_TIME, true)
        .withSingleValueDescriber<List<Long>> {
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
    }.withValueArgument<Int>(-1, VariantArgType.INT_MONTH, true)
        .withSingleValueDescriber<List<Int>> {
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
    }.withValueArgument<Int>(-1, VariantArgType.INT_DAY_OF_MONTH, true)
        .withSingleValueDescriber<Collection<Int>> {
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
    }.withValueArgument<Int>(-1, VariantArgType.INT_DAY_OF_WEEK, true)
        .withSingleValueDescriber<Collection<Int>> {
            val arrays = R.array.days_of_week.array
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
                it.get(Calendar.HOUR_OF_DAY), it.get(Calendar.MINUTE), it.get(Calendar.SECOND)
            )
        }
    }.withValueArgument<Int>(-1, VariantArgType.INT_TIME_OF_DAY, true)
        .withSingleValueDescriber<List<Int>> {
            val start = it.firstOrNull()
            val stop = it.lastOrNull()
            when {
                start == null && stop != null -> R.string.format_before.format(formatTimeInDay(stop))
                start != null && stop == null -> R.string.format_after.format(formatTimeInDay(start))
                start != null && stop != null -> if (start != stop) {
                    R.string.format_range.format(formatTimeInDay(start), formatTimeInDay(stop))
                } else {
                    formatTimeInDay(start)
                }

                else -> R.string.no_limit.str
            }
        }

    @AppletOrdinal(0x00_05)
    val isSpecifiedTime = appletOption(R.string.is_specified_time) {
        equalCriterion<Calendar, Long> { calendar ->
            calendar.timeInMillis
        }
    }.withValueArgument<Long>(R.string.specified_time, VariantArgType.LONG_TIME)
        .withSingleValueDescriber<Long> {
            it.formatTime()
        }


    @AppletOrdinal(0x00_06)
    val isSpecifiedTimeInDay = appletOption(R.string.is_specified_time_in_day) {
        equalCriterion<Calendar, Int> {
            IntValueUtil.composeTime(
                it.get(Calendar.HOUR_OF_DAY), it.get(Calendar.MINUTE), it.get(Calendar.SECOND)
            )
        }
    }.withValueArgument<Int>(R.string.specified_time, VariantArgType.INT_TIME_OF_DAY)
        .withSingleValueDescriber<Int> {
            formatTimeInDay(it)
        }

    private fun makeTimeRangeDesc(range: Collection<Int?>, @StringRes unit: Int): CharSequence {
        val start = range.firstOrNull()
        val stop = range.lastOrNull()
        return when {
            start == null && stop != null -> R.string.format_before.format(unit.format(stop))

            start != null && stop == null -> R.string.format_after.format(unit.format(start))

            start != null && stop != null -> if (start != stop) {
                R.string.format_range.format(unit.format(start), unit.format(stop))
            } else {
                unit.format(start)
            }

            else -> illegalArgument("range")
        }
    }

    @AppletOrdinal(0x0007)
    val inSpecifiedHours = invertibleAppletOption(R.string.in_specified_hours) {
        numberRangeCriterion<Calendar, Int> {
            it.get(Calendar.HOUR_OF_DAY)
        }
    }.withValueArgument<Int>(-1, VariantArgType.INT_HOUR_OF_DAY, true)
        .withSingleValueDescriber<Collection<Int?>> {
            makeTimeRangeDesc(it, R.string.format_hour)
        }

    @AppletOrdinal(0x0008)
    val inSpecifiedMinutes = invertibleAppletOption(R.string.in_specified_minutes) {
        numberRangeCriterion<Calendar, Int> {
            it.get(Calendar.MINUTE)
        }
    }.withValueArgument<Int>(-1, VariantArgType.INT_MIN_OR_SEC, true)
        .withSingleValueDescriber<Collection<Int?>> {
            makeTimeRangeDesc(it, R.string.format_minute)
        }

    @AppletOrdinal(0x0009)
    val inSpecifiedSeconds = invertibleAppletOption(R.string.in_specified_seconds) {
        numberRangeCriterion<Calendar, Int> {
            it.get(Calendar.SECOND)
        }
    }.withValueArgument<Int>(-1, VariantArgType.INT_MIN_OR_SEC, true)
        .withSingleValueDescriber<Collection<Int?>> {
            makeTimeRangeDesc(it, R.string.format_second)
        }
}