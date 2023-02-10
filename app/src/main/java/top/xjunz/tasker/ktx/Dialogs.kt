/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ktx

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.CheckResult
import androidx.appcompat.app.AlertDialog
import androidx.core.view.doOnAttach
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.LayoutProgressBinding
import top.xjunz.tasker.util.ClickUtil.setAntiMoneyClickListener

/**
 * @author xjunz 2022/07/25
 */

@CheckResult
inline fun Context.makeSimplePromptDialog(
    title: Int = R.string.tip, msg: Int,
    showCancellationBtn: Boolean = true,
    crossinline positiveAction: () -> Unit = {}
): AlertDialog.Builder {
    return makeSimplePromptDialog(title.text, msg.text, showCancellationBtn, positiveAction)
}

@CheckResult
inline fun Context.makeSimplePromptDialog(
    title: CharSequence = R.string.tip.text, msg: CharSequence,
    showCancellationBtn: Boolean = true,
    crossinline positiveAction: () -> Unit = {}
): AlertDialog.Builder {
    val builder = MaterialAlertDialogBuilder(this).setTitle(title).setMessage(msg)
    if (showCancellationBtn) {
        builder.setNegativeButton(android.R.string.cancel, null)
    }
    builder.setPositiveButton(android.R.string.ok) { _, _ -> positiveAction.invoke() }
    return builder
}

fun LifecycleOwner.showErrorDialog(t: Throwable): AlertDialog {
    return peekContext().showErrorDialog(t.stackTraceToString())
}

fun Context.showErrorDialog(stackTrace: String): AlertDialog {
    val dialog = MaterialAlertDialogBuilder(this).setTitle(R.string.error_occurred)
        .setMessage(stackTrace).setPositiveButton(R.string.feedback, null)
        .setNegativeButton(android.R.string.cancel, null).create()
    dialog.show()
    dialog.getButton(DialogInterface.BUTTON_POSITIVE).setAntiMoneyClickListener {
      //  Feedbacks.showErrorFeedbackDialog(this, stackTrace)
    }
    return dialog
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
