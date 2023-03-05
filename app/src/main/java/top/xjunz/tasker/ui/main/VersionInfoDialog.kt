/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.main

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import top.xjunz.tasker.BuildConfig
import top.xjunz.tasker.databinding.DialogVersionInfoBinding
import top.xjunz.tasker.ktx.show
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.ui.main.EventCenter.doOnEventRouted
import top.xjunz.tasker.util.Icons.myIcon

/**
 * @author xjunz 2023/02/28
 */
class VersionInfoDialog : BaseDialogFragment<DialogVersionInfoBinding>() {

    companion object {
        const val HOST_PRIVACY_POLICY = "privacy_policy"
    }

    override val isFullScreen: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.ivIcon.setImageBitmap(myIcon)
        binding.tvCr.movementMethod = LinkMovementMethod.getInstance()
        binding.tvVersionName.text = BuildConfig.VERSION_NAME
        doOnEventRouted(HOST_PRIVACY_POLICY) {
            PrivacyPolicyDialog().show(childFragmentManager)
        }
    }
}