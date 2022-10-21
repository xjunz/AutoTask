package top.xjunz.tasker.engine.base

import kotlinx.serialization.*
import top.xjunz.tasker.engine.AppletContext
import top.xjunz.tasker.engine.AppletValueSerializer.deserialize
import top.xjunz.tasker.engine.AppletValueSerializer.judgeValueType
import top.xjunz.tasker.engine.AppletValueSerializer.serialize
import top.xjunz.tasker.engine.FlowRuntime

/**
 * The base executable element of a [Flow].
 *
 * @author xjunz 2022/08/04
 */
@Serializable
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
     * this applet will not be executed when its previous peer fails, otherwise representing `OR`
     * relation and this applet will not be executed when its previous peer succeeds. If this applet
     * is the first element of a flow, this field will be ignored.
     */
    @SerialName("a")
    var isAnd: Boolean = true

    /**
     * The id identifying an applet's factory and type.
     *
     * @see appletId
     * @see factoryId
     */
    var id: Int = NO_ID

    /**
     * A human-readable label.
     */
    @Transient
    open var label: String? = null

    /**
     * If an applet is invertible, its execution result can be inverted to the contrary side.
     */
    @Transient
    open val isInvertible = true

    /**
     * If an applet is required, it is not allowed to be removed from its parent [Flow].
     */
    @Transient
    open val isRequired = false

    /**
     * Whether the result is inverted, only takes effect when the applet is [invertible][isInvertible].
     */
    @SerialName("i")
    var isInverted = false

    /**
     * The masked type of value for `serialization`.
     */
    @SerialName("t")
    var valueType = 0

    /**
     * The literal value for `serialization` and `deserialization`.
     */
    @SerialName("l")
    protected lateinit var literal: String

    /**
     * The real value in runtime.
     */
    @Transient
    private var real: Any? = null

    /**
     * Get the id of the factory where the applet is created.
     */
    inline val factoryId get() = id ushr 16

    /**
     * Get the type id of this applet.
     */
    inline val appletId get() = id and 0xFFFF

    fun parseValue() {
        check(::literal.isInitialized) {
            "Nothing to parse!"
        }
        real = deserialize(literal)
    }

    var value: Any
        get() = checkNotNull(real) {
            "The value is not yet set or parsed!"
        }
        set(value) {
            valueType = judgeValueType(value)
            literal = serialize(value)
            real = value
        }

    /**
     * Execute the applet.
     *
     * @param context The overall context of the [Applet] providing the access to its owner flow,
     * its owner task or some environment variables.
     * @param runtime The shared runtime throughout the root flow's lifecycle.
     */
    abstract fun apply(context: AppletContext, runtime: FlowRuntime)

    fun toggleRelation() {
        isAnd = !isAnd
    }

    override fun toString(): String {
        if (label == null) {
            return javaClass.simpleName
        }
        return "${javaClass.simpleName}(label=$label, id=$id)"
    }

}