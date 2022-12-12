package top.xjunz.tasker.ui.task.editor

import android.view.Gravity
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentManager
import top.xjunz.tasker.Preferences
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.ControlFlow
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.applet.serialization.AppletValues
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.task.applet.isContainer
import top.xjunz.tasker.task.applet.option.AppletOptionFactory
import top.xjunz.tasker.ui.common.PreferenceHelpDialog
import top.xjunz.tasker.ui.common.TextEditorDialog
import top.xjunz.tasker.ui.demo.LongClickToSelectDemo
import top.xjunz.tasker.ui.demo.SwipeToRemoveDemo
import top.xjunz.tasker.ui.task.selector.AppletOptionOnClickListener
import top.xjunz.tasker.ui.task.selector.AppletSelectorDialog
import top.xjunz.tasker.ui.widget.PopupListMenu

/**
 * @author xjunz 2022/11/08
 */
class FlowItemMenuHelper(
    val viewModel: FlowEditorViewModel,
    val factory: AppletOptionFactory,
    private val fm: FragmentManager
) {

    private val optionOnClickListener by lazy {
        AppletOptionOnClickListener(fm, factory)
    }

    fun showMenu(anchor: View, applet: Applet): PopupMenu {
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
                val index = popup.indexOf(it) - 1
                if (index >= 0) {
                    val newApplet = registry.allOptions[index].yield()
                    parent[applet.index] = newApplet
                    viewModel.regenerateApplets()
                    viewModel.onAppletChanged.value = newApplet
                }
                return@l true
            }
        } else {
            popup.menuInflater.inflate(R.menu.flow_editor, menu)
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
            if (applet.valueType == AppletValues.VAL_TYPE_IRRELEVANT && option.arguments.isEmpty()) {
                menu.removeItem(R.id.item_edit)
            }
            // Not movable
            if (applet.requiredIndex != -1) {
                menu.removeItem(R.id.item_remove)
                menu.removeItem(R.id.item_toggle_ability)
            }
            // Not child addable
            if (applet !is Flow || applet.size == applet.maxSize) {
                menu.removeItem(R.id.item_add_inside)
            }
            if (applet is ControlFlow) {
                if (factory.flowRegistry.getPeerOptions(applet, true).isEmpty()) {
                    menu.removeItem(R.id.item_add_before)
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
                onFlowMenuItemClick(anchor, applet, it.itemId, it)
            }
        }
        popup.show()
        return popup
    }

    fun onFlowMenuItemClick(
        anchor: View,
        applet: Applet,
        id: Int,
        item: MenuItem? = null
    ): Boolean {
        if (id == 0) return false
        when (id) {
            R.id.item_split -> viewModel.splitContainerFlow(applet as Flow)
            R.id.item_edit -> optionOnClickListener.onClick(applet) {
                viewModel.onAppletChanged.value = applet
            }
            R.id.item_add_comment -> {
                TextEditorDialog().init(item?.title, applet.comment) {
                    if (it.isEmpty()) {
                        applet.comment = null
                    } else {
                        applet.comment = it
                    }
                    viewModel.onAppletChanged.value = applet
                    return@init null
                }.setAllowEmptyInput().configEditText {
                    it.configInputType(String::class.java, true)
                    it.maxLines = 10
                }.show(fm)
            }
            R.id.item_invert -> {
                applet.toggleInversion()
                viewModel.onAppletChanged.value = applet
            }
            R.id.item_remove -> {
                PreferenceHelpDialog().init(
                    R.string.prompt,
                    R.string.help_swipe_to_remove,
                    Preferences.showSwipeToRemoveDemo
                ) {
                    Preferences.showSwipeToRemoveDemo = !it
                    viewModel.removeApplet(applet)
                }.setDemonstration {
                    SwipeToRemoveDemo(it)
                }.show(fm)
            }
            R.id.item_select -> {
                PreferenceHelpDialog().init(
                    R.string.select,
                    R.string.help_long_click_to_select,
                    Preferences.showLongClickToSelectDemo
                ) {
                    Preferences.showLongClickToSelectDemo = !it
                    viewModel.toggleMultiSelection(applet)
                }.setDemonstration {
                    LongClickToSelectDemo(it)
                }.show(fm)
            }
            R.id.item_toggle_ability -> {
                applet.toggleAbility()
                if (applet is Flow) {
                    viewModel.notifyFlowAbilityChanged(applet)
                } else {
                    viewModel.onAppletChanged.value = applet
                }
            }
            R.id.item_add_inside -> {
                val flow = applet as Flow
                if (flow.size == flow.maxSize) {
                    toast(R.string.reach_max_applet_size)
                    return true
                }
                AppletSelectorDialog().doOnCompletion {
                    if (flow.size + it.size > Applet.MAX_FLOW_CHILD_COUNT) {
                        toast(R.string.over_max_applet_size)
                        return@doOnCompletion
                    }
                    val last = flow.lastOrNull()
                    flow.addAll(it)
                    // Notify divider changed
                    viewModel.onAppletChanged.value = last
                    // Notify action icon changed
                    if (flow.size == it.size) {
                        viewModel.onAppletChanged.value = flow
                    }
                    viewModel.notifyFlowChanged()
                }.scopedBy(flow).show(fm)
            }
            R.id.item_add_before, R.id.item_add_after -> {
                val addBefore = id == R.id.item_add_before
                if (applet is ControlFlow) {
                    val popup = PopupMenu(
                        anchor.context, anchor, Gravity.END, 0, R.style.FlowEditorPopupMenuStyle
                    )
                    popup.menu.add(item?.title)
                    val options = viewModel.factory.flowRegistry.getPeerOptions(applet, addBefore)
                    options.forEach {
                        popup.menu.add(it.rawTitle)
                    }
                    popup.setOnMenuItemClickListener {
                        val index = popup.indexOf(it) - 1
                        if (index >= 0) {
                            val parent = applet.requireParent()
                            val yielded = options[index].yield()
                            if (addBefore) {
                                parent.add(applet.index, yielded)
                            } else if (applet.index == parent.lastIndex) {
                                parent.add(yielded)
                            } else {
                                parent.add(applet.index + 1, yielded)
                            }
                            viewModel.notifyFlowChanged()
                        }
                        return@setOnMenuItemClickListener true
                    }
                    popup.show()
                    popup.configHeaderTitle()
                } else {
                    val flow = applet.requireParent()
                    if (flow.size == flow.maxSize) {
                        toast(R.string.reach_max_applet_size)
                        return true
                    }
                    AppletSelectorDialog().doOnCompletion { applets ->
                        if (flow.size + applets.size > Applet.MAX_FLOW_CHILD_COUNT) {
                            toast(R.string.over_max_applet_size)
                            return@doOnCompletion
                        }
                        if (addBefore) {
                            flow.addAll(applet.index, applets)
                            viewModel.updateChildrenIndexesIfNeeded(flow)
                        } else {
                            flow.addAll(applets)
                            // Divider changed
                            viewModel.onAppletChanged.value = applet
                        }
                        viewModel.notifyFlowChanged()
                    }.scopedBy(flow).show(fm)
                }
            }
        }
        return true
    }
}