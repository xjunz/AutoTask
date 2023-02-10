/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.editor

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import top.xjunz.tasker.ui.task.selector.option.BitsValueEditorDialog
import top.xjunz.tasker.ui.task.selector.option.ComponentSelectorDialog
import top.xjunz.tasker.ui.task.selector.option.CoordinateEditorDialog
import top.xjunz.tasker.ui.task.selector.option.TimeIntervalEditorDialog
import top.xjunz.tasker.util.ClickUtil.setAntiMoneyClickListener
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

    private lateinit var pvm: FlowEditorViewModel

    private val gvm get() = pvm.global

    private fun showValueInputDialog(
        standalone: Boolean,
        which: Int,
        arg: ArgumentDescriptor
    ) {
        val fragmentManager = if (standalone) {
            parentFragmentManager
        } else {
            childFragmentManager
        }

        fun updateValue(newValue: Any?) {
            gvm.referenceEditor.setValue(applet, which, newValue)
            if (standalone) {
                vm.doOnCompletion.invoke()
            } else {
                vm.onItemChanged.value = arg
            }
        }
        when (arg.variantValueType) {
            VariantType.INT_COORDINATE -> {
                val point = applet.value?.let {
                    IntValueUtil.parseCoordinate(it.casted())
                }
                CoordinateEditorDialog().init(arg.name, point) { x, y ->
                    updateValue(IntValueUtil.composeCoordinate(x, y))
                }.show(fragmentManager)
            }
            VariantType.INT_INTERVAL -> {
                TimeIntervalEditorDialog().init(
                    arg.name, (applet.value ?: applet.defaultValue) as Int
                ) {
                    updateValue(it)
                }.show(fragmentManager)
            }
            VariantType.BITS_SWIPE ->
                BitsValueEditorDialog().init(arg.name, applet.value as? Long, Swipe.COMPOSER) {
                    updateValue(it)
                }.setEnums(0, R.array.swipe_directions)
                    .setHints(R.array.swipe_arg_hints)
                    .show(fragmentManager)

            VariantType.TEXT_PACKAGE_NAME -> {
                val singleSelection = !arg.isCollection
                val value: Collection<String>? = if (singleSelection) applet.value?.let {
                    Collections.singleton(it as String)
                } else applet.value?.casted()
                ComponentSelectorDialog().setSelectedPackages(value ?: emptyList())
                    .doOnCompleted {
                        updateValue(if (singleSelection) it.single() else it)
                    }
                    .setSingleSelection(singleSelection)
                    .setTitle(option.loadDummyTitle(applet))
                    .show(fragmentManager)
            }
            VariantType.TEXT_ACTIVITY -> {
                val singleSelection = !arg.isCollection
                val value: Collection<String>? = if (singleSelection) applet.value?.let {
                    Collections.singleton(it as String)
                } else applet.value?.casted()
                ComponentSelectorDialog().setTitle(option.loadDummyTitle(applet))
                    .setSelectedActivities(value ?: emptyList())
                    .doOnCompleted {
                        updateValue(if (singleSelection) it.single() else it)
                    }
                    .setSingleSelection(singleSelection)
                    .setMode(ComponentSelectorDialog.MODE_ACTIVITY)
                    .show(fragmentManager)
            }
            else -> if (arg.isCollection) {
                VarargTextEditorDialog().init(arg.name, applet, arg) { value, referents ->
                    updateValue(value)
                    gvm.referenceEditor.setVarargReferences(applet, referents)
                }.show(fragmentManager)
            } else {
                TextEditorDialog().configEditText { et ->
                    et.configInputType(arg.valueClass, true)
                    et.maxLines = 10
                }.setVariantType(arg.variantValueType).setCaption(option.helpText)
                    .init(arg.name, applet.value?.toString()) init@{
                        val parsed =
                            arg.parseValueFromInput(it)
                                ?: return@init R.string.error_mal_format.str
                        updateValue(parsed)
                        return@init null
                    }.show(fragmentManager)
            }
        }
    }

    private fun showReferenceSelectorDialog(
        standalone: Boolean,
        whichArg: Int,
        arg: ArgumentDescriptor,
        id: String?
    ) {
        val fragmentManager = if (standalone) {
            parentFragmentManager
        } else {
            childFragmentManager
        }
        FlowEditorDialog().init(pvm.task, gvm.root, true, gvm)
            .setArgumentToSelect(applet, arg, id)
            .doOnArgumentSelected { referent ->
                gvm.referenceEditor.setReference(applet, arg, whichArg, referent)
                if (standalone) {
                    vm.doOnCompletion.invoke()
                } else {
                    vm.onItemChanged.value = arg
                }
            }.show(fragmentManager)
    }

    private val adapter by lazy {
        inlineAdapter(option.arguments, ItemArgumentEditorBinding::class.java, {
            binding.btnRefer.setAntiMoneyClickListener {
                val pos = adapterPosition
                showReferenceSelectorDialog(
                    false, pos,
                    option.arguments[pos],
                    applet.references[pos]
                )
            }
            binding.btnSpecify.setAntiMoneyClickListener {
                showValueInputDialog(false, adapterPosition, option.arguments[adapterPosition])
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
            if (!arg.isValueOnly && applet.references.containsKey(pos)) {
                val referent = applet.references.getValue(pos)
                // Not value only and the reference is available
                binding.tvValue.text = referent.underlined().foreColored().italic()
                binding.tvValue.setDrawableStart(R.drawable.ic_baseline_link_24)
                binding.tvValue.isClickable = true
            } else if (!arg.isReferenceOnly && applet.value != null) {
                // Not reference only and the value is available
                binding.tvValue.text = option.describe(applet)
                binding.tvValue.setDrawableStart(R.drawable.ic_text_fields_24px)
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        pvm = peekParentViewModel<FlowEditorDialog, FlowEditorViewModel>()
        if (vm.option.arguments.size == 1) {
            val singleArg = vm.option.arguments.single()
            if (singleArg.isReferenceOnly) {
                showReferenceSelectorDialog(true, 0, option.arguments[0], applet.references[0])
                dismiss()
                return null
            } else if (singleArg.isValueOnly) {
                showValueInputDialog(true, 0, option.arguments[0])
                dismiss()
                return null
            }
        }
        return super.onCreateView(inflater, container, savedInstanceState)
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
                toast(R.string.error_unspecified.format(option.arguments[illegal].name))
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