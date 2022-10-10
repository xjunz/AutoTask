package top.xjunz.tasker.engine.flow

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import top.xjunz.tasker.engine.AppletContext
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
         * The `AND` relation. Any applet with this relation will not be executed if its previous
         * applet fails.
         *
         * @see Applet.relation
         */
        const val RELATION_AND = 1

        /**
         * The `OR` relation. Any applet with this relation will not be executed if its previous
         * applet succeeded.
         *
         * @see Applet.relation
         */
        const val RELATION_OR = 2

        /**
         * Not specifying any relation, only available for the first applet in a flow. The applet will
         * be executed anyway.
         *
         * @see Applet.relation
         */
        const val RELATION_NONE = 0

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
     * The logical relation to its previous peer applet. Hence, if an applet is the first element in
     * a flow, its relation should be [NONE][RELATION_NONE].
     *
     * @see RELATION_NONE
     * @see RELATION_AND
     * @see RELATION_OR
     */
    var relation: Int = RELATION_NONE

    /**
     * The id, which is useful for identifying a class of applet. Note that this is not designed to
     * distinguish an specific applet between different instances (with different hash codes).
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
     * Whether the result is inverted, only takes effect when the applet [is invertible][isInvertible].
     */
    var isInverted = false

    /**
     * Execute the applet.
     *
     * @param context The overall context of the [Applet] providing the access to its owner flow,
     * its owner task or some environment variables.
     * @param runtime The shared runtime throughout the root flow's lifecycle.
     */
    abstract fun apply(context: AppletContext, runtime: FlowRuntime)

    fun switchRelation() {
        if (relation == RELATION_OR) {
            relation = RELATION_AND
        } else if (relation == RELATION_AND) {
            relation = RELATION_OR
        }
    }

    override fun toString(): String {
        if (label == null) {
            return javaClass.simpleName
        }
        return "${javaClass.simpleName}(label=$label, id=$id)"
    }

}