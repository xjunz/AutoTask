package top.xjunz.tasker.task.factory

import top.xjunz.tasker.R
import top.xjunz.tasker.engine.flow.*
import top.xjunz.tasker.ktx.format
import top.xjunz.tasker.ktx.str
import top.xjunz.tasker.util.illegalArgument

/**
 * @author xjunz 2022/08/25
 */
object FlowFactory : AppletFactory() {

    const val FLOW_ROOT = "RootFlow"

    override val name: String = "FlowFactory"

    override fun rawCreateApplet(name: String): Applet {
        return when (name) {
            And.NAME -> And()
            If.NAME -> If()
            Or.NAME -> Or()
            FLOW_ROOT -> Flow()
            When.NAME -> When()
            UiObjectFlow.NAME -> UiObjectFlow()
            else -> illegalArgument("name", name)
        }
    }

    override fun getDescriptionOf(applet: Applet): CharSequence {
        applet as Flow
        return R.string.format_flow_desc.format(applet.applets.size)
    }

    override fun getPromptOf(name: String): CharSequence {
        TODO("Not yet implemented")
    }

    override fun getLabelOf(name: String): CharSequence? {
        return when (name) {
            And.NAME -> R.string.flow_and.str
            If.NAME -> R.string.flow_if.str
            Or.NAME -> R.string.flow_or.str
            When.NAME -> R.string.flow_when.str
            UiObjectFlow.NAME -> R.string.flow_ui_object.str
            FLOW_ROOT -> null
            else -> illegalArgument("name", name)
        }
    }

    override val supportedNames: Array<String> = arrayOf(
        FLOW_ROOT, When.NAME, If.NAME, Or.NAME,
        And.NAME, UiObjectFlow.NAME, And.NAME
    )

    fun getScopedNames(scope: String): Array<String> {
        return when (scope) {
            FLOW_ROOT -> supportedNames
            UiObjectFlow.NAME, If.NAME, And.NAME, Or.NAME -> arrayOf(And.NAME, Or.NAME)
            When.NAME -> emptyArray()
            else -> illegalArgument("scope", scope)
        }
    }
}