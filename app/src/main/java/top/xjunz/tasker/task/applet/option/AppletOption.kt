package top.xjunz.tasker.task.applet.option

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import androidx.annotation.ArrayRes
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.R
import top.xjunz.tasker.app
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.ktx.text
import top.xjunz.tasker.ui.ColorSchemes

/**
 * The entity describing an applet's information. You can call [yieldApplet] to create an [Applet]
 * as per the option.
 *
 * @author xjunz 2022/09/22
 */
abstract class AppletOption(
    val appletId: Int,
    val registryId: Int,
    private val titleRes: Int,
    private val invertedTitleRes: Int
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

        private fun makeLabelSpan(label: CharSequence): CharSequence {
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

        fun makeRelationSpan(origin: CharSequence, isAnd: Boolean): CharSequence {
            val relationText = if (isAnd) R.string._and.text else R.string._or.text
            return SpannableStringBuilder().append(
                relationText,
                ForegroundColorSpan(ColorSchemes.colorPrimary),
                SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
            ).append(origin).also {
                it.setSpan(
                    UnderlineSpan(), 0, 1,
                    SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                it.setSpan(
                    StyleSpan(Typeface.BOLD), 0, 1,
                    SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

        }

    }

    private var describer: (Any) -> CharSequence = DEFAULT_DESCRIBER

    /**
     * Whether this is an valid option able to yield an applet.
     */
    val isValid = appletId != -1

    /**
     * The category id identifying the option's category and its position in the category.
     *
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

    var descAsTitle: Boolean = false

    var isShizukuOnly = false

    var isA11yOnly = false

    var arguments: Array<ReferenceDescriptor> = emptyArray()

    var results: Array<ReferenceDescriptor> = emptyArray()

    val title: CharSequence?
        get() = if (titleRes == TITLE_NONE) null else makeLabelSpan(titleRes.text)

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
        get() = if (invertedTitleResource == TITLE_NONE) null else makeLabelSpan(
            invertedTitleResource.text
        )

    val currentTitle get() = getTitle(isInverted)

    fun getTitle(isInverted: Boolean): CharSequence? {
        return if (isInverted) invertedTitle else title
    }

    fun shizukuOnly(): AppletOption {
        isShizukuOnly = true
        return this
    }

    fun descAsTitle(): AppletOption {
        descAsTitle = true
        return this
    }

    fun withValue(value: Any?): AppletOption {
        this.value = value
        return this
    }

    fun describe(value: Any?): CharSequence? = if (value == null) null else describer(value)

    val description: CharSequence?
        get() = if (value == null) null else describe(value!!)

    fun toggleInversion() {
        isInverted = !isInverted
    }

    fun yieldApplet(): Applet {
        check(isValid) {
            "Invalid applet option unable to yield an applet!"
        }
        return rawCreateApplet().also {
            it.id = registryId shl 16 or appletId
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

    fun withResults(vararg pairs: Pair<Int, Class<*>>): AppletOption {
        check(pairs.isNotEmpty())
        results = Array(pairs.size) {
            ReferenceDescriptor(pairs[it].first, pairs[it].second)
        }
        return this
    }

    fun withArguments(vararg pairs: Pair<Int, Class<*>>): AppletOption {
        check(pairs.isNotEmpty())
        arguments = Array(pairs.size) {
            ReferenceDescriptor(pairs[it].first, pairs[it].second)
        }
        return this
    }

    protected abstract fun rawCreateApplet(): Applet

    override fun compareTo(other: AppletOption): Int {
        check(registryId == other.registryId) {
            "Only applets with the same factory id are comparable!"
        }
        check(categoryId > -1)
        return categoryId.compareTo(other.categoryId)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppletOption

        if (appletId != other.appletId) return false
        if (registryId != other.registryId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = appletId
        result = 31 * result + registryId
        return result
    }

    /**
     * Clone the option clean (without cloning late init variables, such as [value]).
     */
    fun cloned(): AppletOption {
        return object : AppletOption(appletId, registryId, titleRes, invertedTitleRes) {
            override fun rawCreateApplet(): Applet {
                return yieldApplet()
            }
        }.withDescriber(describer)
    }
}