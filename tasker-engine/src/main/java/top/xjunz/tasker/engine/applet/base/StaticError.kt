package top.xjunz.tasker.engine.applet.base

import top.xjunz.shared.utils.illegalArgument

/**
 * Defining static errors in a [Flow] that could be checked before runtime. Also these errors are not
 * from code bugs but from legal user operations. That is, we allow lenient flow edition but this may
 * also cause known errors.
 *
 * @author xjunz 2022/12/22
 */
class StaticError(val victim: Applet, val code: Int, val arg: String? = null) {

    companion object {


        const val ERR_NONE = -1

        /**
         * There is an empty flow.
         */
        const val ERR_FLOW_NO_ELEMENT = 0

        /**
         * Referring a reference which is not yet available when reached.
         */
        const val ERR_INVALID_REFERENCE = 1
        const val ERR_IF_NOT_FOLLOWED_BY_DO = 2
        const val ERR_ELSEIF_NOT_FOLLOWING_DO = 3
        const val ERR_WHEN_NO_FELLOW = 4

        const val PROMPT_ADD_INSIDE = 0

        @Deprecated("No longer used but preserved.")
        const val PROMPT_ADD_BEFORE = 1
        const val PROMPT_ADD_AFTER = 2
        const val PROMPT_RESET_REFERENCE = 3
    }

    val prompt: Int by lazy {
        when (code) {
            ERR_FLOW_NO_ELEMENT -> PROMPT_ADD_INSIDE
            ERR_WHEN_NO_FELLOW, ERR_IF_NOT_FOLLOWED_BY_DO -> PROMPT_ADD_AFTER
            ERR_INVALID_REFERENCE -> PROMPT_RESET_REFERENCE
            else -> illegalArgument()
        }
    }

}