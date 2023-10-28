/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.base

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.ComponentDialog
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.view.WindowCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.R
import top.xjunz.tasker.ktx.createMaterialShapeDrawable
import top.xjunz.tasker.ktx.dpFloat
import top.xjunz.tasker.ktx.shake
import top.xjunz.tasker.ktx.toast
import top.xjunz.tasker.ui.main.MainViewModel
import top.xjunz.tasker.util.ReflectionUtil.superClassFirstParameterizedType

/**
 * @author xjunz 2022/04/20
 */
open class BaseDialogFragment<T : ViewBinding> : AppCompatDialogFragment(),
    HasDefaultViewModelProviderFactory {

    private var isExiting = false

    protected lateinit var binding: T

    protected val mainViewModel by activityViewModels<MainViewModel>()

    protected open val bindingRequiredSuperClassDepth = 1

    protected open val isFullScreen = true

    protected open val decorFitsSystemWindows get() = !isFullScreen

    protected open val windowAnimationStyle get() = R.style.DialogAnimationSade

    private val mixin by lazy {
        DialogStackMixin(this, isFullScreen)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(
            if (isFullScreen) STYLE_NO_FRAME else STYLE_NORMAL,
            if (isFullScreen) R.style.Base_Dialog else R.style.Base_Dialog_Min
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
        requireDialog().window?.setWindowAnimations(windowAnimationStyle)
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
        val window = requireDialog().window!!
        WindowCompat.setDecorFitsSystemWindows(window, decorFitsSystemWindows)
        if (!isFullScreen) {
            val decorView = window.peekDecorView()
            // Set the background as a [MaterialAlertDialog]'s
            decorView.background = requireContext().createMaterialShapeDrawable(
                fillColorRes = com.google.android.material.R.attr.colorSurface,
                elevation = decorView.elevation, cornerSize = 24.dpFloat
            )
        }
        (dialog as ComponentDialog).onBackPressedDispatcher.addCallback(this) {
            if (!onBackPressed()) {
                requireDialog().cancel()
            }
        }
        mixin.doOnViewCreated()
    }

    override fun onStart() {
        super.onStart()
        mixin.doOnStart()
    }

    override fun dismiss() {
        mixin.doOnDismissOrCancel()
        super.dismiss()
    }

    override fun onCancel(dialog: DialogInterface) {
        mixin.doOnDismissOrCancel()
        super.onCancel(dialog)
        onBackPressed()
    }

    protected fun toastAndShake(any: Any?) {
        toast(any)
        binding.root.rootView.shake()
    }

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory = InnerViewModelFactory
}