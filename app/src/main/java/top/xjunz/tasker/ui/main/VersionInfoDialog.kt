/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.main

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import top.xjunz.tasker.BuildConfig
import top.xjunz.tasker.R
import top.xjunz.tasker.app
import top.xjunz.tasker.databinding.DialogVersionInfoBinding
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.ui.main.EventCenter.doOnEventRouted
import top.xjunz.tasker.ui.main.MainViewModel.Companion.peekMainViewModel
import top.xjunz.tasker.util.ClickListenerUtil.setNoDoubleClickListener
import top.xjunz.tasker.util.Icons.myIcon

/**
 * @author xjunz 2023/02/28
 */
class VersionInfoDialog : BaseDialogFragment<DialogVersionInfoBinding>() {

    companion object {
        const val HOST_PRIVACY_POLICY = "privacy_policy"
    }

    override val isFullScreen: Boolean = false

    private var observed = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.ivIcon.setImageBitmap(myIcon)
        binding.tvCr.movementMethod = LinkMovementMethod.getInstance()
        binding.tvVersionName.text = BuildConfig.VERSION_NAME
        val mvm = peekMainViewModel()
        binding.btnUpdate.setNoDoubleClickListener {
            if (app.updateInfo.isNull()) {
                if (!observed) {
                    observeTransient(mvm.checkingForUpdates) {
                        binding.btnUpdate.isEnabled = !it
                    }
                    observeTransient(mvm.checkingForUpdatesError) {
                        toast(R.string.format_request_failed.format(it))
                    }
                    observed = true
                }
                mvm.checkForUpdates()
            } else {
                if (app.updateInfo.value?.hasUpdates() == true) {
                    mvm.showUpdateDialog = true
                    app.updateInfo.notifySelfChanged()
                } else {
                    toast(R.string.no_updates)
                }
            }
        }
        doOnEventRouted(HOST_PRIVACY_POLICY) {
            PrivacyPolicyDialog().show(childFragmentManager)
        }
    }
}