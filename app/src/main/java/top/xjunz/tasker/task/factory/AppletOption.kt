package top.xjunz.tasker.task.factory

import android.annotation.SuppressLint
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import androidx.annotation.IntRange
import top.xjunz.tasker.app
import top.xjunz.tasker.colorSchemes
import top.xjunz.tasker.engine.flow.Applet
import top.xjunz.tasker.ktx.text
import top.xjunz.tasker.util.unsupportedOperation

/**
 * @author xjunz 2022/09/22
 */
abstract class AppletOption(
    val appletId: Int, private val labelRes: Int, private val invertedLabelRes: Int
) : Comparable<AppletOption> {

    val isValid = appletId != -1

    var factoryId: Int = -1

    var isInverted = false

    var categoryId: Int = -1

    @get:IntRange(from = 0)
    val categoryIndex: Int
        get() = categoryId ushr 8

    val label: CharSequence? by lazy {
        if (labelRes == AppletFactory.LABEL_NONE) null else makeSpannedLabel(labelRes.text)
    }

    val invertedLabel: CharSequence? by lazy {
        @SuppressLint("DiscouragedApi")
        when (invertedLabelRes) {
            AppletFactory.LABEL_AUTO_INVERTED -> {
                val invertedResName = "not_" + app.resources.getResourceEntryName(labelRes)
                val id = app.resources.getIdentifier(invertedResName, "string", app.packageName)
                check(id != 0) { "Resource id 'R.string.$invertedResName' not found!" }
                makeSpannedLabel(id.text)
            }
            AppletFactory.LABEL_NONE -> null
            else -> makeSpannedLabel(invertedLabelRes.text)
        }
    }

    private fun makeSpannedLabel(label: CharSequence): CharSequence {
        val index = label.indexOf('(')
        if (index > -1) {
            return SpannableStringBuilder().append(label.substring(0, index)).append(
                label.substring(index),
                ForegroundColorSpan(colorSchemes.textColorSecondaryNoDisable),
                SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return label
    }

    fun toggleInverted() {
        isInverted = !isInverted
    }

    fun createApplet(): Applet {
        return rawCreateApplet().also {
            it.id = factoryId shl 16 or appletId
            it.isInverted = isInverted
        }
    }

    protected abstract fun rawCreateApplet(): Applet

    val isInvertible get() = invertedLabelRes != AppletFactory.LABEL_NONE

    override fun compareTo(other: AppletOption): Int {
        check(factoryId == other.factoryId) {
            "Only applets with the same factory id are comparable!"
        }
        check(categoryId > -1)
        return categoryId.compareTo(other.categoryId)
    }
}

fun AppletCategoryOption(label: Int): AppletOption {
    return AppletOption(-1, label, AppletFactory.LABEL_NONE) {
        unsupportedOperation()
    }
}

fun AppletOption(
    appletId: Int,
    labelRes: Int,
    invertedLabelRes: Int = AppletFactory.LABEL_AUTO_INVERTED,
    creator: () -> Applet
): AppletOption {
    return object : AppletOption(appletId, labelRes, invertedLabelRes) {
        override fun rawCreateApplet(): Applet {
            return creator.invoke()
        }
    }
}

fun NotInvertibleAppletOption(
    appletId: Int,
    labelRes: Int,
    creator: () -> Applet
): AppletOption {
    return object : AppletOption(appletId, labelRes, AppletFactory.LABEL_NONE) {
        override fun rawCreateApplet(): Applet {
            return creator.invoke()
        }
    }
}