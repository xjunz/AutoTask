/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.selector.option

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.DialogArgumentsEditorBinding
import top.xjunz.tasker.databinding.ItemArgumentEditorBinding
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.StaticError
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.task.applet.option.descriptor.ArgumentDescriptor
import top.xjunz.tasker.task.applet.option.descriptor.ValueDescriptor
import top.xjunz.tasker.task.applet.util.IntValueUtil
import top.xjunz.tasker.task.applet.value.Swipe
import top.xjunz.tasker.task.applet.value.VariantType
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.ui.base.inlineAdapter
import top.xjunz.tasker.ui.common.TextEditorDialog
import top.xjunz.tasker.ui.task.editor.FlowEditorDialog
import top.xjunz.tasker.ui.task.editor.FlowEditorViewModel
import top.xjunz.tasker.util.AntiMonkeyUtil.setAntiMoneyClickListener
import java.util.*

/**
 * @author xjunz 2022/11/22
 */
class ArgumentsEditorDialog : BaseDialogFragment<DialogArgumentsEditorBinding>() {

    override val isFullScreen: Boolean = false

    private class InnerViewModel : ViewModel() {

        lateinit var option: AppletOption

        lateinit var applet: Applet

        lateinit var doOnCompletion: () -> Unit

        val onItemChanged = MutableLiveData<ValueDescriptor>()

        fun checkForUnspecifiedArgument(): Int {
            option.arguments.forEachIndexed { which, arg ->
                val isValueSet = applet.value != null
                if (arg.isValueOnly && !isValueSet) {
                    return which
                }
                val isReferenceSet = applet.references[which] != null
                if (arg.isReferenceOnly && !isReferenceSet) {
                    return which
                }
                if (arg.isTolerant && !isValueSet && !isReferenceSet) {
                    return which
                }
            }
            return -1
        }
    }

    private inline val option get() = vm.option

    private inline val applet get() = vm.applet

    private val vm by viewModels<InnerViewModel>()

    private val pvm by lazy {
        getParentViewModel<FlowEditorDialog, FlowEditorViewModel>()
    }

    private val gvm get() = pvm.global

    private fun showValueInputDialog(which: Int, arg: ValueDescriptor) {
        when (arg.variantValueType) {
            VariantType.INT_COORDINATE -> {
                val point = applet.value?.let {
                    IntValueUtil.parseCoordinate(it.casted())
                }
                CoordinateEditorDialog().init(arg.name, point) { x, y ->
                    applet.value = IntValueUtil.composeCoordinate(x, y)
                    vm.onItemChanged.value = arg
                }.show(childFragmentManager)
            }
            VariantType.BITS_SWIPE ->
                BitsValueEditorDialog().init(arg.name, applet.value as? Long, Swipe.COMPOSER) {
                    applet.value = it
                    vm.onItemChanged.value = arg
                }.setEnums(0, R.array.swipe_directions)
                    .setHints(R.array.swipe_arg_hints)
                    .show(childFragmentManager)

            VariantType.TEXT_APP_LIST, VariantType.TEXT_PACKAGE_NAME -> {
                val singleSelection = arg.variantValueType == VariantType.TEXT_PACKAGE_NAME
                val value: Collection<String>? = if (singleSelection) applet.value?.let {
                    Collections.singleton(it as String)
                } else applet.value?.casted()
                ComponentSelectorDialog().setSelectedPackages(value ?: emptyList())
                    .doOnCompleted {
                        applet.value = if (singleSelection) it.single() else it
                        vm.onItemChanged.value = arg
                    }
                    .setSingleSelection(singleSelection)
                    .setTitle(option.loadDummyTitle(applet))
                    .show(childFragmentManager)
            }
            VariantType.TEXT_ACTIVITY, VariantType.TEXT_ACTIVITY_LIST -> {
                val singleSelection = arg.variantValueType == VariantType.TEXT_ACTIVITY
                val value: Collection<String>? = if (singleSelection) applet.value?.let {
                    Collections.singleton(it as String)
                } else applet.value?.casted()
                ComponentSelectorDialog().setTitle(option.loadDummyTitle(applet))
                    .setSelectedActivities(value ?: emptyList())
                    .doOnCompleted {
                        applet.value = if (singleSelection) it.single() else it
                        vm.onItemChanged.value = arg
                    }
                    .setSingleSelection(true)
                    .setMode(ComponentSelectorDialog.MODE_ACTIVITY)
                    .show(childFragmentManager)
            }
            else -> TextEditorDialog().configEditText { et ->
                et.configInputType(arg.valueType, true)
                et.maxLines = 10
            }.setCaption(option.helpText).init(arg.name, applet.value?.toString()) set@{
                val parsed = arg.parseValueFromInput(it) ?: return@set R.string.error_mal_format.str
                gvm.referenceEditor.setValue(applet, which, parsed)
                vm.onItemChanged.value = arg
                return@set null
            }.show(childFragmentManager)
        }
    }

