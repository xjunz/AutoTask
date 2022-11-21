package top.xjunz.tasker.ui.task.editor

import android.annotation.SuppressLint
import android.graphics.Typeface
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.google.android.material.R.attr.selectableItemBackground
import com.google.android.material.R.style.*
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.ControlFlow
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.applet.serialization.AppletValues
import top.xjunz.tasker.ktx.format
import top.xjunz.tasker.ktx.resolvedId
import top.xjunz.tasker.ktx.setContentDescriptionAndTooltip
import top.xjunz.tasker.ktx.text
import top.xjunz.tasker.task.applet.isContainer
import top.xjunz.tasker.task.applet.isRelating
import top.xjunz.tasker.task.applet.nonContainerParent
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.ui.ColorSchemes

/**
 * @author xjunz 2022/11/07
 */
class FlowItemViewBinder(private val viewModel: FlowEditorViewModel) {

    companion object {
        const val ACTION_COLLAPSE = 0
        const val ACTION_INVERT = 1
        const val ACTION_ENTER = 2
    }

    @SuppressLint("SetTextI18n")
    fun bindViewHolder(holder: TaskFlowAdapter.FlowViewHolder, applet: Applet) {
        val option = if (applet.id == -1) null else viewModel.appletOptionFactory.findOption(applet)
        holder.binding.apply {
            root.translationX = 0F
            root.isSelected = viewModel.selectedApplet.value == applet
                    || viewModel.isMultiSelected(applet)
            tvNumber.isVisible = false
            dividerTop.isVisible = false
            dividerBott.isVisible = false
            if (option != null) {
                var desc = option.describe(applet.value)
                var title = option.getTitle(applet.isInverted) ?: applet.label
                tvTitle.isVisible = true
                if (option.descAsTitle) {
                    title = desc
                }
                if (title != null && applet.isRelating) {
                    title = AppletOption.makeRelationSpan(title, applet.isAnd)
                    tvTitle.background = AppCompatResources.getDrawable(
                        root.context, selectableItemBackground.resolvedId
                    )
                    tvTitle.isClickable = true
                } else {
                    tvTitle.background = null
                    tvTitle.isClickable = false
                }
                if (applet is ControlFlow) {
                    tvDesc.isVisible = false
                    tvTitle.setTextAppearance(TextAppearance_Material3_TitleLarge)
                    tvTitle.setTextColor(ColorSchemes.colorPrimary)
                } else if (applet.nonContainerParent is ControlFlow) {
                    tvTitle.setTextAppearance(TextAppearance_Material3_TitleMedium)
                    tvTitle.setTextColor(ColorSchemes.colorOnSurface)
                } else {
                    dividerTop.isVisible = true
                    tvNumber.isVisible = true
                    dividerBott.isVisible = applet.index != applet.parent?.lastIndex
                    tvNumber.text = (applet.index + 1).toString()
                    tvTitle.setTextAppearance(TextAppearance_Material3_LabelLarge)
                    tvTitle.setTextColor(ColorSchemes.colorOnSurface)
                }
                if (applet is Flow) {
                    if (applet.isContainer) {
                        ibAction.isVisible = true
                        ibAction.tag = ACTION_ENTER
                        ibAction.setImageResource(R.drawable.ic_baseline_chevron_right_24)
                        ibAction.setContentDescriptionAndTooltip(R.string.enter.text)
                        tvDesc.isVisible = true
                        desc = R.string.format_applet_count.format(applet.flatSize)
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
                    ibAction.isInvisible = !applet.isInvertible
                    ibAction.tag = ACTION_INVERT
                    if (applet.isInvertible) {
                        ibAction.setImageResource(R.drawable.ic_baseline_switch_24)
                        ibAction.setContentDescriptionAndTooltip(R.string.invert.text)
                    }
                }
                if (applet.valueType == AppletValues.VAL_TYPE_TEXT) {
                    tvDesc.setTypeface(null, Typeface.ITALIC)
                } else {
                    tvDesc.setTypeface(null, Typeface.NORMAL)
                }
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