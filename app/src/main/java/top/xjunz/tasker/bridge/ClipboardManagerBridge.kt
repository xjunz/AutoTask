package top.xjunz.tasker.bridge

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.IClipboard
import android.system.Os
import rikka.shizuku.SystemServiceHelper
import top.xjunz.tasker.BuildConfig
import top.xjunz.tasker.app
import top.xjunz.tasker.isInRemoteProcess

/**
 * @author xjunz 2022/11/16
 */
object ClipboardManagerBridge {

    fun copyToClipboard(text: CharSequence) {
        val clipData = ClipData.newPlainText(null, text)
        if (isInRemoteProcess) {
            IClipboard.Stub.asInterface(SystemServiceHelper.getSystemService(Context.CLIPBOARD_SERVICE))
                .setPrimaryClip(clipData, BuildConfig.APPLICATION_ID, Os.getuid())
        } else {
            val cbm = app.getSystemService(ClipboardManager::class.java)
            cbm.setPrimaryClip(clipData)
        }
    }
}