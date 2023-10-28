/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option

import android.annotation.SuppressLint
import android.view.View
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import top.xjunz.shared.ktx.arrayMapOf
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.R
import top.xjunz.tasker.app
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.util.isAttached
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.task.applet.option.descriptor.ArgumentDescriptor
import top.xjunz.tasker.task.applet.option.descriptor.ValueDescriptor
import top.xjunz.tasker.task.applet.value.VariantArgType
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

        private val DEFAULT_SINGLE_VALUE_DESCRIBER: (Applet?, Map<Int, Any>) -> CharSequence? =
            { _, values ->
                if (values.isEmpty()) {
                    null
                } else {
                    val value = values.values.single()
                    if (value is Boolean) {
                        if (value) R.string._true.text else R.string._false.text
                    } else {
                        value.toString()
                    }
                }
            }

        private val DEFAULT_RANGE_FORMATTER: (Applet?, Map<Int, Any>) -> CharSequence? =
            { _, values ->
                if (values.isEmpty()) {
                    null
                } else {
                    val range = values.values.single()
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

    /**
     * Tell which type of option should be accepted
     */
    fun withScopeRegistryId(registryId: Int): AppletOption {
        scopeRegistryId = registryId
        return this
    }

    var name: String? = null

    private var describer: (Applet?, Map<Int, Any>) -> CharSequence? =
        DEFAULT_SINGLE_VALUE_DESCRIBER

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

    private var presetsNameRes: Int = -1

    private var presetsValueRes: Int = -1

    val hasPresets get() = presetsNameRes != -1 && presetsValueRes != -1

    var descAsTitle: Boolean = false
        private set

    private var isTitleComposite: Boolean = false

    var isShizukuOnly = false
        private set

    var isPremiumOnly = false
        private set

    var arguments: List<ArgumentDescriptor> = Collections.emptyList()

    var results: List<ValueDescriptor> = Collections.emptyList()

    val rawTitle: CharSequence?
        get() = if (titleResource == TITLE_NONE) null else titleResource.text

    private var titleModifierRes: Int = -1

    var titleModifier: String? = null
        get() = if (titleModifierRes != -1) titleModifierRes.str else field
        private set

    var expectSingleValue = false

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

    fun matchReferents(argument: ArgumentDescriptor): List<ValueDescriptor> {
        val shouldIgnoreVariantType =
            VariantArgType.shouldIgnoreVariantTypeWhenMatching(argument.variantValueType)
        val argVariantType =
            if (shouldIgnoreVariantType) VariantArgType.NONE else argument.variantValueType
        val type = argument.referenceType ?: argument.valueType
        return results.filter {
            if (it.isCollection != argument.isCollection) {
                false
            } else {
                if (it.valueType != type) {
                    false
                } else {
                    argVariantType == VariantArgType.NONE || argVariantType == it.variantValueType
                }
            }
        }
    }

    private fun loadTitle(applet: Applet?, isInverted: Boolean): CharSequence? {
        if (isTitleComposite) {
            return composeTitle(applet, isInverted)
        }
        return if (isInverted) invertedTitle else rawTitle
    }

    fun loadSpannedTitle(applet: Applet): CharSequence? {
        return loadTitle(applet, applet.isInverted)
    }

    fun loadUnspannedTitle(applet: Applet?): CharSequence? {
        return loadTitle(
            null, if (applet == null || !applet.isAttached) isInverted else applet.isInverted
        )
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

    fun describe(applet: Applet): CharSequence? = describer(applet, applet.values)

    fun toggleInversion() {
        isInverted = !isInverted
    }

    fun withDefaultRangeDescriber(): AppletOption {
        describer = DEFAULT_RANGE_FORMATTER
        return this
    }

    fun yieldWithFirstValue(value: Any): Applet {
        return yield(0 to value)
    }

    fun yieldCriterion(inverted: Boolean): Applet {
        return yield().also { it.isInverted = inverted }
    }

    fun yield(vararg initialValues: Pair<Int, Any>): Applet {
        check(isValid) {
            "Invalid applet option unable to yield an applet!"
        }
        return rawCreateApplet().also {
            it.id = registryId shl 16 or appletId
            it.name = name
            it.isInverted = isInverted
            it.isInvertible = isInvertible
            if (initialValues.isNotEmpty()) {
                it.values = arrayMapOf(*initialValues)
            }
            it.argumentTypes = IntArray(arguments.size) { index ->
                val arg = arguments[index]
                if (arg.isReferenceOnly) {
                    Applet.ARG_TYPE_REFERENCE
                } else {
                    val type = if (arg.variantValueType == VariantArgType.NONE) {
                        Applet.judgeValueType(arg.valueType)
                    } else {
                        VariantArgType.getRawType(arg.variantValueType)
                    }
                    if (arg.isCollection) Applet.collectionTypeOf(type) else type
                }
            }
        }
    }

    fun <T : Any> withSingleValueDescriber(block: (T) -> CharSequence): AppletOption {
        expectSingleValue = true
        describer = { _, values ->
            if (values.isEmpty()) {
                null
            } else {
                block(values.values.single().casted())
            }
        }
        return this
    }

    fun <T : Any> withSingleValueAppletDescriber(block: (Applet, T?) -> CharSequence?): AppletOption {
        expectSingleValue = true
        describer = { applet, values ->
            if (applet == null) {
                null
            } else {
                if (values.isEmpty()) {
                    null
                } else {
                    block(applet, values.values.single().casted())
                }
            }
        }
        return this
    }

    fun withValuesDescriber(block: (Applet, values: Map<Int, Any>) -> CharSequence?): AppletOption {
        describer = { applet, values ->
            if (applet == null) {
                null
            } else {
                block(applet, values)
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
            val value = applet.values[index]
            val ref = applet.references[index]
            val sub = when {
                arg.isReferenceOnly -> makeReferenceText(applet, ref) ?: arg.substitution
                arg.isValueOnly -> arg.substitution
                else -> when {
                    ref == null -> arg.substitution
                    value == null -> makeReferenceText(applet, ref)!!
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
        variantValueType: Int = VariantArgType.NONE,
        isCollection: Boolean = false
    ): AppletOption {
        requireResults().add(ValueDescriptor(name, T::class.java, variantValueType, isCollection))
        return this
    }

    /**
     * An argument whose value and ref type are of the same type.
     */
    inline fun <reified V> withUnaryArgument(
        @StringRes name: Int,
        @StringRes substitution: Int = -1,
        variantValueType: Int = VariantArgType.NONE,
        isRef: Boolean? = null,
        isCollection: Boolean = false,
    ): AppletOption {
        return withArgument(
            name,
            substitution,
            V::class.java,
            null,
            variantValueType,
            isRef,
            isCollection
        )
    }

    fun withArgument(
        @StringRes name: Int,
        @StringRes substitution: Int,
        valueType: Class<*>,
        refType: Class<*>?,
        variantValueType: Int,
        isRef: Boolean?,
        isCollection: Boolean
    ): AppletOption {
        if (arguments == Collections.EMPTY_LIST) arguments = mutableListOf()
        (arguments as MutableList<ArgumentDescriptor>).add(
            ArgumentDescriptor(
                name,
                substitution,
                valueType,
                refType,
                variantValueType,
                isRef,
                isCollection
            )
        )
        return this
    }

    inline fun <reified V> withValueArgument(
        @StringRes name: Int,
        variantValueType: Int = VariantArgType.NONE,
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

    inline fun <reified V> withAnonymousSingleValueArgument(
        variantValueType: Int = VariantArgType.NONE,
        isCollection: Boolean = false
    ): AppletOption {
        check(arguments.isEmpty()) {
            "Only one anonymous value is allowed!"
        }
        return withUnaryArgument<V>(-1, -1, variantValueType, false, isCollection)
    }

    /**
     * An argument which is a reference.
     */
    inline fun <reified Ref> withRefArgument(
        @StringRes name: Int,
        @StringRes substitution: Int = -1,
        variantValueType: Int = VariantArgType.NONE,
        isCollection: Boolean = false
    ): AppletOption {
        return withUnaryArgument<Ref>(name, substitution, variantValueType, true, isCollection)
    }

    inline fun <reified Ref, reified V> withBinaryArgument(
        @StringRes name: Int,
        @StringRes substitution: Int = -1,
        variantValueType: Int = VariantArgType.NONE,
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

    /**
     * Make this currently last argument as a result, which means other applets could refer to this
     * argument.
     *
     * @see ArgumentDescriptor.asResult
     */
    fun thisArgAsResult(): AppletOption {
        check(arguments.isNotEmpty())
        requireResults().add(arguments.last().asResult())
        return this
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