/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.viewbinding.ViewBinding
import top.xjunz.shared.ktx.unsafeCast
import top.xjunz.tasker.util.ReflectionUtil.superClassFirstParameterizedType

/**
 * @author xjunz 2022/04/23
 */
open class BaseFragment<T : ViewBinding> : Fragment(), HasDefaultViewModelProviderFactory {

    open val bindingRequiredSuperClassDepth = 1

    protected val binding: T by lazy {
        var superClass: Class<*> = javaClass
        for (i in 1 until bindingRequiredSuperClassDepth) {
            superClass = superClass.superclass
        }
        superClass.superClassFirstParameterizedType().getDeclaredMethod(
            "inflate", LayoutInflater::class.java
        ).invoke(null, layoutInflater)!!.unsafeCast()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun getDefaultViewModelProviderFactory() = InnerViewModelFactory

}