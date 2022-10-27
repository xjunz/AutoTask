package top.xjunz.tasker.task.applet.option

import android.annotation.SuppressLint
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import androidx.annotation.ArrayRes
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.R
import top.xjunz.tasker.app
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.ktx.text
import top.xjunz.tasker.ui.ColorSchemes
import top.xjunz.tasker.util.unsupportedOperation

/**
 * The entity describing an applet's information. You can call [yieldApplet] to create an [Applet]
 * as per the option.
 *
 * @author xjunz 2022/09/22
 */
abstract class AppletOption(
    val appletId: Int, private val titleRes: Int, private val invertedTitleRes: Int
) : Comparable<AppletOption> {

    companion object {
        /**
         * Indicate that the inverted title of an option is auto-generated.
         *
         * @see AppletOption.invertedTitle
         */
        const val TITLE_AUTO_INVERTED = 0

        const val TITLE_NONE = -1

        private val DEFAULT_DESCRIBER: (Any) -> CharSequence = {
            if (it is Boolean) {
                if (it) R.string._true.text else R.string._false.text
            } else {
                it.toString()
            }
        }

        private fun makeSpannedLabel(label: CharSequence): CharSequence {
            val index = label.indexOf('(')
            if (index > -1) {
                return SpannableStringBuilder().append(label.substring(0, index)).append(
                    label.substring(index),
                    ForegroundColorSpan(ColorSchemes.textColorDisabled),
                    SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            return label
        }
    }

    private var describer: (Any) -> CharSequence = DEFAULT_DESCRIBER

    /**
     * Whether this is an valid option able to yield an applet.
     */
    val isValid = appletId != -1

    /**
     * The id of the factory where the option is manufactured.
     */
    var factoryId: Int = -1

    /**
     * The category id identifying the option's category and its position in the category.
     *
     * @see AppletOptionRegistry
     */
    var categoryId: Int = -1

    /**
     * As per [Applet.isInverted].
     */
    var isInverted = false

    /**
     * As per [Applet.isInvertible].
     */
    val isInvertible get() = invertedTitleRes != TITLE_NONE

    /**
     * As per [Applet.value].
     */
    var value: Any? = null

    /**
     * The index in all categories.
     */
    val categoryIndex: Int get() = categoryId ushr 8

    var presetsNameRes: Int = -1

    var presetsValueRes: Int = -1

    val hasPresets get() = presetsNameRes != -1 && presetsValueRes != -1

    val title: CharSequence?
        get() = if (titleRes == TITLE_NONE) null else makeSpannedLabel(titleRes.text)

    private val invertedTitleResource: Int by lazy {
        @SuppressLint("DiscouragedApi")
        when (invertedTitleRes) {
            TITLE_AUTO_INVERTED -> {
                val invertedResName = "not_" + app.resources.getResourceEntryName(titleRes)
                val id = app.resources.getIdentifier(invertedResName, "string", app.packageName)
                check(id != 0) { "Resource id 'R.string.$invertedResName' not found!" }
                id
            }
            TITLE_NONE -> TITLE_NONE
            else -> invertedTitleRes
        }
    }

    val invertedTitle: CharSequence?
        get() = if (invertedTitleResource == TITLE_NONE) null else makeSpannedLabel(
            invertedTitleResource.text
        )

    val currentTitle get() = getTitle(isInverted)

    fun getTitle(isInverted: Boolean): CharSequence? {
        return if (isInverted) invertedTitle else title
    }

    fun withValue(value: Any?): AppletOption {
        this.value = value
        return this
    }

    fun describe(value: Any?): CharSequence? = if (value == null) null else describer(value)

    val description: CharSequence?
        get() = if (value == null) null else describe(value!!)

    fun toggleInverted() {
        isInverted = !isInverted
    }

    fun yieldApplet(): Applet {
        check(isValid) {
            "Invalid applet option unable to yield an applet!"
        }
        return rawCreateApplet().also {
            it.id = factoryId shl 16 or appletId
            it.isInverted = isInverted
            it.isInvertible = isInvertible
            it.value = value
        }
    }

    fun <T : Any> withDescriber(block: (T) -> CharSequence): AppletOption {
        describer = { block(it.casted()) }
        return this
    }

    /**
     * Only for text type options. Set its preset text array resource.
     */
    fun withPresetArray(@ArrayRes nameRes: Int, @ArrayRes valueRes: Int): AppletOption {
        presetsNameRes = nameRes
        presetsValueRes = valueRes
        return this
    }

    protected abstract fun rawCreateApplet(): Applet

    override fun compareTo(other: AppletOption): Int {
        check(factoryId == other.factoryId) {
            "Only applets with the same factory id are comparable!"
        }
        check(categoryId > -1)
        return categoryId.compareTo(other.categoryId)
    }

    fun isOptionOf(applet: Applet): Boolean {
        return factoryId shl 16 or appletId == applet.id
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppletOption

        if (appletId != other.appletId) return false
        if (factoryId != other.factoryId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = appletId
        result = 31 * result + factoryId
        return result
    }

    /**
     * Clone the option clean (without cloning late init variables, such as [value]).
     */
    fun cloned(): AppletOption {
        return AppletOption(appletId, titleRes, invertedTitleRes, ::yieldApplet)
            .withDescriber(describer).also {
                it.factoryId = factoryId
            }
    }
}

fun AppletCategoryOption(label: Int): AppletOption {
    return AppletOption(-1, label, AppletOption.TITLE_NONE) {
        unsupportedOperation()
    }
}

fun AppletOption(
    appletId: Int,
    title: Int,
    invertedTitle: Int = AppletOption.TITLE_AUTO_INVERTED,
    creator: () -> Applet
): AppletOption {
    return object : AppletOption(appletId, title, invertedTitle) {
        override fun rawCreateApplet(): Applet {
            return creator.invoke()
        }
    }
}

fun NotInvertibleAppletOption(appletId: Int, title: Int, creator: () -> Applet): AppletOption {
    return object : AppletOption(appletId, title, TITLE_NONE) {
        override fun rawCreateApplet(): Applet {
            return creator.invoke()
        }
    }
}