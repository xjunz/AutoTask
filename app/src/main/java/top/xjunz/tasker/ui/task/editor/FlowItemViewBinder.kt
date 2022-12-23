package top.xjunz.tasker.ui.task.editor

import android.annotation.SuppressLint
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.google.android.material.R.style.*
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.base.*
import top.xjunz.tasker.engine.applet.dto.AppletValues
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.task.applet.*
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.ui.ColorScheme

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
        const val ACTION_ADD = 4
    }

    private val errorPrompts by lazy {
        R.array.static_error_prompts.array
    }

    @SuppressLint("SetTextI18n")
    fun bindViewHolder(holder: TaskFlowAdapter.FlowViewHolder, applet: Applet) {
        val option = viewModel.factory.requireOption(applet)
        holder.binding.apply {
            root.translationX = 0F
            root.isSelected = viewModel.isSelected(applet)
                    || (viewModel.isSelectingRef && globalViewModel.isRefSelected(applet))
            root.isActivated = viewModel.staticError?.victim === applet
            root.isEnabled = true

            ibAction.tag = null

            tvNumber.isVisible = false
            dividerTop.isVisible = false
            dividerBott.isVisible = false

            cgRefids.isVisible = false
            tvTitle.isEnabled = true
            bullet.isVisible = false
            // Don't show innate value
            var desc = if (option.isValueInnate) null else option.describe(applet)
            var title = option.getTitle(applet) ?: applet.comment
            tvTitle.isVisible = true
            if (option.descAsTitle) {
                title = desc
            } else if (applet.isContainer) {
                title = if (applet.controlFlow is If) {
                    R.string.matches_rule_set.text
                } else {
                    R.string.execute_rule_set.text
                }
            }
            if (title != null && applet.index != 0 && applet !is ControlFlow) {
                title = AppletOption.makeRelationSpan(
                    title, applet, applet.controlFlow is If
                )
            }

            val depth = applet.depthInAncestor(viewModel.flow)
            // Set text style
            when (depth) {
                1 -> tvTitle.setTextAppearance(TextAppearance_Material3_TitleLarge)

                2 -> {
                    tvTitle.setTextAppearance(TextAppearance_Material3_TitleMedium)
                    bullet.isVisible = true
                }
                else -> {
                    dividerTop.isVisible = true
                    tvNumber.isVisible = true
                    dividerBott.isVisible = applet.index != applet.parent?.lastIndex
                    tvNumber.text = (applet.index + 1).toString()
                    tvTitle.setTextAppearance(TextAppearance_Material3_LabelLarge)
                    // tvTitle.setTextColor(ColorScheme.textColorPrimary)
                    if (applet is Flow) {
                        val size = applet.size.toString().foreColored()
                        desc = R.string.format_applet_count.formatSpans(size)
                    }
                }
            }
            if (applet is ControlFlow) {
                tvTitle.setTextColor(R.color.color_text_control_normal.colorStateList)
            } else if (depth == 1) {
                title = title?.relativeSize(.8F)
            }
            // Set action
            if (applet is Flow) {
                if (depth > 2) {
                    ibAction.tag = ACTION_ENTER
                    ibAction.setImageResource(R.drawable.ic_baseline_chevron_right_24)
                    ibAction.setContentDescriptionAndTooltip(R.string.enter.text)
                    tvDesc.isVisible = true
                } else if (applet.isEmpty()) {
                    ibAction.tag = ACTION_ADD
                    ibAction.setContentDescriptionAndTooltip(R.string.add_inside.text)
                    ibAction.setImageResource(R.drawable.ic_baseline_add_24)
                } else {
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
                if (option.arguments.isNotEmpty() || (applet.value != null && !option.isValueInnate)) {
                    ibAction.tag = ACTION_EDIT
                    ibAction.setImageResource(R.drawable.ic_edit_24dp)
                    ibAction.setContentDescriptionAndTooltip(R.string.edit.text)
                } else if (applet.isInvertible) {
                    ibAction.tag = ACTION_INVERT
                    ibAction.setImageResource(R.drawable.ic_baseline_switch_24)
                    ibAction.setContentDescriptionAndTooltip(R.string.invert.text)
                }
            }
            if (applet.valueType == AppletValues.VAL_TYPE_TEXT) {
                desc = desc?.quoted()
                tvDesc.setBackgroundColor(ColorScheme.colorPrimaryContainer.alphaModified(.3))
            } else {
                tvDesc.background = null
            }
            if (viewModel.isSelectingRef) {
                title = title?.toString()
                if (!tvDesc.isEnabled) {
                    desc = desc?.toString()
                }
                val isAhead = viewModel.refSelectingApplet.parent == null
                        || applet.isAheadOf(viewModel.refSelectingApplet)
                // When selecting ref, only enable valid targets
                val ref = if (!isAhead) null else option.results.find {
                    it.type == viewModel.refValueDescriptor.type
                }
                if (applet.isContainer && depth == 3) {
                    root.isEnabled = isAhead && viewModel.hasCandidateReference(applet as Flow)
                } else {
                    root.isEnabled = ref != null
                    cgRefids.isVisible = root.isEnabled
                }
                tvTitle.isEnabled = root.isEnabled
                if (ref != null) {
                    val refid = applet.refids[option.results.indexOf(ref)]
                    if (refid != null) {
                        tvBadge.text = ref.name + " [$refid]".foreColored(ColorScheme.colorTertiary)
                    } else {
                        tvBadge.text = ref.name
                    }
                }
            } else {
                cgRefids.isVisible = false
            }

            if (!applet.isEnabledInHierarchy) title = title?.strikeThrough()

            ibAction.isGone =
                ibAction.tag == null || (viewModel.isSelectingRef && ibAction.tag != ACTION_COLLAPSE)
            tvTitle.text = title
            tvDesc.isVisible = !option.descAsTitle && !desc.isNullOrEmpty()
            tvDesc.text = desc
            tvComment.isVisible = applet.comment != null || root.isActivated
            if (root.isActivated) {
                var prompt = errorPrompts[viewModel.staticError!!.code]
                viewModel.staticError?.arg?.let {
                    prompt = prompt.toString().format(it)
                }
                tvComment.text = (R.string.error.text.bold() + prompt).foreColored(
                    ColorScheme.colorError
                )
            } else if (applet.comment != null) {
                tvComment.text = (R.string.comment.text.bold() + applet.comment!!)
                    .quoted(ColorScheme.colorTertiaryContainer)
            }
        }
    }
}