    private fun showReferenceSelectorDialog(whichArg: Int, arg: ArgumentDescriptor, id: String?) {
        FlowEditorDialog().init(pvm.task, gvm.root, true, gvm)
            .setArgumentToSelect(applet, arg, id)
            .doOnArgumentSelected { referent ->
                gvm.referenceEditor.setReference(applet, arg, whichArg, referent)
                vm.onItemChanged.value = arg
            }.show(childFragmentManager)
    }

    private val adapter by lazy {
        inlineAdapter(option.arguments, ItemArgumentEditorBinding::class.java, {
            binding.btnRefer.setAntiMoneyClickListener {
                val pos = adapterPosition
                showReferenceSelectorDialog(pos, option.arguments[pos], applet.references[pos])
            }
            binding.btnSpecify.setAntiMoneyClickListener {
                showValueInputDialog(adapterPosition, option.arguments[adapterPosition])
            }
            binding.tvValue.setAntiMoneyClickListener {
                val position = adapterPosition
                val arg = option.arguments[position]
                val referent = applet.references.getValue(position)
                TextEditorDialog().setCaption(R.string.prompt_set_referent.text).configEditText {
                    it.setMaxLength(Applet.MAX_REFERENCE_ID_LENGTH)
                }.init(R.string.edit_referent.text, referent) {
                    if (it == referent) return@init null
                    if (!gvm.isReferentLegalForSelections(it)) {
                        return@init R.string.error_tag_exists.text
                    }
                    // This applet may not be attached to the root
                    gvm.referenceEditor.renameReference(applet, position, it)
                    gvm.renameReferentInRoot(Collections.singleton(referent), it)
                    vm.onItemChanged.value = arg
                    return@init null
                }.show(childFragmentManager)
            }
        }) { binding, pos, arg ->
            binding.tvTitle.text = arg.name
            binding.tvValue.isEnabled = true
            binding.tvValue.isClickable = false
            binding.tvValue.background = null
            if (!arg.isValueOnly && applet.references.containsKey(pos)) {
                val referent = applet.references.getValue(pos)
                // Not value only and the reference is available
                binding.tvValue.text = referent.underlined().foreColored()
                binding.tvValue.setDrawableStart(R.drawable.ic_baseline_link_24)
                binding.tvValue.isClickable = true
                binding.tvValue.background =
                    android.R.attr.selectableItemBackground.resolvedId.getDrawable()
            } else if (!arg.isReferenceOnly && applet.value != null) {
                // Not reference only and the value is available
                binding.tvValue.text = option.describe(applet)
                binding.tvValue.setDrawableStart(R.drawable.ic_baseline_text_fields_24)
            } else {
                binding.tvValue.text = R.string.unspecified.text.italic()
                binding.tvValue.setDrawableStart(View.NO_ID)
                binding.tvValue.isEnabled = false
            }
            binding.btnRefer.isVisible = !arg.isValueOnly
            binding.btnSpecify.isVisible = !arg.isReferenceOnly
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        gvm.referenceEditor.revokeAll()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvArgument.adapter = adapter
        binding.tvTitle.text = option.loadDummyTitle(applet)
        binding.btnCancel.setOnClickListener {
            gvm.referenceEditor.revokeAll()
            dismiss()
        }
        binding.btnComplete.setAntiMoneyClickListener {
            val illegal = vm.checkForUnspecifiedArgument()
            if (illegal == -1) {
                vm.doOnCompletion()
                pvm.clearStaticErrorIfNeeded(vm.applet, StaticError.PROMPT_RESET_REFERENCE)
                gvm.referenceEditor.getReferenceChangedApplets().forEach {
                    gvm.onAppletChanged.value = it
                }
                gvm.referenceEditor.reset()
                dismiss()
            } else {
                binding.rvArgument.findViewHolderForAdapterPosition(illegal)?.itemView?.shake()
                toast(R.string.format_not_specified.format(option.arguments[illegal].name))
            }
        }
        observeTransient(vm.onItemChanged) {
            adapter.notifyItemChanged(option.arguments.indexOf(it), true)
        }
    }

    fun setAppletOption(applet: Applet, option: AppletOption) = doWhenCreated {
        vm.option = option
        vm.applet = applet
    }

    fun doOnCompletion(block: () -> Unit) = doWhenCreated {
        vm.doOnCompletion = block
    }
}