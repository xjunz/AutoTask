/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.selector.option

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import top.xjunz.tasker.R
import top.xjunz.tasker.ktx.setMaxLength
import top.xjunz.tasker.ktx.str
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author xjunz 2022/10/26
 */
class DateTimeRangeEditorDialog : RangeEditorDialog() {

    override val bindingRequiredSuperClassDepth: Int = 2

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvSubtitleMin.text = R.string.start_time.str
        binding.tvSubtitleMax.text = R.string.end_time.str
    }

    override fun String.toNumberOrNull(): Number? {
        return try {
            val date = dateFormat.parse(this)
            date?.time
        } catch (e: ParseException) {
            null
        }
    }

    override fun Number.toStringOrNull(): String? {
        return dateFormat.format(this as Long)
    }

    override fun configEditText(et: EditText) {
        et.setMaxLength(19)
        et.inputType = InputType.TYPE_CLASS_DATETIME
    }
}