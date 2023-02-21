/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ktx

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withCreated
import kotlinx.coroutines.launch
import top.xjunz.shared.ktx.casted

/**
 * @author xjunz 2022/05/18
 */
fun <T : Fragment> T.doWhenCreated(block: () -> Unit): T {
    if (lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
        block()
    } else lifecycleScope.launch {
        lifecycle.withCreated(block)
    }
    return this
}

fun DialogFragment.show(fm: FragmentManager): Fragment {
    show(
        fm.beginTransaction(),
        javaClass.simpleName + "#" + Integer.toHexString(System.identityHashCode(this))
    )
    return this
}

inline fun <reified F : Fragment, reified VM : ViewModel> Fragment.peekParentViewModel(): VM {
    var parent = requireParentFragment()
    while (parent.javaClass != F::class.java) {
        parent = parent.requireParentFragment()
    }
    return parent.casted<F>().viewModels<VM>().value
}