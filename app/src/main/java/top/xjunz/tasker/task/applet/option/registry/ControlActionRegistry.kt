package top.xjunz.tasker.task.applet.option.registry

import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.base.If
import top.xjunz.tasker.ktx.clickable
import top.xjunz.tasker.ktx.foreColored
import top.xjunz.tasker.ktx.formatSpans
import top.xjunz.tasker.task.applet.anno.AppletCategory
import top.xjunz.tasker.task.applet.flow.DelayAction
import top.xjunz.tasker.task.applet.flow.RepeatFlow

/**
 * @author xjunz 2022/12/04
 */
class ControlActionRegistry(id: Int) : AppletOptionRegistry(id) {

    override val categoryNames: IntArray? = null

    @AppletCategory(0x0000)
    val ifAction = appletOption(0, R.string._if) { If() }

    @AppletCategory(0x0001)
    val delayAction = appletOption(1, R.string.delay) {
        DelayAction()
    }.withHelperText(R.string.help_delay_flow).withDescriber<Int> { applet, t ->
        R.string.format_delay.formatSpans(t.toString().foreColored().clickable {
            //  app.launchAction()
        })
    }.descAsTitle()

    @AppletCategory(0x0002)
    val repeatFlow = appletOption(2, R.string.repeat) {
        RepeatFlow()
    }.withValueDescriber<Int> {
        R.string.format_repeat.formatSpans(it.toString().foreColored())
    }.descAsTitle().withHelperText(R.string.input_repeat_count)
}