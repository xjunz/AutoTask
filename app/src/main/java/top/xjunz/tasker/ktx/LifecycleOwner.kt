/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ktx

import android.app.Activity
import android.app.Dialog
import android.content.Context
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

/**
 * @author xjunz 2022/05/09
 */
private fun LifecycleOwner.peekContext(): Context {
    if (this is Activity) return this
    if (this is Fragment) return requireContext()
    error("This LifecycleOwner is not an Activity or a Fragment!")
}

fun <V> LifecycleOwner.observe(ld: LiveData<V>, observer: Observer<V>) {
    ld.observe(this, observer)
}

/**
 * Observe a [MutableLiveData] as transient (non-sticky), whose value will be set to `null` once the
 * [observer] is triggered.
 */
fun <V> LifecycleOwner.observeTransient(ld: MutableLiveData<V>, observer: (V) -> Unit) {
    ld.observe(this) {
        if (it != null) {
            observer.invoke(it)
            ld.value = null
        }
    }
}

fun <V> LifecycleOwner.observeDialog(ld: MutableLiveData<V>, block: (V) -> Dialog) {
    ld.observe(this) {
        if (it == null) return@observe
        if (it is Boolean && !it) return@observe
        val dialog = block.invoke(it)
        if (!dialog.isShowing) dialog.show()
        dialog.setOnDismissListener {
            ld.value = null
        }
    }
}

fun LifecycleOwner.observePrompt(ld: MutableLiveData<*>, @StringRes promptTextRes: Int) {
    ld.observe(this) {
        if (it == null) return@observe
        if (it is Boolean && !it) return@observe
        peekContext().makeSimplePromptDialog(msg = promptTextRes).setOnDismissListener {
            ld.value = null
        }.show()
    }
}

fun <V : Throwable> LifecycleOwner.observeError(ld: MutableLiveData<V>) {
    ld.observe(this) {
        if (it == null) return@observe
        peekContext().showErrorDialog(it).setOnDismissListener {
            ld.value = null
        }
    }
}

fun LifecycleOwner.observeConfirmation(
    ld: MutableLiveData<*>,
    @StringRes promptTextRes: Int,
    onConfirmed: () -> Unit
) {
    ld.observe(this) {
        if (it == null) return@observe
        if (it is Boolean && !it) return@observe
        peekContext().makeSimplePromptDialog(
            msg = promptTextRes, positiveAction = onConfirmed
        ).setOnDismissListener {
            ld.value = null
        }.show()
    }
}
