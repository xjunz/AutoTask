/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option

import android.annotation.SuppressLint
import android.view.View
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.R
import top.xjunz.tasker.app
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.util.isAttached
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.task.applet.option.descriptor.ArgumentDescriptor
import top.xjunz.tasker.task.applet.option.descriptor.ValueDescriptor
import top.xjunz.tasker.ui.main.EventCenter
import java.util.*

/**
 * The entity describing an applet's information. You can call [yield] to create an [Applet]
 * as per the option.
 *
 * @author xjunz 2022/09/22
 */
class AppletOption(
    val registryId: Int,
    private val titleResource: Int,
    private val invertedTitleRes: Int,
    private inline val rawCreateApplet: () -> Applet
) : Comparable<AppletOption> {

    companion object {

        var deliveringEvent: String? = null

        const val EVENT_TOGGLE_RELATION = "applet.option.event.TOGGLE_REL"
        const val EVENT_NAVIGATE_REFERENCE = "applet.option.event.NAVI_REF"
        const val EVENT_EDIT_VALUE = "applet.option.event.EDIT_VAL"

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

        private val DEFAULT_RANGE_FORMATTER: (Applet?, Any?) -> CharSequence? = { _, range ->
            range as Collection<*>
            check(range.size == 2)
            val first = range.firstOrNull()
            val last = range.lastOrNull()
            check(first != null || last != null)
            if (first == last) {
                first.toString()
            } else if (first == null) {
                R.string.format_less_than.format(last)
            } else if (last == null) {
                R.string.format_larger_than.format(first)
            } else {
                R.string.format_range.format(first, last)
            }
        }

        fun deliverEvent(view: View, action: String, value: Any) {
            deliveringEvent = action
            view.post {
                deliveringEvent = null
            }
            EventCenter.sendEvent(action, value)
        }

        fun makeRelationSpan(origin: CharSequence, applet: Applet): CharSequence {
            val relation = if (applet.supportsAnywayRelation) {
                if (applet.isAnd) R.string.then.str
                else if (applet.isOr) R.string._else.str
                else R.string.anyway.str
            } else {
                if (applet.isAnd) R.string._and.str else R.string._or.str
            }
            return relation.clickable {
                deliverEvent(it, EVENT_TOGGLE_RELATION, applet)
            }.bold().underlined() + origin
        }

        private fun makeReferenceText(applet: Applet, name: CharSequence?): CharSequence? {
            if (name == null) return null
            return name.clickable {
                deliverEvent(it, EVENT_NAVIGATE_REFERENCE, name to applet)
            }.foreColored().backColored().underlined()
        }

        fun makeAppletIdentifier(registryId: Int, appletId: Int): Int {
            return registryId shl 16 or appletId
        }

    }

    var appletId: Int = -1
        set(value) {
            check(value == field || field == -1) {
                "appletId is already set. Pls do not set again!"
            }
            field = value
        }

    var scopeRegistryId: Int = -1
        private set

    fun withScopeRegistryId(registryId: Int): AppletOption {
        scopeRegistryId = registryId
        return this
    }

    private var describer: (Applet?, Any?) -> CharSequence? = DEFAULT_DESCRIBER

    private var helpTextRes: Int = -1

    var helpText: CharSequence? = null
        get() = if (helpTextRes != -1) helpTextRes.text else field
        private set

    /**
     * Whether this is an valid option able to yield an applet.
     */
    val isValid get() = appletId != -1

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

    var minApiLevel: Int = -1

    /**
     * The index in all categories.
     */
    val categoryIndex: Int get() = categoryId ushr 8

    var presetsNameRes: Int = -1
        private set

    var presetsValueRes: Int = -1
        private set

    val hasPresets get() = presetsNameRes != -1 && presetsValueRes != -1

    var descAsTitle: Boolean = false
        private set

    var isTitleComposite: Boolean = false
        private set

    var isShizukuOnly = false
        private set

    var isPremiumOnly = false
        private set

    var arguments: List<ArgumentDescriptor> = emptyList()

    var results: List<ValueDescriptor> = emptyList()

    val rawTitle: CharSequence?
        get() = if (titleResource == TITLE_NONE) null else titleResource.text

    /**
     * The applet's value is innate and hence not modifiable.
     */
    var isValueInnate: Boolean = false
        private set

    private var titleModifierRes: Int = -1

    var titleModifier: String? = null
        get() = if (titleModifierRes != -1) titleModifierRes.str else field
        private set

    private val invertedTitleResource: Int by lazy {
        @SuppressLint("DiscouragedApi")
        when (invertedTitleRes) {
            TITLE_AUTO_INVERTED -> {
                val invertedResName = "not_" + app.resources.getResourceEntryName(titleResource)
                val id = app.resources.getIdentifier(invertedResName, "string", app.packageName)
                check(id != 0) { "Resource id 'R.string.$invertedResName' not found!" }
                id
            }
            TITLE_NONE -> TITLE_NONE
            else -> invertedTitleRes
        }
    }

    private val invertedTitle: CharSequence?
        get() = if (invertedTitleResource == TITLE_NONE) null else invertedTitleResource.text

    /**
     * Non-spanned title considering inversion status of the [applet].
     */
    fun loadDummyTitle(applet: Applet?): CharSequence? {
        return loadTitle(
            null, if (applet == null || !applet.isAttached) isInverted else applet.isInverted
        )
    }

    fun findResults(argument: ArgumentDescriptor): List<ValueDescriptor> {
        return results.filter {
            if (argument.referenceClass == null) {
                it.valueClass == argument.valueClass && it.variantValueType == argument.variantValueType
            } else {
                it.valueClass == argument.referenceClass
            }
        }
    }

    private fun loadTitle(applet: Applet?, isInverted: Boolean): CharSequence? {
        if (isTitleComposite) {
            return composeTitle(applet, isInverted)
        }
        return if (isInverted) invertedTitle else rawTitle
    }

    fun loadTitle(applet: Applet): CharSequence? {
        return loadTitle(applet, applet.isInverted)
    }

    fun shizukuOnly(): AppletOption {
        isShizukuOnly = true
        return this
    }

    fun premiumOnly(): AppletOption {
        isPremiumOnly = true
        return this
    }

    fun descAsTitle(): AppletOption {
        descAsTitle = true
        return this
    }

    fun hasInnateValue(): AppletOption {
        isValueInnate = true
        return this
    }

    fun describe(applet: Applet): CharSequence? = describer(applet, applet.value)

    fun toggleInversion() {
        isInverted = !isInverted
    }

    fun withDefaultRangeDescriber(): AppletOption {
        describer = DEFAULT_RANGE_FORMATTER
        return this
    }

    fun yield(initialValue: Any? = null): Applet {
        check(isValid) {
            "Invalid applet option unable to yield an applet!"
        }
        return rawCreateApplet().also {
            it.id = makeAppletIdentifier(registryId, appletId)
            it.isInverted = isInverted
            it.isInvertible = isInvertible
            if (!isValueInnate) {
                if (initialValue != null) it.value = initialValue else it.value = it.defaultValue
            }
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

    private fun composeTitle(applet: Applet?, isInverted: Boolean): CharSequence {
        val res = if (isInverted) invertedTitleResource else titleResource
        if (applet == null) {
            return res.format(*Array(arguments.size) {
                arguments[it].substitution
            })
        }
        val split = res.str.split("%s")
        var title: CharSequence = split[0]
        for (i in 1..split.lastIndex) {
            val s = split[i]
            val index = i - 1
            val arg = arguments[index]
            val ref = applet.references[index]
            val sub = when {
                arg.isReferenceOnly -> makeReferenceText(applet, ref) ?: arg.substitution
                arg.isValueOnly -> arg.substitution
                else -> when {
                    applet.value == null && ref == null -> arg.substitution
                    applet.value == null && ref != null -> makeReferenceText(applet, ref)!!
                    ref == null -> arg.substitution
                    else -> error("Value and reference both specified!")
                }
            }
            title += sub + s
        }
        return title
    }

    fun restrictApiLevel(minApiLevel: Int): AppletOption {
        this.minApiLevel = minApiLevel
        return this
    }

    fun hasCompositeTitle(): AppletOption {
        isTitleComposite = true
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

    fun requireResults(): MutableList<ValueDescriptor> {
        if (results == Collections.EMPTY_LIST) results = mutableListOf()
        return (results as MutableList<ValueDescriptor>)
    }

    inline fun <reified T> withResult(
        @StringRes name: Int,
        variantType: Int = -1,
        isCollection: Boolean = false
    ): AppletOption {
        requireResults().add(ValueDescriptor(name, T::class.java, variantType, isCollection))
        return this
    }

    /**
     * An argument whose value and ref type are of the same type.
     */
    inline fun <reified V> withUnaryArgument(
        @StringRes name: Int,
        @StringRes substitution: Int = -1,
        variantType: Int = -1,
        isRef: Boolean? = null,
        isCollection: Boolean = false,
    ): AppletOption {
        return withArgument(
            name,
            substitution,
            V::class.java,
            null,
            variantType,
            isRef,
            isCollection
        )
    }

    /**
     * An argument whose value and argument are of different types.
     */
    inline fun <reified V, reified Ref> withBinaryArgument(
        @StringRes name: Int,
        @StringRes substitution: Int = -1,
        variantValueType: Int = -1,
        isCollection: Boolean = false,
    ): AppletOption {
        return withArgument(
            name,
            substitution,
            V::class.java,
            Ref::class.java,
            variantValueType,
            null,
            isCollection
        )
    }

    fun withArgument(
        @StringRes name: Int,
        @StringRes substitution: Int,
        valueType: Class<*>,
        referenceType: Class<*>?,
        variantType: Int,
        isRef: Boolean?,
        isCollection: Boolean
    ): AppletOption {
        if (arguments == Collections.EMPTY_LIST) arguments = mutableListOf()
        (arguments as MutableList<ArgumentDescriptor>).add(
            ArgumentDescriptor(
                name,
                substitution,
                valueType,
                referenceType,
                variantType,
                isRef,
                isCollection
            )
        )
        return this
    }

    /**
     * Describe [Applet.value] as an argument.
     */
    inline fun <reified V> withValueArgument(
        @StringRes name: Int,
        variantValueType: Int = -1,
        isCollection: Boolean = false
    ): AppletOption {
        return withUnaryArgument<V>(
            name,
            substitution = -1,
            variantValueType,
            isRef = false,
            isCollection
        )
    }

    /**
     * An argument which is a reference.
     */
    inline fun <reified Ref> withRefArgument(
        @StringRes name: Int,
        @StringRes substitution: Int = -1,
        variantValueType: Int = -1,
        isCollection: Boolean = false,
    ): AppletOption {
        return withUnaryArgument<Ref>(name, substitution, variantValueType, true, isCollection)
    }

    fun withHelperText(@StringRes res: Int): AppletOption {
        helpTextRes = res
        helpText = null
        return this
    }

    fun withTitleModifier(@StringRes res: Int): AppletOption {
        titleModifierRes = res
        return this
    }

    fun withTitleModifier(text: String): AppletOption {
        titleModifier = text
        titleModifierRes = -1
        return this
    }

    fun withHelperText(text: CharSequence): AppletOption {
        helpText = text
        helpTextRes = -1
        return this
    }

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
}