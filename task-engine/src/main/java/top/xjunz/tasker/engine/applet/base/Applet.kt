package top.xjunz.tasker.engine.applet.base

import top.xjunz.shared.utils.unsupportedOperation
import top.xjunz.tasker.engine.AutomatorTask
import top.xjunz.tasker.engine.applet.serialization.AppletValues
import top.xjunz.tasker.engine.runtime.FlowRuntime

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
         * limited to 9 (64/7) and the max child count of a flow is 128 (2^7). Also there is only 1
         * (64%7) bit remaining unused.
         *
         * @see FlowRuntime
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
    open var isAnd: Boolean = true

    /**
     * The id identifying an applet's factory and type.
     *
     * @see appletId
     * @see registryId
     */
    var id: Int = NO_ID

    /**
     * A human-readable label.
     */
    open var label: String? = null

    /**
     * If an applet is invertible, its execution result can be inverted to the contrary side.
     */
    open var isInvertible = true

    /**
     * If an applet is required, it is not allowed to be removed from its parent [Flow].
     */
    open val isRequired = false

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
    abstract var valueType: Int
        internal set

    /**
     * Get the id of the registry where the applet is created.
     */
    inline val registryId get() = id ushr 16

    /**
     * Get the type id of this applet.
     */
    inline val appletId get() = id and 0xFFFF

    var parent: Flow? = null

    var index: Int = -1

    var value: Any? = null

    fun requireParent() = parent!!

    fun isChildOf(flow: Flow): Boolean {
        if (parent == null) return false
        if (parent === flow)
            return true
        return requireParent().isChildOf(flow)
    }

    /**
     * Execute the applet.
     *
     * @param task The owner task
     * @param runtime The shared runtime throughout the root flow's lifecycle.
     */
    abstract fun apply(task: AutomatorTask, runtime: FlowRuntime)

    fun toggleRelation() {
        isAnd = !isAnd
    }

    fun toggleInversion() {
        isInverted = !isInverted
    }

    protected fun collectionTypeOf(rawType: Int) = rawType or AppletValues.MASK_VAL_TYPE_COLLECTION

    override fun toString(): String {
        if (label == null) {
            return javaClass.simpleName
        }
        return "${javaClass.simpleName}(label=$label, id=$id)"
    }

}