package top.xjunz.tasker.engine.flow

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import top.xjunz.tasker.engine.AppletContext
import top.xjunz.tasker.engine.FlowRuntime

/**
 * The base element of a [Flow].
 *
 * @author xjunz 2022/08/04
 */
@Serializable
abstract class Applet{

    companion object {
        const val NO_ID = -1
    }

    /**
     * The id, which is useful for identifying an [Applet].
     */
    var id: Int = NO_ID

    /**
     * The human-readable label.
     */
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
     * Whether the result is inverted. Only take effect when the applet [is invertible][isInvertible].
     */
    var isInverted = false

    /**
     * Execute the applet.
     *
     * @param context The overall context of the [Applet] providing the access to its owner flow and
     * its owner task.
     * @param runtime The shared runtime throughout the root flow's lifecycle.
     */
    abstract fun apply(context: AppletContext, runtime: FlowRuntime)

    override fun toString(): String {
        if (label == null) {
            return javaClass.simpleName
        }
        return "${javaClass.simpleName}(label=$label)"
    }

}