/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.selector.argument

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import top.xjunz.tasker.R
import top.xjunz.tasker.ktx.setDigits
import top.xjunz.tasker.ktx.setMaxLength
import top.xjunz.tasker.ktx.str
import top.xjunz.tasker.task.applet.util.IntValueUtil

/**
 * @author xjunz 2022/10/27
 */
class TimeRangeEditorDialog : RangeEditorDialog() {

    override val bindingRequiredSuperClassDepth: Int = 2

    override fun String.toNumberOrNull(): Number? {
        val split = split(':')
        if (split.size != 3) return null
        val hour = split[0].toIntOrNull() ?: return null
        val min = split[1].toIntOrNull() ?: return null
        val sec = split[2].toIntOrNull() ?: return null
        if (hour !in 0..23) return null
        if (min !in 0..59) return null
        if (sec !in 0..59) return null
        return IntValueUtil.composeTime(hour, min, sec)
    }

    override fun Number.toStringOrNull(): String {
        return "%02d:%02d:%02d".format(*IntValueUtil.parseTime(this as Int))
    }

    override fun configEditText(et: EditText) {
        et.setMaxLength(8)
        et.setDigits("0123456789:")
        et.inputType = InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_TIME
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!isUnaryRange) {
            binding.tvSubtitleMin.text = R.string.start_time.str
            binding.tvSubtitleMax.text = R.string.end_time.str
        }
    }
}