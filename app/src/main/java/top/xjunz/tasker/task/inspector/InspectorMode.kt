/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.inspector

import androidx.annotation.StringRes
import top.xjunz.tasker.R
import top.xjunz.tasker.ktx.str

/**
 * @author xjunz 2022/10/31
 */
enum class InspectorMode(@StringRes labelRes: Int) {

    COMPONENT(R.string.component_info), UI_OBJECT(R.string.ui_object), COORDS(R.string.coords);

    val label: String = labelRes.str
}