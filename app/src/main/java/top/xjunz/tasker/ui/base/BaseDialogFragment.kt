/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.base

import android.os.Bundle
import android.view.*
import androidx.core.view.WindowCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.viewbinding.ViewBinding
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.R
import top.xjunz.tasker.ktx.createMaterialShapeDrawable
import top.xjunz.tasker.ktx.dpFloat
import top.xjunz.tasker.ktx.shake
import top.xjunz.tasker.ktx.toast
import top.xjunz.tasker.util.ReflectionUtil.superClassFirstParameterizedType

/**
 * @author xjunz 2022/04/20
 */
open class BaseDialogFragment<T : ViewBinding> : DialogFragment(),
    HasDefaultViewModelProviderFactory {

    protected lateinit var binding: T

    open val bindingRequiredSuperClassDepth = 1

    open val isFullScreen = true

    open val decorFitsSystemWindows = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(
            if (isFullScreen) STYLE_NO_FRAME else STYLE_NORMAL,
            if (isFullScreen) R.style.Base_FragmentDialog else R.style.Base_FragmentDialog_Min
        )
    }

    open fun onBackPressed(): Boolean {
        return false
    }

    protected fun showSoftInput(view: View? = null) {
        view?.requestFocus()
        dialog?.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        var superClass: Class<*> = javaClass
        for (i in 1 until bindingRequiredSuperClassDepth) {
            superClass = superClass.superclass
        }
        binding = superClass.superClassFirstParameterizedType().getDeclaredMethod(
            "inflate", LayoutInflater::class.java, ViewGroup::class.java, Boolean::class.java
        ).invoke(null, layoutInflater, container, false)!!.casted()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val window = dialog!!.window!!
        WindowCompat.setDecorFitsSystemWindows(window, decorFitsSystemWindows)
        if (!isFullScreen) {
            val decorView = window.peekDecorView()
            // Set the background as a [MaterialAlertDialog]'s
            decorView.background = requireContext().createMaterialShapeDrawable(
                fillColorRes = com.google.android.material.R.attr.colorSurface,
                elevation = decorView.elevation, cornerSize = 24.dpFloat
            )
        }
        dialog?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                return@setOnKeyListener onBackPressed()
            }
            return@setOnKeyListener false
        }
    }

    protected fun toastAndShake(any: Any?) {
        toast(any)
        binding.root.rootView.shake()
    }

    override fun getDefaultViewModelProviderFactory() = InnerViewModelFactory
}