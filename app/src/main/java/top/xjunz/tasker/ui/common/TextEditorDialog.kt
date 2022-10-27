/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.common

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import top.xjunz.tasker.databinding.DialogTextEditorBinding
import top.xjunz.tasker.ktx.doWhenCreated
import top.xjunz.tasker.ktx.shake
import top.xjunz.tasker.ktx.textString
import top.xjunz.tasker.ktx.toast
import top.xjunz.tasker.ui.base.BaseDialogFragment

/**
 * @author xjunz 2022/05/10
 */
class TextEditorDialog : BaseDialogFragment<DialogTextEditorBinding>() {

    override val isFullScreen: Boolean = false

    private class InnerViewModel : ViewModel() {

        lateinit var title: CharSequence

        lateinit var currentText: CharSequence

        lateinit var defText: CharSequence

        var dropDownNames: Array<CharSequence>? = null

        var dropDownValues: Array<CharSequence>? = null

        /**
         * Return the error message, null if there is no error
         */
        lateinit var onConfirmed: (CharSequence) -> String?

        var editTextConfig: ((EditText) -> Unit)? = null
    }

    private val viewModel by viewModels<InnerViewModel>()

    fun setArguments(
        title: CharSequence, defText: String = "", onConfirmed: (CharSequence) -> String?
    ) = doWhenCreated {
        viewModel.title = title
        viewModel.currentText = defText
        viewModel.defText = defText
        viewModel.onConfirmed = onConfirmed
    }


    fun configEditText(config: (EditText) -> Unit) = doWhenCreated {
        viewModel.editTextConfig = config
    }

    fun setDropDownData(
        names: Array<CharSequence>,
        values: Array<CharSequence>? = null
    ) = doWhenCreated {
        viewModel.dropDownNames = names
        viewModel.dropDownValues = values
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        binding.apply {
            tvTitle.text = viewModel.title
            viewModel.editTextConfig?.invoke(etInput)
            btnPositive.setOnClickListener {
                val error = viewModel.onConfirmed(viewModel.currentText)
                if (error == null) {
                    dismiss()
                } else {
                    tilInput.shake()
                    toast(error)
                }
            }
            if (viewModel.dropDownNames != null) {
                etInput.setAdapter(
                    DropdownArrayAdapter(
                        requireContext(), viewModel.dropDownNames!!.toMutableList()
                    )
                )
                etInput.setOnItemClickListener { _, _, position, _ ->
                    etInput.setText(
                        if (viewModel.dropDownValues != null) {
                            viewModel.dropDownValues?.get(position)
                        } else {
                            viewModel.dropDownNames!![position]
                        }
                    )
                    etInput.setSelection(etInput.textString.length)
                }
            }
            etInput.setText(viewModel.currentText)
            etInput.setSelection(viewModel.currentText.length)
            etInput.requestFocus()
            etInput.doAfterTextChanged {
                viewModel.currentText = it!!.toString()
            }
            btnNegative.setOnClickListener {
                etInput.setText(viewModel.defText)
            }
            ibDismiss.setOnClickListener {
                dismiss()
            }
        }
    }

}