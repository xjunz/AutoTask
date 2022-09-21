/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ktx

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenCreated
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * @author xjunz 2022/05/18
 */
fun Fragment.doOnCreated(block: CoroutineScope.() -> Unit) {
    lifecycleScope.launch {
        lifecycle.whenCreated(block)
    }
}

fun DialogFragment.show(fm: FragmentManager) {
    show(fm, javaClass.simpleName)
}