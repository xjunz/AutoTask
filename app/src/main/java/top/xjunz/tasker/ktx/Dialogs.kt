package top.xjunz.tasker.ktx

import android.content.Context
import android.content.DialogInterface
import androidx.annotation.CheckResult
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import top.xjunz.tasker.R

/**
 * @author xjunz 2022/07/25
 */

@CheckResult
inline fun Context.makeSimplePromptDialog(
    title: Int = R.string.prompt, msg: Int,
    showCancellationBtn: Boolean = true,
    crossinline positiveAction: () -> Unit = {}
): AlertDialog.Builder {
    return makeSimplePromptDialog(title.text, msg.text, showCancellationBtn, positiveAction)
}

@CheckResult
inline fun Context.makeSimplePromptDialog(
    title: CharSequence = R.string.prompt.text, msg: CharSequence,
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

fun Context.showErrorDialog(t: Throwable): AlertDialog {
    return showErrorDialog(t.stackTraceToString())
}

fun Context.showErrorDialog(stackTrace: String): AlertDialog {
    val dialog = MaterialAlertDialogBuilder(this).setTitle(R.string.error_occurred)
        .setMessage(stackTrace).setPositiveButton(R.string.feedback, null)
        .setNegativeButton(android.R.string.cancel, null).create()
    dialog.show()
    dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
      //  Feedbacks.showErrorFeedbackDialog(this, stackTrace)
    }
    return dialog
}