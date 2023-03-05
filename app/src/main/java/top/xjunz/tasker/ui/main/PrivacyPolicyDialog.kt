/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.main

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.WebSettings
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import top.xjunz.tasker.Preferences
import top.xjunz.tasker.bridge.DisplayManagerBridge
import top.xjunz.tasker.databinding.DialogPrivacyPolicyBinding
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.util.ClickListenerUtil.setNoDoubleClickListener

/**
 * @author xjunz 2023/03/05
 */
class PrivacyPolicyDialog : BaseDialogFragment<DialogPrivacyPolicyBinding>() {

    override val isFullScreen: Boolean = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.updateLayoutParams {
            height = (.75 * DisplayManagerBridge.size.y).toInt()
        }
        isCancelable = Preferences.privacyPolicyAcknowledged
        binding.webView.apply {
            settings.javaScriptEnabled = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                settings.isAlgorithmicDarkeningAllowed = true
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                settings.forceDark = WebSettings.FORCE_DARK_AUTO
            }
            setBackgroundColor(Color.TRANSPARENT)
            loadUrl("file:///android_asset/privacy-policy.html")
        }
        binding.btnAck.setNoDoubleClickListener {
            Preferences.privacyPolicyAcknowledged = true
            dismiss()
        }
        binding.btnQuit.isVisible = !Preferences.privacyPolicyAcknowledged
        binding.btnQuit.setNoDoubleClickListener {
            requireActivity().finish()
        }
    }
}