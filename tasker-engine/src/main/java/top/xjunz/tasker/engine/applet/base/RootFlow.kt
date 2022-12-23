package top.xjunz.tasker.engine.applet.base

/**
 * @author xjunz 2022/12/05
 */
open class RootFlow : Flow() {

    override fun staticCheckMyself(): Int {
        check(isEnabled)
        return super.staticCheckMyself()
    }

    /**
     * If this is root flow, we will check invalid references in addition.
     */
    override fun performStaticCheck(): StaticError? {
        var error = super.performStaticCheck()
        if (error == null) {
            error = checkInvalidReference(this, mutableSetOf())
        }
        return error
    }

    private fun checkInvalidReference(applet: Applet, refids: MutableSet<String>): StaticError? {
        val invalid = applet.references.values.find {
            !refids.contains(it)
        }
        if (invalid != null) {
            return StaticError(applet, StaticError.ERR_INVALID_REFERENCE, invalid)
        }
        refids.addAll(applet.refids.values)
        if (applet is Flow) {
            applet.forEach {
                val error = checkInvalidReference(it, refids)
                if (error != null) {
                    return error
                }
            }
        }
        return null
    }
}