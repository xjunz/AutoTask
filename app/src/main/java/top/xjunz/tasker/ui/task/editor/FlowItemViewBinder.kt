package top.xjunz.tasker.ui.task.editor

import android.annotation.SuppressLint
import androidx.core.graphics.ColorUtils
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.google.android.material.R.style.*
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.ControlFlow
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.applet.base.If
import top.xjunz.tasker.engine.applet.serialization.AppletValues
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.task.applet.*
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.ui.ColorSchemes

/**
 * @author xjunz 2022/11/07
 */
class FlowItemViewBinder(
    private val viewModel: FlowEditorViewModel,
    private val globalViewModel: GlobalFlowEditorViewModel
) {

    companion object {
        const val ACTION_COLLAPSE = 0
        const val ACTION_INVERT = 1
        const val ACTION_ENTER = 2
        const val ACTION_EDIT = 3
    }

    private inline val factory get() = viewModel.factory

    @SuppressLint("SetTextI18n")
    fun bindViewHolder(holder: TaskFlowAdapter.FlowViewHolder, applet: Applet) {
        val option = if (applet.id == -1) null else factory.requireOption(applet)
        holder.binding.apply {
            root.translationX = 0F
            root.isSelected = viewModel.isSelected(applet)
                    || (viewModel.isSelectingRef && globalViewModel.isRefSelected(applet))
            tvNumber.isVisible = false
            dividerTop.isVisible = false
            dividerBott.isVisible = false
            cgRefids.isVisible = false
            root.isEnabled = true
            tvTitle.isEnabled = true
            if (option != null) {
                if (viewModel.isSelectingRef) {
                    val isAhead = viewModel.refSelectingApplet.parent == null
                            || applet.isAheadOf(viewModel.refSelectingApplet)
                    // When selecting ref, only enable valid targets
                    val ref = if (!isAhead) null else option.results.find {
                        it.type == viewModel.refValueDescriptor.type
                    }
                    if (applet.isContainer) {
                        root.isEnabled = isAhead && viewModel.hasCandidateReference(applet as Flow)
                    } else {
                        root.isEnabled = ref != null
                        cgRefids.isVisible = root.isEnabled
                    }
                    tvTitle.isEnabled = root.isEnabled
                    if (ref != null) {
                        val refid = applet.refids[option.results.indexOf(ref)]
                        if (refid != null) {
                            tvBadge.text =
                                ref.name + "[$refid]".foreColored(ColorSchemes.colorTertiary)
                        } else {
                            tvBadge.text = ref.name
                        }
                    }
                } else {
                    cgRefids.isVisible = false
                }
                var desc = option.describe(applet)
                var title = option.getTitle(applet) ?: applet.label
                tvTitle.isVisible = true
                if (option.descAsTitle) {
                    title = desc
                } else if (applet.isContainer) {
                    title = if (applet.controlFlowParent is If) {
                        R.string.matches_rule_set.text
                    } else {
                        R.string.execute_rule_set.text
                    }
                }
                if (title != null && applet.index != 0 && applet !is ControlFlow) {
                    title = AppletOption.makeRelationSpan(
                        title, applet, applet.controlFlowParent is If
                    )
                }

                // Clear span if is selected ref
                if (viewModel.isSelectingRef) title = title?.toString()

                if (applet is ControlFlow) {
                    tvDesc.isVisible = false
                    tvTitle.setTextAppearance(TextAppearance_Material3_TitleLarge)
                    tvTitle.setTextColor(R.color.color_text_control_normal.colorStateList)
                } else if (applet.nonContainerParent is ControlFlow) {
                    tvTitle.setTextAppearance(TextAppearance_Material3_TitleMedium)
                    tvTitle.setTextColor(ColorSchemes.textColorPrimary)
                } else {
                    dividerTop.isVisible = true
                    tvNumber.isVisible = true
                    dividerBott.isVisible = applet.index != applet.parent?.lastIndex
                    tvNumber.text = (applet.index + 1).toString()
                    tvTitle.setTextAppearance(TextAppearance_Material3_LabelLarge)
                    tvTitle.setTextColor(ColorSchemes.textColorPrimary)
                }
                if (applet is Flow) {
                    if (applet.isContainer) {
                        ibAction.isVisible = root.isEnabled
                        ibAction.tag = ACTION_ENTER
                        ibAction.setImageResource(R.drawable.ic_baseline_chevron_right_24)
                        ibAction.setContentDescriptionAndTooltip(R.string.enter.text)
                        tvDesc.isVisible = true
                    } else {
                        ibAction.isVisible = true
                        ibAction.tag = ACTION_COLLAPSE

                        if (viewModel.isCollapsed(applet)) {
                            ibAction.setContentDescriptionAndTooltip(R.string.expand_more.text)
                            ibAction.setImageResource(R.drawable.ic_baseline_expand_more_24)
                        } else {
                            ibAction.setContentDescriptionAndTooltip(R.string.unfold_less.text)
                            ibAction.setImageResource(R.drawable.ic_baseline_expand_less_24)
                        }
                    }
                } else {
                    ibAction.isInvisible = viewModel.isSelectingRef
                    if (applet.isInvertible) {
                        ibAction.tag = ACTION_INVERT
                        ibAction.setImageResource(R.drawable.ic_baseline_switch_24)
                        ibAction.setContentDescriptionAndTooltip(R.string.invert.text)
                    } else if (option.arguments.isNotEmpty() || option.value != null) {
                        ibAction.tag = ACTION_EDIT
                        ibAction.setImageResource(R.drawable.ic_edit)
                        ibAction.setContentDescriptionAndTooltip(R.string.edit.text)
                    } else {
                        ibAction.isVisible = false
                    }
                }
                if (applet.valueType == AppletValues.VAL_TYPE_TEXT) {
                    desc = desc?.quoted()
                    tvDesc.setBackgroundColor(
                        ColorUtils.setAlphaComponent(
                            ColorSchemes.colorPrimaryContainer, (0.25F * 0xFF).toInt()
                        )
                    )
                } else {
                    tvDesc.background = null
                }
                // Clear spans
                if (!tvTitle.isEnabled) title = title?.toString()
                tvTitle.text = title
                tvDesc.isVisible = !option.descAsTitle && !desc.isNullOrEmpty()
                tvDesc.text = desc
            } else {
                tvDesc.isVisible = false
                tvTitle.isVisible = false
            }
        }
    }
}