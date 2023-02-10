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
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.service.floatingInspector
import top.xjunz.tasker.task.inspector.FloatingInspector
import top.xjunz.tasker.task.inspector.InspectorMode
import top.xjunz.tasker.ui.MainViewModel.Companion.peekMainViewModel
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.ui.task.inspector.FloatingInspectorDialog
import top.xjunz.tasker.util.ClickUtil.setAntiMoneyClickListener

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
    }

    private val viewModel by viewModels<InnerViewModel>()

    fun init(
        title: CharSequence?, defText: CharSequence? = null, onConfirmed: (String) -> CharSequence?
    ) = doWhenCreated {
        viewModel.title = title
        viewModel.defText = defText
        viewModel.onConfirmed = onConfirmed
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
            btnPositive.setAntiMoneyClickListener {
                if (!viewModel.allowEmptyInput && inputBox.text.isEmpty()) {
                    toastAndShake(R.string.error_empty_input)
                    return@setAntiMoneyClickListener
                }
                val error = viewModel.onConfirmed(inputBox.textString)
                if (error == null) {
                    dismiss()
                } else {
                    toastAndShake(error)
                }
            }
            if (!viewModel.defText.isNullOrEmpty()) {
                inputBox.doAfterTextChanged {
                    if (it?.toString() != viewModel.defText) {
                        btnNegative.setText(R.string.reset)
                    } else {
                        btnNegative.setText(android.R.string.cancel)
                    }
                }
            }
            if (savedInstanceState == null)
                inputBox.setText(viewModel.defText)

            inputBox.setSelectionToEnd()
            showSoftInput(inputBox)
            btnNegative.setOnClickListener {
                if (viewModel.defText.isNullOrEmpty()) {
                    dismiss()
                } else if (inputBox.textString == viewModel.defText) {
                    dismiss()
                } else {
                    inputBox.setText(viewModel.defText)
                    inputBox.setSelectionToEnd()
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
        binding.cvContainer.isVisible = viewModel.variantType != -1
        binding.cvContainer.setAntiMoneyClickListener {
            FloatingInspectorDialog().setMode(InspectorMode.COMPONENT).show(childFragmentManager)
        }
        peekMainViewModel().doOnRouted(this, FloatingInspector.ACTION_COMPONENT_SELECTED) {
            val component = floatingInspector.viewModel.currentComp.require()
            component.paneTitle?.let {
                inputBox.setText(it)
                inputBox.setSelectionToEnd()
                toast(R.string.format_added.format(it))
            }
        }
    }

}