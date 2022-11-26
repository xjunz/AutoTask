package top.xjunz.tasker.task.applet.option

import android.annotation.SuppressLint
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.R
import top.xjunz.tasker.app
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.ui.ColorSchemes
import java.util.*

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

        private val DEFAULT_DESCRIBER: (Applet?, Any?) -> CharSequence? = { _, value ->
            if (value is Boolean) {
                if (value) R.string._true.text else R.string._false.text
            } else {
                value?.toString()
            }
        }

        private fun makeLabelSpan(label: CharSequence): CharSequence {
            val index = label.indexOf('(')
            if (index > -1) {
                return label.subSequence(0, index) + label.substring(index)
                    .foreColored(ColorSchemes.textColorDisabled)
            }
            return label
        }

        fun makeRelationSpan(
            origin: CharSequence,
            isAnd: Boolean,
            isCriterion: Boolean
        ): CharSequence {
            val relation = if (isCriterion) {
                if (isAnd) R.string._and.str else R.string._or.str
            } else {
                if (isAnd) R.string.on_success.str else R.string.on_failure.str
            }
            return relation.foreColored().underlined().bold() + origin
        }

    }

    private var describer: (Applet?, Any?) -> CharSequence? = DEFAULT_DESCRIBER

    private var helpTextRes: Int = -1

    var helpText: CharSequence? = null
        get() = if (helpTextRes != -1) helpTextRes.text else field

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
    private var isInverted = false

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

    var isComplexTitle: Boolean = false

    var isShizukuOnly = false

    var isA11yOnly = false

    var arguments: List<ValueDescriptor> = emptyList()

    var results: List<ValueDescriptor> = emptyList()

    val rawTitle: CharSequence?
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

    val currentTitle get() = getTitle(null, isInverted)

    private fun getTitle(applet: Applet?, isInverted: Boolean): CharSequence? {
        if (isComplexTitle) {
            return getComplexTitle(applet)
        }
        return if (isInverted) invertedTitle else rawTitle
    }

    fun getTitle(applet: Applet): CharSequence? {
        return getTitle(applet, applet.isInverted)
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

    fun describe(applet: Applet): CharSequence? = describer(applet, applet.value)

    val rawDescription: CharSequence?
        get() = if (value == null) null else describer(null, value!!)

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

    fun <T : Any> withValueDescriber(block: (T) -> CharSequence): AppletOption {
        describer = { _, value ->
            if (value == null) {
                null
            } else {
                block(value.casted())
            }
        }
        return this
    }

    fun <T : Any> withDescriber(block: (Applet, T?) -> CharSequence?): AppletOption {
        describer = { applet, value ->
            if (applet == null) {
                null
            } else {
                block(applet, value?.casted())
            }
        }
        return this
    }

    private fun getComplexTitle(applet: Applet?): CharSequence {
        if (applet == null) {
            return titleRes.format(*Array(arguments.size) {
                arguments[it].name
            })
        }
        val split = titleRes.str.split("%s")
        var title: CharSequence = split[0]
        for (i in 1..split.lastIndex) {
            val s = split[i]
            val index = i - 1
            val arg = arguments[index]
            val refid = applet.referring[index]
            val rep = when {
                arg.isReferenceOnly -> refid?.foreColored() ?: arg.name
                arg.isValueOnly -> arg.name
                else -> when {
                    value == null && refid == null -> arg.name
                    value == null && refid != null -> refid.foreColored()
                    refid == null -> arg.name
                    else -> error("Value and reference both specified!")
                }
            }
            title += rep + s
        }
        return title
    }

    fun hasCompositeTitle(): AppletOption {
        isComplexTitle = true
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

    inline fun <reified T> withResult(@StringRes name: Int): AppletOption {
        if (results == Collections.EMPTY_LIST) results = mutableListOf()
        (results as MutableList<ValueDescriptor>).add(ValueDescriptor(name, T::class.java, true))
        return this
    }

    inline fun <reified T> withArgument(
        @StringRes name: Int,
        isRef: Boolean? = null
    ): AppletOption {
        if (arguments == Collections.EMPTY_LIST) arguments = mutableListOf()
        (arguments as MutableList<ValueDescriptor>).add(ValueDescriptor(name, T::class.java, isRef))
        return this
    }

    /**
     * Describe [Applet.value] as an argument.
     */
    inline fun <reified T> withValueArgument(@StringRes name: Int): AppletOption {
        return withArgument<T>(name, false)
    }

    inline fun <reified T> withRefArgument(@StringRes name: Int): AppletOption {
        return withArgument<T>(name, true)
    }

    fun withHelperText(@StringRes res: Int): AppletOption {
        helpTextRes = res
        helpText = null
        return this
    }

    fun withHelperText(text: CharSequence): AppletOption {
        helpText = text
        helpTextRes = -1
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