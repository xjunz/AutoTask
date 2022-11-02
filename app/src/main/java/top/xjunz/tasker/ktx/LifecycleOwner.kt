/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ktx

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.view.doOnAttach
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.LayoutProgressBinding

/**
 * @author xjunz 2022/05/09
 */
fun LifecycleOwner.peekContext(): Context {
    if (this is Activity) return this
    if (this is Fragment) return requireContext()
    error("This LifecycleOwner is not an Activity or a Fragment!")
}

fun LifecycleOwner.makeProgressDialog(config: ((ProgressBar, percent: TextView) -> Unit)? = null):
        MaterialAlertDialogBuilder {
    val binding = LayoutProgressBinding.inflate(LayoutInflater.from(peekContext()))
    if (config != null) {
        binding.root.doOnAttach {
            config(binding.progressIndicator, binding.tvCurrentPercent)
        }
    }
    return MaterialAlertDialogBuilder(peekContext()).setView(binding.root)
        .setCancelable(false)
        .setTitle(R.string.pls_wait)
}

fun <V> LifecycleOwner.observe(ld: LiveData<V>, observer: Observer<V>) {
    ld.observe(this, observer)
}

inline fun <V> LifecycleOwner.observeOnce(ld: LiveData<V>, crossinline observer: (V) -> Unit) {
    observe(ld, object : Observer<V> {
        override fun onChanged(t: V) {
            if (t != null) {
                ld.removeObserver(this)
                observer(t)
            }
        }
    })
}

/**
 * Observe a [MutableLiveData] as transient (non-sticky), whose value will be set to `null` once the
 * [observer] is triggered and the observer will not response to a `null` value.
 */
inline fun <V> LifecycleOwner.observeTransient(
    ld: MutableLiveData<V>,
    crossinline observer: (V) -> Unit
) {
    ld.observe(this) {
        if (it != null) {
            observer.invoke(it)
            // Must postValue(), otherwise the `null` value will fall through.
            ld.postValue(null)
        }
    }
}

inline fun <V> LifecycleOwner.observeNotNull(
    ld: MutableLiveData<V>,
    crossinline observer: (V & Any) -> Unit
) {
    ld.observe(this) {
        if (it != null) observer.invoke(it)
    }
}

inline fun <V> LifecycleOwner.observeDialog(
    ld: MutableLiveData<V>,
    crossinline block: (V) -> Dialog
) {
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

fun LifecycleOwner.observePrompt(
    ld: MutableLiveData<*>,
    @StringRes promptTextRes: Int
) {
    observeDialog(ld) {
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

inline fun LifecycleOwner.observeConfirmation(
    ld: MutableLiveData<*>,
    @StringRes promptTextRes: Int,
    crossinline onConfirmed: () -> Unit
) {
    observeConfirmation(ld, promptTextRes.text, onConfirmed)
}

inline fun LifecycleOwner.observeConfirmation(
    ld: MutableLiveData<*>,
    promptText: CharSequence,
    crossinline onConfirmed: () -> Unit
) {
    ld.observe(this) {
        if (it == null || it == false) return@observe
        peekContext().makeSimplePromptDialog(
            msg = promptText, positiveAction = onConfirmed
        ).setOnDismissListener {
            ld.value = null
        }.show()
    }
}
