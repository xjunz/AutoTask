package top.xjunz.tasker.ui.task.editor

import android.view.Gravity
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.ControlFlow
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.applet.base.When
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.ui.task.selector.AppletSelectorDialog

/**
 * @author xjunz 2022/11/08
 */
class FlowItemMenuHelper(val viewModel: TaskEditorViewModel, val fragment: Fragment) {

    fun showMenu(anchor: View, applet: Applet): PopupMenu? {
        val popup = PopupMenu(
            anchor.context, anchor, Gravity.END, 0, R.style.FlowEditorPopupMenuStyle
        )
        val parent = applet.requireParent()
        if (parent is ControlFlow && parent.requiredElementCount == 1) {
            val registry = viewModel.appletOptionFactory.findRegistryById(applet.registryId)
            popup.menu.add(R.string.replace_with)
            registry.allOptions.forEach {
                popup.menu.add(it.title)
            }
            popup.setOnMenuItemClickListener l@{
                val index = popup.indexOf(it) - 1
                if (index >= 0) {
                    parent[applet.index] = registry.allOptions[index].yieldApplet()
                    viewModel.notifyFlowChanged()
                }
                return@l true
            }
        } else if (applet is Flow) {
            popup.menuInflater.inflate(R.menu.flow_editor, popup.menu)
            if (applet is When) {
                popup.menu.findItem(R.id.item_add_inside).isEnabled = false
                popup.menu.findItem(R.id.item_add_before).isEnabled = false
            }
            popup.setOnMenuItemClickListener { onFlowMenuItemClick(anchor, it, applet) }
        } else {
            return null
        }
        popup.show()
        popup.configHeaderTitle()
        return popup
    }

    private fun onFlowMenuItemClick(anchor: View, item: MenuItem, applet: Applet): Boolean {
        if (item.itemId != 0) {
            val addInside = item.itemId == R.id.item_add_inside
            val addBefore = item.itemId == R.id.item_add_before
            val addAfter = item.itemId == R.id.item_add_after
            var flow = applet as Flow
            if (addInside || flow !is ControlFlow) {
                if (flow !is ControlFlow && (addBefore || addAfter)) {
                    flow = flow.requireParent()
                }
                if (flow.size == Applet.MAX_FLOW_CHILD_COUNT) {
                    toast(R.string.reach_max_applet_size)
                    return true
                }
                if (flow is ControlFlow && flow.size == flow.requiredElementCount) {
                    toast(R.string.format_reach_required_applet_size.format(flow.requiredElementCount))
                    return true
                }
                AppletSelectorDialog().setTitle(item.title!!).doOnCompletion {
                    if (flow.size + it.size > Applet.MAX_FLOW_CHILD_COUNT) {
                        toast(R.string.over_max_applet_size)
                        return@doOnCompletion
                    }
                    if (addBefore) {
                        flow.addAll(applet.index, it)
                    } else {
                        flow.addAll(it)
                    }
                    if (addBefore) {
                        applet.index = applet.requireParent().indexOf(applet)
                        viewModel.changedApplet.value = applet
                    }
                    viewModel.notifyFlowChanged()
                }.scopedBy(flow).show(fragment.parentFragmentManager)
            } else {
                // Control flow add before/after
                val popup = PopupMenu(
                    anchor.context, anchor, Gravity.END, 0, R.style.FlowEditorPopupMenuStyle
                )
                popup.menu.add(item.title)
                val options =
                    viewModel.appletOptionFactory.flowRegistry.getPeerOptions(flow, addBefore)
                options.forEach {
                    popup.menu.add(it.title)
                }
                popup.setOnMenuItemClickListener {
                    val index = popup.indexOf(it) - 1
                    if (index >= 0) {
                        val parent = applet.requireParent()
                        val yielded = options[index].yieldApplet()
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
            }
            return true
        } else {
            return false
        }
    }
}