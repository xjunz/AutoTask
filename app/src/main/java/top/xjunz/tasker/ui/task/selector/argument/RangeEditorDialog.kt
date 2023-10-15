/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.selector.argument

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import androidx.annotation.Keep
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import top.xjunz.shared.utils.illegalArgument
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.DialogRangeEditorBinding
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.ktx.doWhenCreated
import top.xjunz.tasker.ktx.foreColored
import top.xjunz.tasker.ktx.format
import top.xjunz.tasker.ktx.setDigits
import top.xjunz.tasker.ktx.shake
import top.xjunz.tasker.ktx.str
import top.xjunz.tasker.ktx.textString
import top.xjunz.tasker.ktx.toast
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.ui.main.ColorScheme
import top.xjunz.tasker.util.ClickListenerUtil.setNoDoubleClickListener

/**
 * @author xjunz 2022/10/26
 */
open class RangeEditorDialog : BaseDialogFragment<DialogRangeEditorBinding>() {

    override val isFullScreen: Boolean = false

    private class InnerViewModel : ViewModel() {

        var isUnaryRange: Boolean = false

        var type: Int = Applet.ARG_TYPE_INT

        var title: CharSequence? = null

        var rangeStart: Number? = null

        var rangeEnd: Number? = null

        var unarySubtitle: CharSequence? = null

        lateinit var onCompletion: (start: Number?, end: Number?) -> Unit

        var limits: IntRange? = null
    }

    private val viewModel by viewModels<InnerViewModel>()

    protected val isUnaryRange get() = viewModel.isUnaryRange

    protected open fun String.toNumberOrNull(): Number? {
        return when (viewModel.type) {
            Applet.ARG_TYPE_INT -> toIntOrNull()
            Applet.ARG_TYPE_FLOAT -> toFloatOrNull()
            Applet.ARG_TYPE_LONG -> toLongOrNull()
            else -> illegalArgument("number type", viewModel.type)
        }
    }

    /**
     * Keep this method to workaround a R8 issue.
     */
    @Keep
    protected open fun Number.toStringOrNull(): String? {
        return toString()
    }

    private fun compare(a: Number, b: Number): Int {
        return when (viewModel.type) {
            Applet.ARG_TYPE_INT -> a.toInt().compareTo(b.toInt())
            Applet.ARG_TYPE_FLOAT -> a.toFloat().compareTo(b.toFloat())
            Applet.ARG_TYPE_LONG -> a.toLong().compareTo(b.toLong())
            else -> illegalArgument("number type", viewModel.type)
        }
    }

    protected open fun configEditText(et: EditText) {
        when (viewModel.type) {
            Applet.ARG_TYPE_INT, Applet.ARG_TYPE_LONG -> {
                et.inputType = InputType.TYPE_CLASS_NUMBER
                et.setDigits("0123456789")
            }

            Applet.ARG_TYPE_FLOAT -> {
                et.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            }

            else -> illegalArgument("number type", viewModel.type)
        }
    }

    protected open fun hasError(min: Number?, max: Number?): Boolean {
        if (min == null && binding.etMinimum.textString.isNotEmpty()) {
            binding.etMinimum.shake()
            binding.etMinimum.error = R.string.error_mal_format.str
            binding.etMinimum.requestFocus()
            return true
        } else if (max == null && binding.etMaximum.textString.isNotEmpty()) {
            binding.etMaximum.shake()
            binding.etMaximum.error = R.string.error_mal_format.str
            binding.etMaximum.requestFocus()
            return true
        }
        if (min == null && max == null) {
            if (viewModel.isUnaryRange) {
                toastAndShake(R.string.error_empty_input)
            } else {
                toastAndShake(
                    R.string.format_error_no_limit.format(
                        binding.tvSubtitleMin.text,
                        binding.tvSubtitleMax.text
                    )
                )
            }
            return true
        }
        val limitMin = viewModel.limits?.first ?: Int.MIN_VALUE
        val limitMax = viewModel.limits?.last ?: Int.MAX_VALUE
        if (min != null && (compare(min, limitMin) < 0 || compare(min, limitMax) > 0)) {
            binding.etMinimum.shake()
            toast(R.string.error_input_not_in_range)
            return true
        }
        if (max != null && (compare(max, limitMin) < 0 || compare(max, limitMax) > 0)) {
            binding.etMaximum.shake()
            toast(R.string.error_input_not_in_range)
            return true
        }
        if (min != null && max != null && compare(max, min) < 0) {
            toastAndShake(
                R.string.format_error_min_greater_than_max.format(
                    binding.tvSubtitleMin.text,
                    binding.tvSubtitleMax.text
                )
            )
            return true
        }
        return false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnCancel.setNoDoubleClickListener {
            dismiss()
        }
        binding.tvTitle.text = viewModel.title
        binding.btnClearMax.setNoDoubleClickListener {
            binding.etMaximum.text.clear()
        }
        binding.btnClearMin.setNoDoubleClickListener {
            binding.etMinimum.text.clear()
        }
        binding.btnMakeEqualMax.setNoDoubleClickListener {
            binding.etMaximum.text = binding.etMinimum.text
        }
        binding.btnMakeEqualMin.setNoDoubleClickListener {
            binding.etMinimum.text = binding.etMaximum.text
        }
        binding.btnComplete.setNoDoubleClickListener {
            val min: Number? = binding.etMinimum.textString.toNumberOrNull()
            if (viewModel.isUnaryRange) {
                binding.etMaximum.text = binding.etMinimum.text
            }
            val max: Number? = binding.etMaximum.textString.toNumberOrNull()
            if (!hasError(min, max)) {
                viewModel.onCompletion(min, max)
                dismiss()
            }
        }

        configEditText(binding.etMinimum)
        configEditText(binding.etMaximum)

        if (savedInstanceState == null) {
            binding.etMinimum.setText(viewModel.rangeStart?.toStringOrNull())
            binding.etMaximum.setText(viewModel.rangeEnd?.toStringOrNull())
        }

        if (viewModel.isUnaryRange) {
            binding.containerMaximum.isVisible = false
            binding.tvSubtitleMin.text = viewModel.unarySubtitle
            binding.btnMakeEqualMin.isVisible = false
        }
        viewModel.limits?.let {
            val limitStr =
                "(" + it.first + "~" + it.last + ")".foreColored(ColorScheme.textColorDisabled)
            binding.tvSubtitleMin.append(limitStr)
            binding.tvSubtitleMax.append(limitStr)
        }
    }

    fun doOnCompletion(block: (start: Number?, end: Number?) -> Unit) = doWhenCreated {
        viewModel.onCompletion = block
    }

    fun setTitle(title: CharSequence?) = doWhenCreated {
        viewModel.title = title
    }

    fun setType(type: Int): RangeEditorDialog = doWhenCreated {
        viewModel.type = type
    }

    fun asUnary() = doWhenCreated {
        viewModel.isUnaryRange = true
    }

    fun setUnarySubtitle(subtitle: CharSequence?) = doWhenCreated {
        viewModel.unarySubtitle = subtitle
    }

    fun setLimits(limits: IntRange) = doWhenCreated {
        viewModel.limits = limits
    }

    fun setRange(start: Number?, end: Number?, defStart: Number? = null, defEnd: Number? = null) =
        doWhenCreated {
            viewModel.rangeStart = start
            viewModel.rangeEnd = end
            if (start == null && end == null) {
                viewModel.rangeStart = defStart
                viewModel.rangeEnd = defEnd
            }
        }
}