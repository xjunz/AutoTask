/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.selector.option

import android.os.Bundle
import android.text.InputType
import android.util.SparseIntArray
import android.view.View
import androidx.annotation.ArrayRes
import androidx.core.util.containsKey
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import com.google.android.material.textfield.TextInputLayout
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.DialogBitsValueEditorBinding
import top.xjunz.tasker.databinding.ItemInputLayoutBinding
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.task.applet.value.BitwiseValueComposer
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.util.ClickUtil.setAntiMoneyClickListener
import kotlin.math.ceil
import kotlin.math.log10

/**
 * @author xjunz 2023/01/14
 */
class BitsValueEditorDialog : BaseDialogFragment<DialogBitsValueEditorBinding>() {

    override val isFullScreen: Boolean = false

    private class InnerViewModel : ViewModel() {

        var title: CharSequence? = null

        val enums = SparseIntArray()

        var initial: Long? = null

        lateinit var components: Array<ComponentDescriptor>

        lateinit var hints: Array<CharSequence>

        lateinit var onCompletion: (Long) -> Unit

        lateinit var composer: BitwiseValueComposer
    }

    private val viewModel by viewModels<InnerViewModel>()

    private class ComponentDescriptor(rawDescriptor: Int) {

        val isNullable = BitwiseValueComposer.isNullable(rawDescriptor)

        val maxValue = 1 shl BitwiseValueComposer.getBitCount(rawDescriptor)

        val type = BitwiseValueComposer.getType(rawDescriptor)

        val maxDigits = ceil(log10(maxValue.toFloat())).toInt()

        var inputValue: String? = null
    }

    private val bindings: MutableList<ItemInputLayoutBinding> = mutableListOf()

    private fun ItemInputLayoutBinding.getInputLayout(): TextInputLayout {
        if (tilInput.isVisible) return tilInput
        return tilMenu
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var initials: Array<Number?>? = null
        if (savedInstanceState == null && viewModel.initial != null) {
            initials = viewModel.composer.parse(viewModel.initial!!)
        }

        viewModel.components.forEachIndexed { index, descriptor ->
            val binding = ItemInputLayoutBinding.inflate(layoutInflater, binding.container, true)
            bindings.add(binding)
            if (viewModel.enums.containsKey(index)) {
                binding.tilInput.isVisible = false
                binding.tilMenu.isVisible = true
                binding.etMenu.setEntries(viewModel.enums[index], false) {
                    descriptor.inputValue = it.toString()
                }
                binding.tilMenu.hint = viewModel.hints[index]
                initials?.getOrNull(index)?.let {
                    descriptor.inputValue = (it as Int).toString()
                    binding.etMenu.setText(viewModel.enums[index].array[it])
                }
            } else {
                binding.tilInput.hint = viewModel.hints[index]
                binding.etInput.doAfterTextChanged {
                    descriptor.inputValue = it?.toString()
                }
                when (descriptor.type) {
                    BitwiseValueComposer.TYPE_RAW, BitwiseValueComposer.TYPE_PERCENT -> {
                        binding.etInput.setDigits("0123456789")
                        binding.etInput.setMaxLength(descriptor.maxDigits)
                        initials?.getOrNull(index)?.let {
                            binding.etInput.setText(it.toString())
                        }
                    }
                    BitwiseValueComposer.TYPE_FLOAT -> {
                        binding.etInput.inputType =
                            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                        binding.etInput.setDigits(".0123456789")
                        binding.etInput.setMaxLength(8)
                        initials?.getOrNull(index)?.let {
                            binding.etInput.setText(it.toString())
                        }
                    }
                }
            }
        }
        binding.btnNegative.setOnClickListener {
            dismiss()
        }
        binding.btnPositive.setAntiMoneyClickListener {
            val args = arrayOfNulls<Number>(viewModel.components.size)
            viewModel.components.forEachIndexed { index, desc ->
                val arg = desc.parseValue(index) ?: return@setAntiMoneyClickListener
                args[index] = if (desc.isNullable && arg as Int == -1) null else arg
            }
            viewModel.onCompletion(viewModel.composer.compose(*args))
            dismiss()
        }
        binding.tvTitle.text = viewModel.title
    }

    fun setEnums(index: Int, @ArrayRes arrayRes: Int) = doWhenCreated {
        viewModel.enums.put(index, arrayRes)
    }

    fun setHints(@ArrayRes arrayRes: Int) = doWhenCreated {
        viewModel.hints = arrayRes.array
    }

    fun init(
        title: CharSequence?,
        initial: Long?,
        composer: BitwiseValueComposer,
        onCompletion: (Long) -> Unit
    ) = doWhenCreated {
        viewModel.title = title
        viewModel.components = Array(composer.descriptors.size) {
            ComponentDescriptor(composer.descriptors[it])
        }
        viewModel.initial = initial
        viewModel.composer = composer
        viewModel.onCompletion = onCompletion
    }

    /**
     * @return Returns `null` if parse failed and otherwise parse succeeded. When the
     * [ComponentDescriptor] is nullable and the parsed value is null, returns -1.
     */
    private fun ComponentDescriptor.parseValue(index: Int): Number? {
        val value = inputValue
        if (isNullable && value == null) {
            return -1
        }
        val hint = viewModel.hints[index]
        val inputLayout = bindings[index].getInputLayout()
        if (value.isNullOrEmpty()) {
            inputLayout.shake()
            toast(R.string.error_unspecified.format(hint))
            return null
        }
        when (type) {
            BitwiseValueComposer.TYPE_FLOAT -> {
                val float = value.toFloatOrNull()
                if (float == null) {
                    inputLayout.shake()
                    toast(R.string.format_mal_format.format(hint))
                    return null
                }
                if (float * 100 > maxValue) {
                    inputLayout.shake()
                    toast(R.string.error_number_too_large)
                    return null
                }
                return float
            }
            BitwiseValueComposer.TYPE_PERCENT -> {
                val percent = value.toInt()
                if (percent > 100) {
                    inputLayout.shake()
                    toast(R.string.error_number_too_large)
                    return null
                }
                return percent
            }
            else -> {
                val raw = value.toInt()
                if (raw > maxValue) {
                    inputLayout.shake()
                    toast(R.string.error_number_too_large)
                    return null
                }
                return raw
            }
        }
    }
}