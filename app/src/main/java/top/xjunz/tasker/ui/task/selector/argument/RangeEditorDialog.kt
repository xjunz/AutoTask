/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.selector.argument

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import top.xjunz.shared.utils.illegalArgument
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.DialogRangeEditorBinding
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.task.applet.value.VariantType
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.util.ClickListenerUtil.setNoDoubleClickListener

/**
 * @author xjunz 2022/10/26
 */
open class RangeEditorDialog : BaseDialogFragment<DialogRangeEditorBinding>() {

    override val isFullScreen: Boolean = false

    private class InnerViewModel : ViewModel() {

        var type: Int = Applet.VAL_TYPE_INT

        var variantType: Int = -1

        var title: CharSequence? = null

        var rangeStart: Number? = null

        var rangeEnd: Number? = null

        lateinit var onCompletion: (start: Number?, end: Number?) -> Unit
    }

    private val viewModel by viewModels<InnerViewModel>()

    protected open fun String.toNumberOrNull(): Number? {
        return when (viewModel.type) {
            Applet.VAL_TYPE_INT -> toIntOrNull()
            Applet.VAL_TYPE_FLOAT -> toFloatOrNull()
            Applet.VAL_TYPE_LONG -> toLongOrNull()
            else -> illegalArgument("number type", viewModel.type)
        }
    }

    protected open fun Number.toStringOrNull(): String? {
        return toString()
    }

    private fun compare(a: Number, b: Number): Int {
        return when (viewModel.type) {
            Applet.VAL_TYPE_INT -> (a as Int).compareTo(b as Int)
            Applet.VAL_TYPE_FLOAT -> (a as Float).compareTo(b as Float)
            Applet.VAL_TYPE_LONG -> (a as Long).compareTo(b as Long)
            else -> illegalArgument("number type", viewModel.type)
        }
    }

    protected open fun configEditText(et: EditText) {
        when (viewModel.type) {
            Applet.VAL_TYPE_INT, Applet.VAL_TYPE_LONG -> {
                et.inputType = InputType.TYPE_CLASS_NUMBER
                et.setDigits("0123456789")
            }
            Applet.VAL_TYPE_FLOAT -> {
                et.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            }
            else -> illegalArgument("number type", viewModel.type)
        }
        if (viewModel.variantType == VariantType.INT_PERCENT_RANGE) {
            et.setMaxLength(3)
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
            toastAndShake(
                R.string.format_error_no_limit.format(
                    binding.tvSubtitleMin.text,
                    binding.tvSubtitleMax.text
                )
            )
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
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        binding.tvTitle.text = viewModel.title
        binding.btnNoMaxLimit.setOnClickListener {
            binding.etMaximum.text.clear()
        }
        binding.btnNoMinLimit.setOnClickListener {
            binding.etMinimum.text.clear()
        }
        binding.btnComplete.setNoDoubleClickListener {
            val min: Number? = binding.etMinimum.textString.toNumberOrNull()
            val max: Number? = binding.etMaximum.textString.toNumberOrNull()
            if (!hasError(min, max)) {
                viewModel.onCompletion(min, max)
                dismiss()
            }
        }
        configEditText(binding.etMaximum)
        configEditText(binding.etMinimum)

        if (savedInstanceState == null) {
            binding.etMinimum.setText(viewModel.rangeStart?.toStringOrNull())
            binding.etMaximum.setText(viewModel.rangeEnd?.toStringOrNull())
        }
    }

    fun doOnCompletion(block: (start: Number?, end: Number?) -> Unit) = doWhenCreated {
        viewModel.onCompletion = block
    }

    fun setTitle(title: CharSequence?) = doWhenCreated {
        viewModel.title = title
    }

    fun setType(type: Int, variantType: Int = -1): RangeEditorDialog = doWhenCreated {
        viewModel.type = type
        viewModel.variantType = variantType
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