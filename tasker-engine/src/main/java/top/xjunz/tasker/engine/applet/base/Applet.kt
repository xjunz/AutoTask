/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.base

import android.util.SparseArray
import androidx.annotation.CheckResult
import top.xjunz.shared.utils.illegalArgument
import top.xjunz.shared.utils.unsupportedOperation
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.engine.runtime.ValueRegistry
import java.lang.ref.WeakReference

/**
 * The base executable element of a [Flow].
 *
 * @author xjunz 2022/08/04
 */
abstract class Applet {

    companion object {

        const val NO_ID = -1

        const val REL_AND = 0
        const val REL_OR = 1
        const val REL_ANYWAY = 2

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

    open var relation = REL_AND

    val isAnd: Boolean get() = relation == REL_AND

    val isOr: Boolean get() = relation == REL_OR

    val isAnyway: Boolean get() = relation == REL_ANYWAY

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

    open val defaultValue: Any? = null

    /**
     * References to other [Applet]s as input.
     */
    var references: Map<Int, String> = emptyMap()

    /**
     * Referents exposed to other [Applet]s as output.
     */
    var referents: Map<Int, String> = emptyMap()

    val isClone get() = source?.get() != null

    internal var weakKeys: SparseArray<ValueRegistry.WeakKey>? = null

    /**
     * The source where this is cloned. Could be `null` if this is not a clone or the source is
     * no longer in usage.
     */
    internal var source: WeakReference<Applet>? = null

    fun requireSource(): Applet {
        return source!!.get()!!
    }

    @Synchronized
    fun removeWeakKey(id: Int) {
        weakKeys?.remove(id)
    }

    @Synchronized
    fun getWeakKey(id: Int): ValueRegistry.WeakKey {
        if (weakKeys == null) {
            weakKeys = SparseArray()
        }
        var key = weakKeys?.get(id)
        if (key == null) {
            key = ValueRegistry.WeakKey()
            weakKeys?.put(id, key)
        }
        return key
    }

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

    open fun toggleRelation() {
        relation = if (relation == REL_AND) REL_OR else REL_AND
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

    protected open fun serializeValueToString(value: Any): String {
        return value.toString()
    }

    internal fun serializeValue(): String? {
        val value = this.value
        return when {
            value == null -> null
            rawType == VAL_TYPE_IRRELEVANT -> null
            isCollectionValue ->
                (value as Collection<*>).joinToString(SEPARATOR) {
                    if (it == null) {
                        SERIALIZED_NULL_VALUE_IN_COLLECTION
                    } else {
                        serializeValueToString(it)
                    }
                }
            else -> serializeValueToString(value)
        }
    }

    protected open fun deserializeValueFromString(src: String): Any? {
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
                if (it == SERIALIZED_NULL_VALUE_IN_COLLECTION) null
                else deserializeValueFromString(it)
            }
        } else {
            deserializeValueFromString(src)
        }
    }

    override fun toString(): String {
        return javaClass.simpleName
    }

    override fun hashCode(): Int {
        return source?.get()?.hashCode() ?: super.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Applet) return false
        return super.equals(other)
    }

}