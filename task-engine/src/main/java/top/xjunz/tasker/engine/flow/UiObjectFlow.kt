package top.xjunz.tasker.engine.flow

import kotlinx.serialization.SerialName
import kotlinx.serialization.Transient

/**
 * @author xjunz 2022/08/25
 */
@SerialName(UiObjectFlow.NAME)
class UiObjectFlow : Flow() {

    companion object {
        const val NAME = "UiObjectFlow"
    }

    @Transient
    override var name: String? = NAME
}