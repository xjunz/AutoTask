package top.xjunz.tasker.api

import android.text.format.Formatter
import androidx.core.text.parseAsHtml
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import top.xjunz.tasker.BuildConfig
import top.xjunz.tasker.R
import top.xjunz.tasker.app
import top.xjunz.tasker.ktx.format

@Serializable
data class UpdateInfo(
    val binary: Binary,
    val build: String,
    val changelog: String,
    val direct_install_url: String,
    val installUrl: String,
    val install_url: String,
    val name: String,
    val update_url: String,
    val updated_at: Long,
    val version: String,
    val versionShort: String
) {
    @Serializable
    data class Binary(
        @SerialName("fsize") val size: Long
    )

    fun hasUpdates(): Boolean {
        return (build.toIntOrNull() ?: -1) > BuildConfig.VERSION_CODE
    }

    fun formatToString(): CharSequence {
        return R.string.html_updates_info.format(
            versionShort,
            Formatter.formatFileSize(app, binary.size),
            changelog.replace("\n", "<br>")
        ).parseAsHtml()
    }
}