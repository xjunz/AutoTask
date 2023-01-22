/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ktx

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withCreated
import kotlinx.coroutines.launch
import top.xjunz.shared.ktx.casted

/**
 * @author xjunz 2022/05/18
 */
fun <T : Fragment> T.doWhenCreated(block: () -> Unit): T {
    lifecycleScope.launch {
        lifecycle.withCreated(block)
    }
    return this
}

fun DialogFragment.show(fm: FragmentManager): Fragment {
    show(fm, javaClass.simpleName + "#" + Integer.toHexString(System.identityHashCode(this)))
    return this
}

inline fun <reified T : Fragment> Fragment.peekParentFragment(): T {
    var parent = requireParentFragment()
    while (parent.javaClass != T::class.java) {
        parent = parent.requireParentFragment()
    }
    return parent.casted()
}