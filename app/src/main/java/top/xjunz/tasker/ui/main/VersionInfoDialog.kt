/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.main

import android.os.Bundle
import android.view.View
import top.xjunz.tasker.BuildConfig
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.DialogVersionInfoBinding
import top.xjunz.tasker.ktx.format
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.util.Icons.myIcon
import java.util.*

/**
 * @author xjunz 2023/02/28
 */
class VersionInfoDialog : BaseDialogFragment<DialogVersionInfoBinding>() {

    override val isFullScreen: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.ivIcon.setImageBitmap(myIcon)
        binding.tvCr.text = R.string.cr.format(Calendar.getInstance().get(Calendar.YEAR))
        binding.tvVersionName.text = BuildConfig.VERSION_NAME
    }
}