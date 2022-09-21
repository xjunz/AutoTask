package top.xjunz.tasker.task.factory

import top.xjunz.tasker.engine.flow.Applet

/**
 * @author xjunz 2022/08/11
 */
abstract class AppletFactory {

    abstract val name: String

    abstract val supportedNames: Array<String>

    fun createApplet(name: String): Applet {
        return rawCreateApplet(name).also {
            it.name = name
        }
    }

    /**
     * Create an raw [Applet] from a [name]. The [name] must be one of the [supportedNames].
     */
    protected abstract fun rawCreateApplet(name: String): Applet

    abstract fun getDescriptionOf(applet: Applet): CharSequence?

    abstract fun getPromptOf(name: String): CharSequence

    abstract fun getLabelOf(name: String): CharSequence?

}