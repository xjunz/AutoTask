/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.selector

import androidx.fragment.app.FragmentManager
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.action.Action
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.criterion.PropertyCriterion
import top.xjunz.tasker.engine.applet.util.isAttached
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.task.applet.criterion.BoundsCriterion
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.task.applet.option.AppletOptionFactory
import top.xjunz.tasker.task.applet.util.IntValueUtil
import top.xjunz.tasker.ui.common.TextEditorDialog
import top.xjunz.tasker.ui.task.selector.argument.*

/**
 * @author xjunz 2022/10/08
 */
open class AppletOptionClickHandler(private val fragmentManager: FragmentManager) {

    private val factory = AppletOptionFactory

    private val actionOptionClickHandler = ActionOptionClickHandler(fragmentManager)

    fun onClick(applet: Applet, onCompleted: () -> Unit) {
        onClick(applet, factory.requireOption(applet), onCompleted)
    }

    open fun onClick(applet: Applet, option: AppletOption, onCompleted: () -> Unit) {
        val title = option.loadTitle(applet)!!
        when {
            option.arguments.isNotEmpty() -> ArgumentsEditorDialog()
                .setAppletOption(applet, option)
                .doOnCompletion(onCompleted).show(fragmentManager)

            option.isValueInnate -> onCompleted()

            applet is Action<*> -> actionOptionClickHandler.onClick(applet, option, onCompleted)

            option == factory.timeRegistry.timeRange -> {
                val value = applet.value?.casted<Collection<Long>>()
                DateTimeRangeEditorDialog().setRange(
                    value?.firstOrNull(), value?.lastOrNull(),
                    System.currentTimeMillis(), System.currentTimeMillis()
                ).setType(Applet.VAL_TYPE_LONG).setTitle(title)
                    .doOnCompletion { start, end ->
                        applet.value = listOf(start, end)
                        onCompleted()
                    }.show(fragmentManager)
            }

            option == factory.timeRegistry.hourMinSec -> {
                val value = applet.value?.casted<Collection<Int>>()
                TimeRangeEditorDialog().setRange(
                    value?.firstOrNull(), value?.lastOrNull(),
                    0, IntValueUtil.composeTime(23, 59, 59)
                ).setTitle(title).doOnCompletion { start, end ->
                    applet.value = listOf(start, end)
                    onCompleted()
                }.show(fragmentManager)
            }

            option == factory.timeRegistry.dayOfMonth -> {
                val days = Array<CharSequence>(31) { i ->
                    (i + 1).toString()
                }
                EnumSelectorDialog().init(title, days) {
                    applet.value = it
                    onCompleted()
                }.setSpanCount(4).setInitialSelections(applet.value?.casted()).show(fragmentManager)
            }

            option == factory.timeRegistry.dayOfWeek -> {
                EnumSelectorDialog().init(title, R.array.days_in_week) {
                    applet.value = it
                    onCompleted()
                }.setInitialSelections(applet.value?.casted()).show(fragmentManager)
            }

            option == factory.timeRegistry.month -> {
                EnumSelectorDialog().init(title, R.array.months) {
                    applet.value = it
                    onCompleted()
                }.setInitialSelections(applet.value?.casted()).show(fragmentManager)
            }

            applet is PropertyCriterion<*> -> {
                if (applet.isAttached) applet.toggleInversion()
                onCompleted()
            }

            applet is BoundsCriterion<*> -> {
                DistanceEditorDialog().setArguments(title) {
                    applet.value = it
                    onCompleted()
                }.setDistance(applet.value?.casted()).setDirection(applet.direction)
                    .show(fragmentManager)
            }

            applet.valueType == Applet.VAL_TYPE_TEXT -> {
                val dialog = TextEditorDialog().configEditText {
                    it.setMaxLength(128)
                }.init(title, applet.value?.casted()) {
                    applet.value = it
                    onCompleted()
                    return@init null
                }
                if (option.hasPresets) dialog.setDropDownData(
                    option.presetsNameRes.array, option.presetsValueRes.array
                )
                dialog.show(fragmentManager)
            }

            applet.valueType == Applet.VAL_TYPE_INT -> {
                TextEditorDialog().configEditText {
                    it.setMaxLength(16)
                    it.configInputType(Int::class.java)
                }.setCaption(option.helpText).init(title, applet.value?.toString()) {
                    val parsed = it.toIntOrNull() ?: return@init R.string.error_mal_format.text
                    applet.value = parsed
                    onCompleted()
                    return@init null
                }.show(fragmentManager)
            }
            else -> onCompleted()
        }
    }
}