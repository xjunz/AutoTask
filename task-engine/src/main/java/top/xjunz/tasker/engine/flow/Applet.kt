package top.xjunz.tasker.engine.flow

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import top.xjunz.tasker.engine.AppletContext
import top.xjunz.tasker.engine.AppletResult

/**
 * The base element of a [RootFlow].
 *
 * @author xjunz 2022/08/04
 */
@Serializable
abstract class Applet(
    /**
     * The identifier name, can be [transient][Transient] if the applet is identifiable by its type.
     */
    open var name: String? = null
) {

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
     * @param sharedResult The shared result throughout the root flow's lifecycle.
     */
    abstract fun apply(context: AppletContext, sharedResult: AppletResult)

    override fun toString(): String {
        if (name == null) {
            return javaClass.simpleName
        }
        return "${javaClass.simpleName}(name=$name)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Applet) return false
        if (other.javaClass != javaClass) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        if (name == null) return javaClass.name.hashCode()
        return name?.hashCode() ?: 0
    }

}