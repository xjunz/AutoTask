/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.common

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import top.xjunz.tasker.databinding.DialogTextEditorBinding
import top.xjunz.tasker.ktx.doWhenCreated
import top.xjunz.tasker.ktx.textString
import top.xjunz.tasker.ui.base.BaseDialogFragment

/**
 * @author xjunz 2022/05/10
 */
class TextEditorDialog : BaseDialogFragment<DialogTextEditorBinding>() {

    override val isFullScreen: Boolean = false

    private class InnerViewModel : ViewModel() {

        lateinit var title: CharSequence

        var defText: CharSequence? = null

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
        title: CharSequence, defText: String? = null, onConfirmed: (CharSequence) -> String?
    ) = doWhenCreated {
        viewModel.title = title
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
                val error = viewModel.onConfirmed(binding.etInput.textString)
                if (error == null) {
                    dismiss()
                } else {
                    toastAndShake(error)
                }
            }
            if (viewModel.dropDownNames != null) {
                etInput.setAdapter(
                    DropdownArrayAdapter(
                        requireContext(),
                        viewModel.dropDownNames!!.toMutableList()
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
            if (savedInstanceState == null)
                etInput.setText(viewModel.defText)

            etInput.setSelection(etInput.textString.length)
            etInput.requestFocus()
            btnNegative.setOnClickListener {
                etInput.setText(viewModel.defText)
            }
            ibDismiss.setOnClickListener {
                dismiss()
            }
        }
    }

}