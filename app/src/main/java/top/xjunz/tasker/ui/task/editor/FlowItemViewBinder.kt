/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.editor

import android.annotation.SuppressLint
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.google.android.material.R.style.*
import com.google.android.material.chip.Chip
import top.xjunz.shared.utils.illegalArgument
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.ItemFlowItemBinding
import top.xjunz.tasker.engine.applet.base.*
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.task.applet.*
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.ui.ColorScheme
import top.xjunz.tasker.util.AntiMonkeyUtil.setAntiMoneyClickListener

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

    private val controlFlowTextTint = R.color.color_text_control_normal.colorStateList

    @SuppressLint("SetTextI18n")
    fun bindViewHolder(holder: TaskFlowAdapter.FlowViewHolder, applet: Applet) {
        val option = viewModel.factory.requireOption(applet)
        holder.binding.apply {
            root.translationX = 0F
            root.isSelected = viewModel.isSelected(applet)
            root.isActivated = viewModel.staticError?.victim === applet
            root.isEnabled = true

            ibAction.tag = null

            tvNumber.isVisible = false
            dividerTop.isVisible = false
            dividerBott.isVisible = false

            tvTitle.isEnabled = true
            bullet.isVisible = false
            // Don't show innate value
            var desc = if (option.isValueInnate) null else option.describe(applet)
            var title = option.loadTitle(applet) ?: applet.comment
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
                    if (applet is Flow) {
                        val size = applet.size.toString().foreColored()
                        desc = R.string.format_applet_count.formatSpans(size)
                    }
                }
            }
            if (applet is ControlFlow) {
                tvTitle.setTextColor(controlFlowTextTint)
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
            if (applet.valueType == Applet.VAL_TYPE_TEXT) {
                desc = desc?.italic()
            }
            if (viewModel.isSelectingReferent) {
                title = title?.toString()
                if (!tvDesc.isEnabled) {
                    desc = desc?.toString()
                }
                val ahead = viewModel.referentAnchor.parent == null
                        || applet.isAheadOf(viewModel.referentAnchor)
                // When selecting ref, only enable valid targets
                val refs =
                    if (!ahead) emptyList() else option.findResults(viewModel.referentDescriptor)
                if (applet.isContainer && depth == 3) {
                    root.isEnabled = ahead && viewModel.hasCandidateReference(applet as Flow)
                } else {
                    root.isEnabled = refs.isNotEmpty()
                    containerReferents.isVisible = refs.isNotEmpty()
                }
                tvTitle.isEnabled = root.isEnabled
                if (refs.isNotEmpty()) {
                    chipReferent1.isVisible = false
                    chipReferent2.isVisible = false
                    chipReferent3.isVisible = false
                    refs.forEachIndexed { index, ref ->
                        val which = option.results.indexOf(ref)
                        val referent = applet.referents[which]
                        showReference(applet, index, which, ref.name, referent)
                    }
                }
            } else {
                containerReferents.isVisible = false
            }

            if (!applet.isEnabledInHierarchy) title = title?.strikeThrough()

            ibAction.isGone = ibAction.tag == null || (viewModel.isSelectingReferent
                    && ibAction.tag != ACTION_COLLAPSE
                    && ibAction.tag != ACTION_ENTER)
            tvTitle.text = title
            tvDesc.isVisible = !option.descAsTitle && !desc.isNullOrEmpty()
            tvDesc.text = desc
            tvComment.isVisible = applet.comment != null || root.isActivated
            if (root.isActivated) {
                var prompt = errorPrompts[viewModel.staticError!!.code]
                viewModel.staticError?.arg?.let {
                    prompt = prompt.toString().formatSpans(it.italic().underlined())
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

    private fun ItemFlowItemBinding.getReferenceChip(index: Int): Chip {
        return when (index) {
            0 -> chipReferent1
            1 -> chipReferent2
            2 -> chipReferent3
            else -> illegalArgument("chip index", index)
        }
    }

    private fun toggleSelectReference(applet: Applet, index: Int) {
        if (globalViewModel.isReferentSelected(applet, index)) {
            globalViewModel.unselectReferent(applet, index)
            if (globalViewModel.selectedReferents.isEmpty())
                viewModel.isFabVisible.value = false
        } else {
            // Remove existed reference to this applet, because multiple refs to one applet
            // is not allowed!
            if (globalViewModel.isReferentSelected(applet)) {
                globalViewModel.unselectReferent(applet)
            }
            val referent = applet.referents[index]
            if (referent == null) {
                globalViewModel.selectReferent(applet, index)
            } else {
                globalViewModel.selectReferentsWithName(viewModel.referentAnchor, referent)
            }
            if (globalViewModel.selectedReferents.isNotEmpty())
                viewModel.isFabVisible.value = true
        }
    }

    private fun ItemFlowItemBinding.showReference(
        applet: Applet,
        index: Int,
        which: Int,
        refName: CharSequence,
        referent: String?
    ) {
        val chip = getReferenceChip(index)
        chip.isVisible = true
        chip.isChecked = globalViewModel.isReferentSelected(applet, which)
        chip.setAntiMoneyClickListener {
            toggleSelectReference(applet, which)
        }
        chip.text = if (referent == null) {
            refName
        } else {
            refName + " [$referent]".foreColored()
        }
    }
}