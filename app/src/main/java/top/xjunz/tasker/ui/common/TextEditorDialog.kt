/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.common

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.DialogTextEditorBinding
import top.xjunz.tasker.ktx.doWhenCreated
import top.xjunz.tasker.ktx.format
import top.xjunz.tasker.ktx.setSelectionToEnd
import top.xjunz.tasker.ktx.show
import top.xjunz.tasker.ktx.str
import top.xjunz.tasker.ktx.textString
import top.xjunz.tasker.ktx.toast
import top.xjunz.tasker.task.applet.flow.ref.ComponentInfoWrapper
import top.xjunz.tasker.task.applet.value.VariantType
import top.xjunz.tasker.task.inspector.FloatingInspector
import top.xjunz.tasker.task.inspector.InspectorMode
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.ui.main.EventCenter.doOnEventRoutedWithValue
import top.xjunz.tasker.ui.task.inspector.FloatingInspectorDialog
import top.xjunz.tasker.util.ClickListenerUtil.setNoDoubleClickListener

/**
 * @author xjunz 2022/05/10
 */
class TextEditorDialog : BaseDialogFragment<DialogTextEditorBinding>() {

    override val isFullScreen: Boolean = false

    private class InnerViewModel : ViewModel() {

        var variantType: Int = -1

        var allowEmptyInput = false

        var title: CharSequence? = null

        var caption: CharSequence? = null

        var defText: CharSequence? = null

        var dropDownNames: Array<out CharSequence>? = null

        var dropDownValues: Array<out CharSequence>? = null

        /**
         * Return the error message, null if there is no error
         */
        lateinit var onConfirmed: (String) -> CharSequence?

        var editTextConfig: ((EditText) -> Unit)? = null

        var hint: CharSequence? = null
    }

    private val viewModel by viewModels<InnerViewModel>()

    fun init(
        title: CharSequence?, defText: CharSequence? = null, onConfirmed: (String) -> CharSequence?
    ) = doWhenCreated {
        viewModel.title = title
        viewModel.defText = defText
        viewModel.onConfirmed = onConfirmed
    }

    fun setHint(hint: CharSequence?) = doWhenCreated {
        viewModel.hint = hint
    }

    fun configEditText(config: (EditText) -> Unit) = doWhenCreated {
        viewModel.editTextConfig = config
    }

    fun setDropDownData(
        names: Array<out CharSequence>,
        values: Array<out CharSequence>? = null
    ) = doWhenCreated {
        viewModel.dropDownNames = names
        viewModel.dropDownValues = values
    }

    fun setCaption(caption: CharSequence?) = doWhenCreated {
        viewModel.caption = caption
    }

    fun setAllowEmptyInput() = doWhenCreated {
        viewModel.allowEmptyInput = true
    }

    fun setVariantType(variantType: Int) = doWhenCreated {
        viewModel.variantType = variantType
    }

    private lateinit var inputBox: EditText

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            tvTitle.text = viewModel.title
            tvCaption.isVisible = !viewModel.caption.isNullOrEmpty()
            tvCaption.text = viewModel.caption
            if (viewModel.dropDownNames != null) {
                inputBox = etMenu
                etMenu.setAdapter(
                    DropdownArrayAdapter(
                        requireContext(),
                        viewModel.dropDownNames!!.toMutableList()
                    )
                )
                etMenu.setOnItemClickListener { _, _, position, _ ->
                    inputBox.setText(
                        if (viewModel.dropDownValues != null) {
                            viewModel.dropDownValues?.get(position)
                        } else {
                            viewModel.dropDownNames!![position]
                        }
                    )
                    inputBox.setSelectionToEnd()
                }
                etMenu.threshold = Int.MAX_VALUE
            } else {
                inputBox = etInput
                tilMenu.isVisible = false
                tilInput.isVisible = true
            }
            viewModel.editTextConfig?.invoke(inputBox)
            btnPositive.setNoDoubleClickListener {
                if (!viewModel.allowEmptyInput && inputBox.text.isEmpty()) {
                    toastAndShake(R.string.error_empty_input)
                    return@setNoDoubleClickListener
                }
                val error = viewModel.onConfirmed(inputBox.textString)
                if (error == null) {
                    dismiss()
                } else {
                    toastAndShake(error)
                }
            }
            inputBox.doAfterTextChanged {
                if (!it.isNullOrEmpty()) {
                    btnNegative.setText(R.string.clear_all)
                } else {
                    if (viewModel.defText.isNullOrEmpty()) {
                        btnNegative.setText(android.R.string.cancel)
                    } else {
                        btnNegative.setText(R.string._default)
                    }
                }
            }
            tilInput.hint = viewModel.hint

            if (savedInstanceState == null) inputBox.setText(viewModel.defText)
            inputBox.setSelectionToEnd()
            showSoftInput(inputBox)

            btnNegative.setOnClickListener {
                when (btnNegative.text.toString()) {
                    R.string.clear_all.str -> {
                        inputBox.text.clear()
                    }

                    R.string._default.str -> {
                        inputBox.setText(viewModel.defText)
                        inputBox.setSelectionToEnd()
                    }

                    android.R.string.cancel.str -> {
                        dismiss()
                    }
                }
            }
            ibDismiss.setOnClickListener {
                dismiss()
            }
        }
        dialog?.setOnKeyListener l@{ _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER
                && event.action == KeyEvent.ACTION_UP
                && inputBox.maxLines == 1
            ) {
                binding.btnPositive.performClick()
                return@l true
            }
            return@l false
        }
        binding.cvContainer.isVisible = viewModel.variantType == VariantType.TEXT_ACTIVITY
        binding.cvContainer.setNoDoubleClickListener {
            FloatingInspectorDialog().setMode(InspectorMode.COMPONENT).show(childFragmentManager)
        }
        doOnEventRoutedWithValue<ComponentInfoWrapper>(FloatingInspector.EVENT_COMPONENT_SELECTED) {
            it.paneTitle?.run {
                inputBox.setText(this)
                inputBox.setSelectionToEnd()
                toast(R.string.format_added.format(this))
            }
        }
    }
}