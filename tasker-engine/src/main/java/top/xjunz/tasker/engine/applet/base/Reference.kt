package top.xjunz.tasker.engine.applet.base

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A reference describes the information of a required value: where the value is from via field [id]
 * (required) and how the value is fetched via field [which].
 *
 * If there is only one way to fetch the value, the [which] is 0.
 *
 * @author xjunz 2022/11/22
 */
@Serializable
data class Reference(
    @SerialName("n")
    val id: String,
    @SerialName("i")
    val which: Int = 0
)