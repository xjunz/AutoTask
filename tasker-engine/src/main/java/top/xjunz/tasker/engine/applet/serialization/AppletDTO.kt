package top.xjunz.tasker.engine.applet.serialization

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.applet.base.Reference
import top.xjunz.tasker.engine.applet.factory.AppletFactory
import top.xjunz.tasker.engine.applet.serialization.AppletValues.deserializeValue
import top.xjunz.tasker.engine.applet.serialization.AppletValues.serializeValue

/**
 * Data Transfer Object for [applets][Applet].
 *
 * @author xjunz 2022/10/28
 */
@Serializable
@SerialName("E") // Element
class AppletDTO(
    private val id: Int,
    @SerialName("a")
    private val isAnd: Boolean = true,
    @SerialName("i")
    private val isInverted: Boolean = false,
    @SerialName("v")
    private val literal: String? = null,
    @SerialName("t")
    private val tag: String? = null,
    @SerialName("r")
    private val refs: List<Reference> = emptyList()
) {

    @SerialName("e")
    private var elements: Array<AppletDTO>? = null

    companion object {
        /**
         * Convert a normal applet to a serializable applet.
         */
        fun Applet.toDTO(): AppletDTO {
            val dto = AppletDTO(id, isAnd, isInverted, serializeValue(value), refid, references)
            if (this is Flow) {
                check(size != 0) {
                    "No element!"
                }
                dto.elements = Array(size) {
                    this[it].toDTO()
                }
            }
            return dto
        }
    }

    fun toApplet(registry: AppletFactory): Applet {
        val prototype = registry.createAppletById(id)
        prototype.isAnd = isAnd
        prototype.isInverted = isInverted
        prototype.refid = tag
        prototype.references = refs
        if (prototype is Flow) {
            elements?.forEach {
                prototype.add(it.toApplet(registry))
            }
        }
        if (literal != null)
            prototype.value = prototype.deserializeValue(literal)

        return prototype
    }
}