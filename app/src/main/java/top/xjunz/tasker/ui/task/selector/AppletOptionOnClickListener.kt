package top.xjunz.tasker.ui.task.selector

import androidx.fragment.app.Fragment
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.action.Action
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
import top.xjunz.tasker.ui.task.selector.option.*

/**
 * @author xjunz 2022/10/08
 */
open class AppletOptionOnClickListener(
    fragment: Fragment,
    private val factory: AppletOptionFactory
) {

    private val actionOptionOnClickListener by lazy {
        ActionOptionOnClickListener(fragment, factory)
    }

    protected val fragmentManager by lazy {
        fragment.childFragmentManager
    }

    fun onClick(applet: Applet, onCompleted: () -> Unit) {
        onClick(applet, factory.requireOption(applet), onCompleted)
    }

    open fun onClick(applet: Applet, option: AppletOption, onCompleted: () -> Unit) {
        when {
            applet is Action -> actionOptionOnClickListener.onClick(applet, option, onCompleted)

            option == factory.packageRegistry.pkgCollection ->
                ComponentSelectorDialog().setSelectedPackages(applet.value?.casted() ?: emptyList())
                    .doOnCompleted {
                        applet.value = it
                        onCompleted()
                    }
                    .setTitle(option.currentTitle!!)
                    .show(fragmentManager)

            option == factory.packageRegistry.activityCollection ->
                ComponentSelectorDialog().setTitle(option.currentTitle!!)
                    .setSelectedActivities(applet.value?.casted() ?: emptyList())
                    .doOnCompleted {
                        applet.value = it
                        onCompleted()
                    }
                    .setMode(ComponentSelectorDialog.MODE_ACTIVITY)
                    .show(fragmentManager)

            option == factory.timeRegistry.timeRange -> {
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

            option == factory.timeRegistry.hourMinSec -> {
                val value = applet.value?.casted<Collection<Int>>()
                TimeRangeEditorDialog().setRange(
                    value?.firstOrNull(), value?.lastOrNull()
                ).setTitle(option.currentTitle!!).doOnCompletion { start, end ->
                    applet.value = listOf(start, end)
                    onCompleted()
                }.show(fragmentManager)
            }

            option == factory.timeRegistry.dayOfMonth -> {
                val days = Array<CharSequence>(31) { i ->
                    (i + 1).toString()
                }
                EnumSelectorDialog().setArguments(option.currentTitle, days) {
                    applet.value = it
                    onCompleted()
                }.setSpanCount(4).setInitialSelections(applet.value?.casted()).show(fragmentManager)
            }

            option == factory.timeRegistry.dayOfWeek -> {
                EnumSelectorDialog().setArguments(option.currentTitle, R.array.days_in_week) {
                    applet.value = it
                    onCompleted()
                }.setInitialSelections(applet.value?.casted()).show(fragmentManager)
            }

            option == factory.timeRegistry.month -> {
                EnumSelectorDialog().setArguments(option.currentTitle, R.array.months) {
                    applet.value = it
                    onCompleted()
                }.setInitialSelections(applet.value?.casted()).show(fragmentManager)
            }

            applet is PropertyCriterion<*> -> {
                applet.toggleInversion()
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
                }.setDistance(applet.value?.casted()).setDirection(applet.direction)
                    .show(fragmentManager)
            }

            applet is Criterion<*, *> -> when (applet.valueType) {
                AppletValues.VAL_TYPE_TEXT -> {
                    TextEditorDialog().configEditText {
                        it.setMaxLength(64)
                    }.setArguments(option.currentTitle!!, applet.value?.casted()) {
                        if (it.isBlank()) {
                            return@setArguments R.string.error_empty_input.str
                        }
                        applet.value = it
                        onCompleted()
                        return@setArguments null
                    }.apply {
                        if (option.hasPresets) {
                            setDropDownData(
                                option.presetsNameRes.array, option.presetsValueRes.array
                            )
                        }
                    }.show(fragmentManager)
                }
            }

        }
    }
}