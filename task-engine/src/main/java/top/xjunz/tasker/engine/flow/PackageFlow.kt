package top.xjunz.tasker.engine.flow

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import top.xjunz.tasker.engine.AppletContext
import top.xjunz.tasker.engine.AppletResult

/**
 * @author xjunz 2022/09/03
 */
@Serializable
@SerialName(PackageFlow.NAME)
class PackageFlow : If() {

    companion object {
        const val NAME = "PackageFlow"
    }

    @Transient
    override var name: String? = NAME

    override fun onPreApply(ctx: AppletContext, result: AppletResult) {
        super.onPreApply(ctx, result)
    }

}