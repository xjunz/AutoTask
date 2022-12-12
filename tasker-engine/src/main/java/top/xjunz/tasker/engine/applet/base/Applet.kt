package top.xjunz.tasker.engine.applet.base

import top.xjunz.shared.utils.unsupportedOperation
import top.xjunz.tasker.engine.applet.serialization.AppletValues
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * The base executable element of a [Flow].
 *
 * @author xjunz 2022/08/04
 */
abstract class Applet {

    object Configurator {

        const val MAX_REFERENCE_ID_LENGTH = 12

    }

    companion object {

        const val MAX_ID = 0x00FF_FFFF

        const val NO_ID = -1

        /**
         * The bit count used to store the applet index in a flow. We use a `LONG` to track depths
         * and indexes. Considering the race between the max depth and the max index, we choose 7
         * as the bit count to store indexes. Therefore, the max nested depth allowed in a flow is
         * limited to 9 (64/7) and the max child count of a flow is 128 (2^7). Also there is only 1
         * (64%7) bit remaining unused.
         *
         * @see TaskRuntime
         */
        const val FLOW_CHILD_COUNT_BITS = 7

        /**
         * The max child count allowed in a flow.
         */
        const val MAX_FLOW_CHILD_COUNT = 1 shl FLOW_CHILD_COUNT_BITS

        /**
         * The max nested depth allowed in the root flow.
         */
        const val MAX_FLOW_NESTED_DEPTH = Long.SIZE_BITS / FLOW_CHILD_COUNT_BITS
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
        set(value) {
            check(id <= MAX_ID) {
                "Illegal applet ID: $id"
            }
            field = value
        }

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
     * The masked type of value for `serialization`.
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

    var references: Map<Int, String> = emptyMap()

    var refids: Map<Int, String> = emptyMap()

    fun requireParent() = requireNotNull(parent) {
        "Parent not found!"
    }

    /**
     * Execute the applet.
     *
     * @param runtime The shared runtime throughout the root flow's lifecycle.
     */
    abstract suspend fun apply(runtime: TaskRuntime)

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
     * Get referred value from this applet as per a specific reference.
     */
    open fun getReferredValue(which: Int, ret: Any): Any? {
        if (which == 0)
            return ret
        return null
    }

    protected fun collectionTypeOf(rawType: Int) = rawType or AppletValues.MASK_VAL_TYPE_COLLECTION

    override fun toString(): String {
        if (comment == null) {
            return javaClass.simpleName
        }
        return "${javaClass.simpleName}(label=$comment, id=$id)"
    }
}