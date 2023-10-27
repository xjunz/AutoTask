/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.base

import androidx.annotation.CheckResult
import top.xjunz.shared.utils.illegalArgument
import top.xjunz.shared.utils.unsupportedOperation
import top.xjunz.tasker.engine.runtime.TaskRuntime
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

        const val ARG_TYPE_REFERENCE = 0
        const val ARG_TYPE_TEXT = 1

        const val ARG_TYPE_INT = 3
        const val ARG_TYPE_FLOAT = 4
        const val ARG_TYPE_LONG = 5

        /**
         * Bit mask for collection value type.
         */
        private const val MASK_ARG_TYPE_COLLECTION = 1 shl 8

        fun collectionTypeOf(type: Int) = type or MASK_ARG_TYPE_COLLECTION

        fun judgeValueType(clz: Class<*>): Int {
            return when (clz) {
                Int::class.java, Int::class.javaObjectType -> ARG_TYPE_INT
                String::class.java -> ARG_TYPE_TEXT
                Float::class.java, Float::class.javaObjectType -> ARG_TYPE_FLOAT
                Long::class.java, Long::class.javaObjectType -> ARG_TYPE_LONG
                else -> illegalArgument("type", clz)
            }
        }

        fun isCollectionArg(argType: Int): Boolean {
            return argType and MASK_ARG_TYPE_COLLECTION != 0
        }

        fun getRawArgType(argType: Int): Int {
            return argType and MASK_ARG_TYPE_COLLECTION.inv()
        }
    }

    var name: String? = null

    val isAnd: Boolean get() = relation == REL_AND

    val isOr: Boolean get() = relation == REL_OR

    val isAnyway: Boolean get() = relation == REL_ANYWAY

    open val requiredIndex = -1

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

    var parent: Flow? = null

    var index: Int = -1

    val singleValue: Any? get() = values.values.singleOrNull()

    /**
     * References to other [Applet]s as input arguments.
     */
    var references: Map<Int, String> = emptyMap()

    /**
     * Non-reference input arguments.
     */
    var values: Map<Int, Any> = emptyMap()

    /**
     * Referents exposed to other [Applet]s as output results.
     */
    var referents: Map<Int, String> = emptyMap()

    @Deprecated("Only for compatibility use.")
    /**
     * Whether the value argument is innate (not assignable)
     */
    open val isValueInnate = false

    /**
     * Store argument masked types, which are helpful when serializing and deserializing [values].
     * **The size of this does not actually determine how many arguments will be present, because
     * there may be a vararg argument type, which allows unspecific numbers of arguments.**
     *
     * The masked type of value is composed as following:
     *
     * |0|│|0000 0000|
     * |  :----: |:----: | :----:  |
     * |is collection|│|raw type(non-reference type)|
     */
    lateinit var argumentTypes: IntArray

    /**
     * Whether the result is inverted, only takes effect when the applet is [invertible][isInvertible].
     */
    var isInverted = false
        set(value) {
            if (value && !isInvertible) unsupportedOperation("This applet is not invertible!")
            field = value
        }

    /**
     * If an applet is invertible, its execution result can be inverted to the contrary side.
     */
    open var isInvertible = true

    open var relation = REL_AND

    open val supportsAnywayRelation: Boolean = false

    /**
     * Get the id of the registry where the applet is created.
     */
    inline val registryId get() = id ushr 16 and 0xFF

    /**
     * Get the type id of this applet without registry info.
     */
    inline val appletId get() = id and 0xFFFF

    val singleValueType: Int?
        get() = argumentTypes.singleOrNull {
            it != ARG_TYPE_REFERENCE
        }

    val firstValue get() = values.values.first()

    open val defaultValue: Any? = null

    private var cloneSourceRef: WeakReference<Applet>? = null

    var cloneSource: Applet?
        get() {
            return cloneSourceRef?.get()
        }
        set(value) {
            cloneSourceRef = if (value == null) {
                null
            } else {
                WeakReference(value)
            }
        }

    var obsoletedId: Int = -1

    /**
     * Execute the applet.
     *
     * @return returns `null` indicating the applet failed, [Unit] indicating the applet succeeded
     * but there is no result and otherwise returns the result.
     * @param runtime The shared runtime throughout the root flow's lifecycle.
     */
    @CheckResult
    abstract suspend fun apply(runtime: TaskRuntime): AppletResult

    fun requireParent() = requireNotNull(parent) {
        "Parent not found!"
    }

    open fun toggleRelation() {
        relation = if (supportsAnywayRelation) {
            when {
                isAnd -> REL_OR
                isOr -> REL_ANYWAY
                else -> REL_AND
            }
        } else {
            if (relation == REL_AND) REL_OR else REL_AND
        }
    }

    fun toggleInversion() {
        isInverted = !isInverted
    }

    fun toggleAbility() {
        isEnabled = !isEnabled
    }

    open fun serializeArgumentToString(which: Int, rawType: Int, arg: Any): String {
        check(!isCollectionArg(rawType)) {
            "Require a non-collection type!"
        }
        return arg.toString()
    }

    open fun deserializeArgumentFromString(which: Int, rawType: Int, src: String): Any {
        check(!isCollectionArg(rawType)) {
            "Require a non-collection type!"
        }
        return when (rawType) {
            ARG_TYPE_TEXT -> src
            ARG_TYPE_FLOAT -> src.toFloat()
            ARG_TYPE_INT -> src.toInt()
            ARG_TYPE_LONG -> src.toLong()
            else -> illegalArgument("value type", rawType)
        }
    }

    /**
     * Directly tell the runtime whether this applet should be skipped. This is prior to
     * all other criteria.
     *
     * @see Flow.applyFlow
     */
    internal open fun shouldBeSkipped(runtime: TaskRuntime): Boolean {
        return false
    }

    open fun onSkipped(runtime: TaskRuntime) {
        /* no-op */
    }

    /**
     * Do something before the applet is executed. At this time, [TaskRuntime.currentApplet] is
     * not yet assigned to this applet. This is guaranteed to be called even if this is skipped.
     *
     * @see Flow.applyFlow
     */
    open fun onPreApply(runtime: TaskRuntime) {
        /* no-op */
    }

    /**
     * Just before the applet executes. At this time, [TaskRuntime.currentApplet] is assigned
     * to this applet.
     *
     * @see Flow.applyFlow
     */
    open fun onPrepareApply(runtime: TaskRuntime) {
        /* no-op */
    }

    /**
     * Do something after the applying is completed.
     * @see Flow.applyFlow
     */
    open fun onPostApply(runtime: TaskRuntime) {
        /* no-op */
    }

    protected fun getArgument(index: Int, runtime: TaskRuntime): Any? {
        if (values.containsKey(index)) {
            return values[index]
        }
        if (references.containsKey(index)) {
            return runtime.getReferenceArgument(this, index)
        }
        throw RuntimeException("Argument $index is not specified!")
    }

    override fun toString(): String {
        if (name != null) {
            return name!!
        }
        return super.toString()
    }

}