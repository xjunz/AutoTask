/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.base

import androidx.annotation.CheckResult
import top.xjunz.shared.utils.illegalArgument
import top.xjunz.shared.utils.unsupportedOperation
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * The base executable element of a [Flow].
 *
 * @author xjunz 2022/08/04
 */
abstract class Applet {

    companion object {

        const val NO_ID = -1

        /**
         * The bit count used to store the applet index in a flow. We use a `LONG` to track depths
         * and indexes. Considering the race between the max depth and the max index, we choose 7
         * as the bit count to store indexes. Therefore, the max nested depth allowed in a flow is
         * limited to 9 `(64/7)` and the max child count of a flow is 127 `(2^7-1)`. Also there is
         * only 1 `(64%7)` bit remaining unused.
         *
         * @see MAX_FLOW_CHILD_COUNT
         * @see TaskRuntime
         */
        const val FLOW_CHILD_COUNT_BITS = 7

        /**
         * The max child count allowed in a flow. We need to minus 1 which is preserved.
         */
        const val MAX_FLOW_CHILD_COUNT = (1 shl FLOW_CHILD_COUNT_BITS) - 1

        /**
         * The max nested depth allowed in the root flow.
         */
        const val MAX_FLOW_NESTED_DEPTH = Long.SIZE_BITS / FLOW_CHILD_COUNT_BITS

        const val MAX_REFERENCE_ID_LENGTH = 12

        const val VAL_TYPE_IRRELEVANT = 0
        const val VAL_TYPE_TEXT = 1

        @Deprecated("Unsupported! Use [Applet.isInverted] to control boolean value.")
        const val VAL_TYPE_BOOL = VAL_TYPE_IRRELEVANT
        const val VAL_TYPE_INT = 3
        const val VAL_TYPE_FLOAT = 4
        const val VAL_TYPE_LONG = 5

        private val SEPARATOR = Char(0).toString()

        private const val SERIALIZED_NULL_VALUE_IN_COLLECTION = ""

        /**
         * Bit mask for collection value type.
         */
        internal const val MASK_VAL_TYPE_COLLECTION = 1 shl 8

        fun collectionTypeOf(type: Int) = type or MASK_VAL_TYPE_COLLECTION

        inline fun <reified T> judgeValueType(): Int {
            return when (val clz = T::class.java) {
                Int::class.java, Int::class.javaObjectType -> VAL_TYPE_INT
                String::class.java -> VAL_TYPE_TEXT
                Float::class.java, Float::class.javaObjectType -> VAL_TYPE_FLOAT
                Long::class.java, Long::class.javaObjectType -> VAL_TYPE_LONG
                Unit::class.java -> VAL_TYPE_IRRELEVANT
                else -> illegalArgument("type", clz)
            }
        }

    }

    /**
     * The logical relation to its previous peer applet. If true, representing `AND` relation and
     * this applet will not be executed when its previous peer failed, otherwise representing `OR`
     * relation and this applet will not be executed when its previous peer succeeded. If this applet
     * is the first element of a flow, this field will be ignored.
     */
    open var isAnd = true

    /**
     * If an applet is not enabled, the applet will not be executed as if it is removed from its parent.
     */
    var isEnabled = true

    /**
     * The id identifying an applet's factory and type.
     *
     * @see appletId
     * @see registryId
     */
    var id: Int = NO_ID

    /**
     * A human-readable comment to this applet.
     */
    var comment: String? = null

    /**
     * If an applet is invertible, its execution result can be inverted to the contrary side.
     */
    open var isInvertible = true

    open val requiredIndex = -1

    /**
     * Whether the result is inverted, only takes effect when the applet is [invertible][isInvertible].
     */
    var isInverted = false
        set(value) {
            if (value && !isInvertible) unsupportedOperation("This applet is not invertible!")
            field = value
        }

    /**
     * The masked type of value for `serialization`, which is composed as following:
     *
     * |0|│|0000 0000|
     * |  :----: |:----: | :----:  |
     * |is collection|│|raw type|
     */
    abstract val valueType: Int

    /**
     * Get the id of the registry where the applet is created.
     */
    inline val registryId get() = id ushr 16 and 0xFF

    /**
     * Get the type id of this applet.
     */
    inline val appletId get() = id and 0xFFFF

    var parent: Flow? = null

    var index: Int = -1

    var value: Any? = null

    /**
     * Referring ids from other [Applet]s as input.
     */
    var references: Map<Int, String> = emptyMap()

    /**
     * Reference ids exposed to other [Applet]s as output.
     */
    var refids: Map<Int, String> = emptyMap()

    fun requireParent() = requireNotNull(parent) {
        "Parent not found!"
    }

    /**
     * Execute the applet.
     *
     * @return returns `null` indicating the applet failed, [Unit] indicating the applet succeeded
     * but there is no result and otherwise returns the result.
     * @param runtime The shared runtime throughout the root flow's lifecycle.
     */
    @CheckResult
    abstract suspend fun apply(runtime: TaskRuntime): AppletResult

    fun isSuccess(any: Any?): Boolean {
        return any != null
    }

    fun toggleRelation() {
        isAnd = !isAnd
    }

    fun toggleInversion() {
        isInverted = !isInverted
    }

    fun toggleAbility() {
        isEnabled = !isEnabled
    }

    /**
     * Whether its value is a [Collection].
     */
    private val isCollectionValue: Boolean
        get() = valueType and MASK_VAL_TYPE_COLLECTION != 0

    /**
     * Unmasked raw type.
     *
     * @see MASK_VAL_TYPE_COLLECTION
     * @see Applet.valueType
     */
    val rawType: Int
        get() = valueType and MASK_VAL_TYPE_COLLECTION.inv()

    protected open fun serializeToString(value: Any): String {
        return value.toString()
    }

    internal fun serializeValue(): String? {
        val value = this.value
        return when {
            value == null -> null
            rawType == VAL_TYPE_IRRELEVANT -> null
            isCollectionValue -> (value as Collection<*>).joinToString(SEPARATOR) {
                if (it == null) {
                    SERIALIZED_NULL_VALUE_IN_COLLECTION
                } else {
                    serializeToString(it)
                }
            }
            else -> serializeToString(value)
        }
    }

    protected open fun deserializeFromString(src: String): Any? {
        return when (rawType) {
            VAL_TYPE_IRRELEVANT -> null
            VAL_TYPE_TEXT -> src
            VAL_TYPE_FLOAT -> src.toFloat()
            VAL_TYPE_INT -> src.toInt()
            VAL_TYPE_LONG -> src.toLong()
            else -> illegalArgument("value type", rawType)
        }
    }

    internal fun deserializeValue(src: String?) {
        value = if (src == null) null else if (isCollectionValue) {
            val split = src.split(SEPARATOR)
            split.mapTo(ArrayList(split.size)) {
                if (it == SERIALIZED_NULL_VALUE_IN_COLLECTION) null else deserializeFromString(it)
            }
        } else {
            deserializeFromString(src)
        }
    }

    override fun toString(): String {
        return javaClass.simpleName
    }

    /**
     * Derive referred value from this applet according to reference index.
     *
     * @see refids
     */
    open fun getReferent(which: Int, ret: Any): Any? {
        if (which == 0)
            return ret
        return null
    }
}