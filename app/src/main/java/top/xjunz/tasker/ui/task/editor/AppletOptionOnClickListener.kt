package top.xjunz.tasker.ui.task.editor

import androidx.fragment.app.viewModels
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.criterion.BoundsCriterion
import top.xjunz.tasker.engine.applet.criterion.Criterion
import top.xjunz.tasker.engine.applet.criterion.NumberRangeCriterion
import top.xjunz.tasker.engine.applet.criterion.PropertyCriterion
import top.xjunz.tasker.engine.applet.serialization.AppletValues
import top.xjunz.tasker.engine.applet.serialization.AppletValues.rawType
import top.xjunz.tasker.ktx.array
import top.xjunz.tasker.ktx.setMaxLength
import top.xjunz.tasker.ktx.show
import top.xjunz.tasker.ktx.str
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.task.applet.option.AppletOptionFactory
import top.xjunz.tasker.ui.common.TextEditorDialog
import top.xjunz.tasker.ui.task.editor.selector.*

/**
 * @author xjunz 2022/10/08
 */
class AppletOptionOnClickListener(fragment: FlowEditorDialog) {

    private val fragmentManager by lazy {
        fragment.parentFragmentManager
    }

    private val registry: AppletOptionFactory by lazy {
        fragment.viewModels<FlowEditorViewModel>().value.appletOptionFactory
    }

    fun onClick(applet: Applet, option: AppletOption, onCompleted: () -> Unit) {
        when {
            option == registry.packageAppletFactory.pkgCollection ->
                ComponentSelectorDialog().setTitle(option.currentTitle!!)
                    .show(fragmentManager)

            option == registry.packageAppletFactory.activityCollection ->
                ComponentSelectorDialog().setTitle(option.currentTitle!!)
                    .setMode(ComponentSelectorDialog.MODE_ACTIVITY)
                    .show(fragmentManager)

            option == registry.timeAppletFactory.timeRange -> {
                val value = applet.value?.casted<Collection<Long>>()
                DateTimeRangeEditorDialog().setRange(
                    value?.firstOrNull() ?: System.currentTimeMillis(),
                    value?.lastOrNull() ?: System.currentTimeMillis()
                ).setType(AppletValues.VAL_TYPE_LONG).setTitle(option.currentTitle!!)
                    .doOnCompletion { start, end ->
                        applet.value = listOf(start, end)
                        onCompleted()
                    }.show(fragmentManager)
            }

            option == registry.timeAppletFactory.hourMinSec -> {
                val value = applet.value?.casted<Collection<Int>>()
                TimeRangeEditorDialog().setRange(
                    value?.firstOrNull() ?: 0,
                    value?.lastOrNull() ?: 0
                ).setTitle(option.currentTitle!!).doOnCompletion { start, end ->
                    applet.value = listOf(start, end)
                    onCompleted()
                }.show(fragmentManager)
            }

            option == registry.timeAppletFactory.dayOfMonth -> {
                val days = Array<CharSequence>(31) { i ->
                    (i + 1).toString()
                }
                EnumSelectorDialog().setArguments(option.currentTitle, days) {
                    applet.value = it
                    onCompleted()
                }.setSpanCount(4).setInitialSelections(applet.value?.casted()).show(fragmentManager)
            }

            option == registry.timeAppletFactory.dayOfWeek -> {
                EnumSelectorDialog().setArguments(option.currentTitle, R.array.days_in_week) {
                    applet.value = it
                    onCompleted()
                }.setInitialSelections(applet.value?.casted()).show(fragmentManager)
            }

            option == registry.timeAppletFactory.month -> {
                EnumSelectorDialog().setArguments(option.currentTitle, R.array.months) {
                    applet.value = it
                    onCompleted()
                }.setInitialSelections(applet.value?.casted()).show(fragmentManager)
            }

            applet is PropertyCriterion<*> -> {
                applet.isInverted = option.isInverted
                onCompleted()
            }

            applet is NumberRangeCriterion<*, *> -> {
                val value = applet.value?.casted<Collection<Number>>()
                RangeEditorDialog().doOnCompletion { start, end ->
                    applet.value = listOf(start, end)
                    onCompleted()
                }.setType(applet.rawType).setRange(
                    value?.firstOrNull(), value?.lastOrNull()
                ).setTitle(option.currentTitle!!).show(fragmentManager)
            }

            applet is BoundsCriterion<*> -> {
                DistanceEditorDialog().setArguments(option.currentTitle) {
                    applet.value = it
                    onCompleted()
                }.setDirection(applet.direction).show(fragmentManager)
            }

            applet is Criterion<*, *> -> when (applet.valueType) {
                AppletValues.VAL_TYPE_TEXT -> {
                    TextEditorDialog().configEditText {
                        it.setMaxLength(64)
                    }.setArguments(option.currentTitle!!) {
                        if (it.isBlank()) {
                            return@setArguments R.string.empty_input_not_allowed.str
                        }
                        applet.value = it
                        onCompleted()
                        return@setArguments null
                    }.apply {
                        if (option.hasPresets) {
                            setDropDownData(
                                option.presetsNameRes.array,
                                option.presetsValueRes.array
                            )
                        }
                    }.show(fragmentManager)
                }
            }

        }
    }
}