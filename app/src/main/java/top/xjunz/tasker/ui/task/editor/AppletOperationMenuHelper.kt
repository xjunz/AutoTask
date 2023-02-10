/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.editor

import android.view.Gravity
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentManager
import top.xjunz.tasker.Preferences
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.base.*
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.task.applet.isContainer
import top.xjunz.tasker.task.applet.option.AppletOptionFactory
import top.xjunz.tasker.ui.common.PreferenceHelpDialog
import top.xjunz.tasker.ui.common.TextEditorDialog
import top.xjunz.tasker.ui.demo.DragToMoveDemo
import top.xjunz.tasker.ui.demo.LongClickToSelectDemo
import top.xjunz.tasker.ui.demo.SwipeToRemoveDemo
import top.xjunz.tasker.ui.task.selector.AppletOptionClickHandler
import top.xjunz.tasker.ui.task.selector.AppletSelectorDialog
import top.xjunz.tasker.ui.widget.PopupListMenu
import java.util.*

/**
 * @author xjunz 2022/11/08
 */
class AppletOperationMenuHelper(
    private val viewModel: FlowEditorViewModel,
    private val fm: FragmentManager
) {

    private val factory = AppletOptionFactory

    private val optionOnClickListener by lazy {
        AppletOptionClickHandler(fm)
    }

    fun createBatchMenu(anchor: View, applets: List<Applet>): PopupMenu {
        val popup = PopupListMenu(anchor.context, anchor, Gravity.END)
        popup.inflate(R.menu.applet_batch_operation)
        if (applets.size < 2) {
            popup.menu.removeItem(R.id.item_merge)
        }
        popup.setOnMenuItemClickListener {
            return@setOnMenuItemClickListener onBatchMenuItemClick(applets, it.itemId, it.title)
        }
        return popup
    }

    fun createStandaloneMenu(anchor: View, applet: Applet): PopupMenu {
        val popup = PopupListMenu(
            anchor.context, anchor, Gravity.END, 0, R.style.FlowEditorPopupMenuStyle
        )
        val menu = popup.menu
        val parent = applet.requireParent()
        if (parent.requiredSize == 1) {
            val registry = viewModel.factory.requireRegistryById(applet.registryId)
            menu.add(R.string.replace_with)
            registry.allOptions.forEachIndexed { index, appletOption ->
                menu.add(0, index + 1, index, appletOption.rawTitle)
            }
            popup.setOnMenuItemClickListener l@{
                val index = menu.indexOf(it) - 1
                if (index >= 0) {
                    val newApplet = registry.allOptions[index].yield()
                    parent[applet.index] = newApplet
                    viewModel.regenerateApplets()
                    viewModel.onAppletChanged.value = newApplet
                }
                return@l true
            }
        } else {
            popup.inflate(R.menu.applet_operation)
            // Not container
            if (!applet.isContainer) {
                menu.removeItem(R.id.item_split)
            }
            // Not invertible
            if (!applet.isInvertible) {
                menu.removeItem(R.id.item_invert)
            }
            val option = factory.requireOption(applet)
            // Not editable
            if (applet.valueType == Applet.VAL_TYPE_IRRELEVANT && option.arguments.isEmpty()) {
                menu.removeItem(R.id.item_edit)
            }
            // Not removable
            if (applet.requiredIndex != -1) {
                menu.removeItem(R.id.item_remove)
                menu.removeItem(R.id.item_move)
                // Disabled item is regarded as removed, so remove ability toggle
                menu.removeItem(R.id.item_toggle_ability)
            }
            // Not movable
            if (applet is ControlFlow || applet.parent?.size == 1) {
                menu.removeItem(R.id.item_move)
            }
            // Not child addable
            if (applet !is Flow || applet.size == applet.maxSize) {
                menu.removeItem(R.id.item_add_inside)
            }
            if (!Preferences.showDragToMoveTip) {
                menu.removeItem(R.id.item_move)
            }
            if (!Preferences.showSwipeToRemoveTip) {
                menu.removeItem(R.id.item_remove)
            }
            if (!Preferences.showLongClickToSelectTip) {
                menu.removeItem(R.id.item_select)
            }
            if (applet !is Flow) {
                menu.removeGroup(R.id.group_more)
            }
            if (applet is ControlFlow) {
                if (factory.flowRegistry.getPeerOptions(applet, true).isEmpty()) {
                    menu.removeItem(R.id.item_add_before)
                }
                if (applet is Do
                    && applet.requireParent().getOrNull(applet.index - 1) !is If
                ) {
                    menu.removeItem(R.id.item_add_after)
                }
                if (factory.flowRegistry.getPeerOptions(applet, false).isEmpty()) {
                    menu.removeItem(R.id.item_add_after)
                }
            }
            if (applet.comment != null) {
                menu.findItem(R.id.item_add_comment).title = R.string.edit_comment.text
            }
            if (!applet.isEnabled) {
                menu.findItem(R.id.item_toggle_ability).title = R.string.enable.text
            }
            popup.setOnMenuItemClickListener {
                triggerMenuItem(anchor, applet, it.itemId, it.title)
            }
        }
        return popup
    }

    private fun onBatchMenuItemClick(
        applets: Iterable<Applet>,
        id: Int,
        title: CharSequence?
    ): Boolean {
        when (id) {
            R.id.item_merge -> if (viewModel.selections.size < 2) {
                toast(R.string.error_merge_single_applet)
            } else {
                viewModel.showMergeConfirmation.value = true
            }
            R.id.item_enable, R.id.item_disable -> applets.forEach {
                if (it.requiredIndex != -1) return@forEach
                it.isEnabled = id == R.id.item_enable
                if (it is Flow) {
                    viewModel.notifyFlowAbilityChanged(it)
                } else {
                    viewModel.onAppletChanged.value = it
                }
            }
            R.id.item_invert -> applets.forEach {
                if (it.isInvertible) {
                    it.toggleInversion()
                    viewModel.onAppletChanged.value = it
                }
            }
            R.id.item_remove ->
                PreferenceHelpDialog().init(R.string.tip, R.string.tip_swipe_to_remove) {
                    Preferences.showSwipeToRemoveTip = !it
                }.setDemonstration {
                    SwipeToRemoveDemo(it)
                }.show(fm)
            R.id.item_add_comment -> {
                val defText = applets.map { it.comment }.distinct()
                TextEditorDialog().init(title, defText.singleOrNull()) { comment ->
                    applets.forEach {
                        if (comment.isEmpty()) it.comment = null else it.comment = comment
                        viewModel.onAppletChanged.value = it
                    }
                    return@init null
                }.setAllowEmptyInput().configEditText {
                    it.configInputType(String::class.java, true)
                    it.maxLines = 10
                }.show(fm)
            }
            else -> return false
        }
        return true
    }

    fun triggerMenuItem(
        anchor: View?,
        applet: Applet,
        id: Int,
        title: CharSequence? = null,
    ): Boolean {
        if (id == 0) return false
        if (onBatchMenuItemClick(Collections.singleton(applet), id, title)) return true
        when (id) {
            R.id.item_open_in_new -> {
                val dialog = FlowEditorDialog().init(
                    viewModel.task,
                    applet as Flow,
                    viewModel.isReadyOnly,
                    viewModel.global
                ).doAfterFlowEdited { edited ->
                    if (edited.isEmpty() && applet.isContainer) {
                        // If all children in a container is removed, remove the container as well
                        applet.requireParent().remove(applet)
                        viewModel.updateChildrenIndexesIfNeeded(applet.requireParent())
                    } else {
                        // We don't need to replace the flow, just refilling it is ok
                        applet.clear()
                        applet.addAll(edited)
                        if (edited.isNotEmpty()) viewModel.clearStaticErrorIfNeeded(
                            applet, StaticError.PROMPT_ADD_INSIDE
                        )
                        viewModel.onAppletChanged.value = applet
                    }
                    viewModel.notifyFlowChanged()
                }.setStaticError(viewModel.staticError).doSplit {
                    viewModel.splitContainerFlow(applet)
                }
                if (viewModel.isSelectingArgument) {
                    dialog.doOnArgumentSelected(viewModel.doOnArgSelected)
                    dialog.setArgumentToSelect(
                        viewModel.referentAnchor, viewModel.argumentDescriptor, null
                    )
                }
                if (viewModel.isInTrackMode) {
                    dialog.setTrackMode()
                }
                dialog.show(fm)
            }
            R.id.item_split -> viewModel.splitContainerFlow(applet as Flow)
            R.id.item_edit -> optionOnClickListener.onClick(applet) {
                viewModel.onAppletChanged.value = applet
            }
            R.id.item_toggle_ability -> {
                applet.toggleAbility()
                if (applet is Flow) {
                    viewModel.notifyFlowAbilityChanged(applet)
                } else {
                    viewModel.onAppletChanged.value = applet
                }
            }
            R.id.item_move -> PreferenceHelpDialog().init(R.string.tip, R.string.tip_drag_to_move) {
                Preferences.showDragToMoveTip = !it
            }.setDemonstration {
                DragToMoveDemo(it)
            }.show(fm)
            R.id.item_select -> PreferenceHelpDialog().init(
                R.string.tip, R.string.tip_long_click_to_select
            ) {
                Preferences.showLongClickToSelectTip = !it
            }.setDemonstration {
                LongClickToSelectDemo(it)
            }.show(fm)

            R.id.item_add_inside -> {
                val flow = applet as Flow
                if (flow.size == flow.maxSize) {
                    toast(R.string.format_error_reach_max_applet_size.format(flow.maxSize))
                    return true
                }
                AppletSelectorDialog().init(flow) {
                    viewModel.addInside(flow, it)
                }.show(fm)
            }
            R.id.item_add_before, R.id.item_add_after -> {
                val addBefore = id == R.id.item_add_before
                if (applet is ControlFlow) {
                    val popup = PopupMenu(
                        anchor!!.context, anchor, Gravity.END, 0, R.style.FlowEditorPopupMenuStyle
                    )
                    popup.menu.add(title)
                    val options = viewModel.factory.flowRegistry.getPeerOptions(applet, addBefore)
                    options.forEach {
                        popup.menu.add(it.rawTitle)
                    }
                    val listener = PopupMenu.OnMenuItemClickListener {
                        val index = popup.menu.indexOf(it) - 1
                        if (index >= 0) {
                            val yielded = options[index].yield()
                            if (addBefore) {
                                viewModel.addBefore(applet, Collections.singletonList(yielded))
                            } else {
                                viewModel.addAfter(applet, Collections.singletonList(yielded))
                            }
                        }
                        true
                    }
                    if (options.size == 1) {
                        listener.onMenuItemClick(popup.menu.getItem(1))
                    } else {
                        popup.setOnMenuItemClickListener(listener)
                        popup.show()
                        popup.configHeaderTitle()
                    }
                } else {
                    val flow = applet.requireParent()
                    if (flow.size == flow.maxSize) {
                        toast(R.string.format_error_reach_max_applet_size.format(flow.maxSize))
                        return true
                    }
                    AppletSelectorDialog().init(flow) { peers ->
                        if (addBefore) {
                            viewModel.addBefore(applet, peers)
                        } else {
                            viewModel.addAfter(applet, peers)
                        }
                    }.show(fm)
                }
            }
        }
        return true
    }
}