package top.xjunz.tasker.task.factory

import android.content.pm.PackageInfo
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.criterion.PropertyCriterion
import top.xjunz.tasker.ktx.isSystemApp
import top.xjunz.tasker.task.anno.AppletCategory

/**
 * @author xjunz 2022/10/02
 */
class ActivityCriteriaFactory(id: Int) : AppletFactory(id) {

    @AppletCategory(0x00_00)
    private val isSystem = AppletOption(0, R.string.is_system) {
        PropertyCriterion<PackageInfo> {
            it.applicationInfo.isSystemApp
        }
    }

    override val label: Int
        get() = TODO("Not yet implemented")
    override val name: String
        get() = TODO("Not yet implemented")
    override val categoryNames: IntArray
        get() = TODO("Not yet implemented")
}