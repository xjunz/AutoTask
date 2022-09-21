package top.xjunz.tasker.task.factory

import top.xjunz.tasker.engine.flow.Applet
import top.xjunz.tasker.engine.flow.Flow
import top.xjunz.tasker.util.runtimeException


/**
 * The registry storing all of the registered [Applet]. Registered criteria can be `serialized`
 * and `deserialized` via a unique name.
 *
 * @author xjunz 2022/08/09
 */
object AppletRegistry {

    private val ALL_FACTORIES = arrayOf(FlowFactory, EventFilterFactory, PackageCriterionFactory)

    private fun throwIfNameNotFound(name: String): Nothing {
        runtimeException("Cannot find any applet with name '$name'!")
    }

    val Applet.description: CharSequence?
        get() {
            if (name == null) return null
            ALL_FACTORIES.forEach {
                if (it.supportedNames.contains(name)) {
                    return it.getDescriptionOf(this)
                }
            }
            throwIfNameNotFound(name!!)
        }

    val Applet.label: CharSequence?
        get() {
            if (name == null) return null
            ALL_FACTORIES.forEach {
                if (it.supportedNames.contains(name)) {
                    return it.getLabelOf(name!!)
                }
            }
            throwIfNameNotFound(name!!)
        }

    fun createAppletByName(name: String): Applet {
        ALL_FACTORIES.forEach {
            if (it.supportedNames.contains(name)) {
                return it.createApplet(name)
            }
        }
        throwIfNameNotFound(name)
    }

    fun Flow.getFactory(): AppletFactory {
        TODO()
    }
}

