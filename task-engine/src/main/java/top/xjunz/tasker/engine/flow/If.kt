package top.xjunz.tasker.engine.flow

import kotlinx.serialization.SerialName
import kotlinx.serialization.Transient
import top.xjunz.tasker.engine.AppletContext
import top.xjunz.tasker.engine.AppletResult
import top.xjunz.tasker.util.runtimeException

/**
 * @author xjunz 2022/08/11
 */
@SerialName(If.NAME)
open class If : Flow() {

    companion object {
        const val NAME = "If"
    }

    @Transient
    override var name: String? = NAME

    override val isInvertible: Boolean = true

    override fun checkElements() {
        super.checkElements()
        if (applets.size >= 1 && (applets[0] is And || applets[0] is Or)) {
            runtimeException("The first element or [If] must not be an [And] or an [Or].")
        }
    }

    override fun onPostApply(ctx: AppletContext, result: AppletResult) {
        if (isInverted == result.isSuccessful) {
            // The IF flow is failed.
            stopship(ctx)
        }
    }
}