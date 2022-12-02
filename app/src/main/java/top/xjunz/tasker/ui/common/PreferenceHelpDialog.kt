package top.xjunz.tasker.ui.common

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import top.xjunz.tasker.databinding.DialogPreferenceHelpBinding
import top.xjunz.tasker.ktx.doWhenCreated
import top.xjunz.tasker.ktx.text
import top.xjunz.tasker.ui.base.BaseDialogFragment

/**
 * @author xjunz 2022/12/02
 */
class PreferenceHelpDialog : BaseDialogFragment<DialogPreferenceHelpBinding>() {

    override val isFullScreen: Boolean = false

    private class InnerViewModel : ViewModel() {
        lateinit var title: CharSequence
        lateinit var helpText: CharSequence

        lateinit var doOnConfirmed: (Boolean) -> Unit
    }

    private val viewModel by viewModels<InnerViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvTitle.text = viewModel.title
        binding.tvCaption.text = viewModel.helpText
        binding.btnPositive.setOnClickListener {
            viewModel.doOnConfirmed(binding.materialSwitch.isChecked)
        }
    }

    fun init(titleRes: Int, helpRes: Int, onConfirmation: (Boolean) -> Unit): PreferenceHelpDialog =
        doWhenCreated {
            viewModel.title = titleRes.text
            viewModel.helpText = helpRes.text
            viewModel.doOnConfirmed = onConfirmation
        }

}