package top.xjunz.tasker.task.factory

import top.xjunz.tasker.R
import top.xjunz.tasker.engine.criterion.Criterion
import top.xjunz.tasker.engine.criterion.RangeCriterion
import top.xjunz.tasker.task.anno.AppletCategory
import java.util.*

/**
 * @author xjunz 2022/10/01
 */
class TimeCriteriaFactory : AppletFactory(AppletRegistry.ID_TIME_FACTORY) {

    private fun TimeCollectionCriterion(block: (Calendar) -> Int): Criterion<Calendar, Collection<Int>> {
        return object : Criterion<Calendar, Collection<Int>>() {
            override fun matchTarget(target: Calendar, value: Collection<Int>): Boolean {
                return value.contains(block(target))
            }
        }
    }

    @AppletCategory(0x00_00)
    private val timeRange = NotInvertibleAppletOption(0, R.string.time_range) {
        RangeCriterion<Calendar, Long> {
            it.timeInMillis
        }
    }

    @AppletCategory(0x00_01)
    private val year = AppletOption(0x10, R.string.in_year_range) {
        RangeCriterion<Calendar, Int> {
            it.get(Calendar.YEAR)
        }
    }

    @AppletCategory(0x00_02)
    private val month = AppletOption(0x11, R.string.in_months) {
        TimeCollectionCriterion {
            it.get(Calendar.MONTH)
        }
    }

    @AppletCategory(0x00_03)
    private val dayOfMonth = AppletOption(0x12, R.string.in_day_of_month) {
        RangeCriterion<Calendar, Int> {
            it.get(Calendar.DAY_OF_MONTH)
        }
    }

    @AppletCategory(0x00_03)
    private val dayOfWeek = AppletOption(0x13, R.string.in_day_of_week) {
        TimeCollectionCriterion {
            it.get(Calendar.DAY_OF_WEEK)
        }
    }

    @AppletCategory(0x00_04)
    private val hourMinSec = AppletOption(0x20, R.string.in_hour_min_sec_range) {
        RangeCriterion<Calendar, Int> {
            it.get(Calendar.HOUR) shl 16 and it.get(Calendar.MINUTE) shl 8 and it.get(Calendar.SECOND)
        }
    }


    override val label: Int = R.string.time_matches

    override val name: String
        get() = TODO("Not yet implemented")

    override val categoryNames: IntArray = intArrayOf(LABEL_NONE)
}