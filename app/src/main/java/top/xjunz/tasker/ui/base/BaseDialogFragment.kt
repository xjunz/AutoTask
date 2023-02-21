/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.base

import android.os.Bundle
import android.view.*
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
import top.xjunz.tasker.ui.main.DialogStackManager
import top.xjunz.tasker.ui.main.MainViewModel
import top.xjunz.tasker.util.Motions
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
        mainViewModel.notifyDialogShown(tag, isFullScreen)
        mainViewModel.doOnDialogShown(this) {
            if (it.tag != tag && requireDialog().isShowing && it.isFullScreen) {
                isExiting = true
                animateExit(window)
            }
        }
        mainViewModel.doOnDialogDismissed(this) {
            if (it.tag != tag && (isExiting || !requireDialog().isShowing)
                && DialogStackManager.isVisible(tag)
            ) {
                isExiting = false
                animateReturn(window)
            }
        }
    }

    private fun animateExit(window: Window) {
        window.peekDecorView().animate()
            .scaleX(1.05f)
            .scaleY(1.05f)
            .withEndAction {
                isExiting = false
                onStop()
            }.setDuration(500)
            .setInterpolator(Motions.EASING_EMPHASIZED)
            .start()
    }

    private fun animateReturn(window: Window) {
        // Remove the decor view animation temporarily
        window.setWindowAnimations(0)
        onStart()
        window.peekDecorView().animate()
            .setDuration(200)
            .scaleX(1f)
            .scaleY(1f)
            .withEndAction {
                window.setWindowAnimations(windowAnimationStyle)
            }
            .setInterpolator(Motions.EASING_EMPHASIZED)
            .start()
    }

    override fun onDestroyView() {
        if (view != null) {
            mainViewModel.notifyDialogDismissed()
        }
        super.onDestroyView()
    }

    protected fun toastAndShake(any: Any?) {
        toast(any)
        binding.root.rootView.shake()
    }

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory = InnerViewModelFactory
}