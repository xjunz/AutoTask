/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry

import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.applet.criterion.Criterion
import top.xjunz.tasker.engine.applet.criterion.createCriterion
import top.xjunz.tasker.task.applet.anno.AppletOrdinal

/**
 * @author xjunz 2023/02/12
 */
class TextCriterionRegistry(id: Int) : AppletOptionRegistry(id) {

    private inline fun textCriterion(crossinline block: (String, String) -> Boolean): Criterion<*, *> {
        return createCriterion<String, String> { t, v ->
            AppletResult.resultOf(t) { block(it, v) }
        }
    }

    @AppletOrdinal(0x0000)
    val equalsTo = invertibleAppletOption(R.string.format_text_content) {
        textCriterion { s, s2 ->
            s == s2
        }
    }.withRefArgument<String>(R.string.text)
        .withValueArgument<String>(R.string.content)
        .hasCompositeTitle()

    @AppletOrdinal(0x0001)
    val startsWith = invertibleAppletOption(R.string.format_starts_with) {
        textCriterion { s, s2 ->
            s.startsWith(s2)
        }
    }.withRefArgument<String>(R.string.text, R.string.empty)
        .withValueArgument<String>(R.string.prefix)
        .hasCompositeTitle()

    @AppletOrdinal(0x0002)
    val endsWith = invertibleAppletOption(R.string.format_ends_with) {
        textCriterion { s, s2 ->
            s.endsWith(s2)
        }
    }.withRefArgument<String>(R.string.text, R.string.empty)
        .withValueArgument<String>(R.string.suffix)
        .hasCompositeTitle()

    @AppletOrdinal(0x0003)
    val contains = invertibleAppletOption(R.string.format_contains) {
        textCriterion { s, s2 ->
            s.contains(s2)
        }
    }.withRefArgument<String>(R.string.text)
        .withValueArgument<String>(R.string.containment)
        .hasCompositeTitle()

    @AppletOrdinal(0x0004)
    val matches = invertibleAppletOption(R.string.format_matches_regex) {
        textCriterion { s, s2 ->
            s.matches(Regex(s2))
        }
    }.withRefArgument<String>(R.string.text, R.string.empty)
        .withValueArgument<String>(R.string.regex)
        .hasCompositeTitle()

}