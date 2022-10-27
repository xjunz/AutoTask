package top.xjunz.tasker.engine.applet.serialization

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.applet.factory.AppletFactory
import top.xjunz.tasker.engine.applet.serialization.AppletValues.deserializeValue
import top.xjunz.tasker.engine.applet.serialization.AppletValues.serializeValue

/**
 * @author xjunz 2022/10/28
 */
@Serializable
@SerialName("E") // Element
class SerializableApplet(
    private val id: Int,
    @SerialName("a")
    private val isAnd: Boolean = true,
    @SerialName("i")
    private val isInverted: Boolean = false,
    @SerialName("v")
    private val literal: String? = null
) {

    @SerialName("e")
    private var elements: Array<SerializableApplet>? = null

    companion object {
        /**
         * Convert an normal applet to a serializable applet.
         */
        fun Applet.toSerializable(): SerializableApplet {
            val sa = SerializableApplet(id, isAnd, isInverted, serializeValue(value))
            if (this is Flow) {
                check(count != 0) {
                    "No element!"
                }
                sa.elements = Array(count) {
                    elements[it].toSerializable()
                }
            }
            return sa
        }
    }

    fun toApplet(registry: AppletFactory): Applet {
        val prototype = registry.createAppletById(id)
        prototype.isAnd = isAnd
        prototype.isInverted = isInverted
        if (prototype is Flow) {
            elements?.forEach {
                prototype.elements.add(it.toApplet(registry))
            }
        }
        if (literal != null)
            prototype.value = prototype.deserializeValue(literal)

        return prototype
    }
}