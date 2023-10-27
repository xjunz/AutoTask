/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.selector.argument

import android.content.DialogInterface
import android.graphics.Point
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
import top.xjunz.tasker.ktx.configInputType
import top.xjunz.tasker.ktx.doWhenCreated
import top.xjunz.tasker.ktx.foreColored
import top.xjunz.tasker.ktx.format
import top.xjunz.tasker.ktx.italic
import top.xjunz.tasker.ktx.observeTransient
import top.xjunz.tasker.ktx.peekParentViewModel
import top.xjunz.tasker.ktx.setDrawableStart
import top.xjunz.tasker.ktx.setMaxLength
import top.xjunz.tasker.ktx.shake
import top.xjunz.tasker.ktx.show
import top.xjunz.tasker.ktx.str
import top.xjunz.tasker.ktx.text
import top.xjunz.tasker.ktx.toast
import top.xjunz.tasker.ktx.underlined
import top.xjunz.tasker.task.applet.criterion.BoundsCriterion
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.task.applet.option.descriptor.ArgumentDescriptor
import top.xjunz.tasker.task.applet.option.descriptor.ValueDescriptor
import top.xjunz.tasker.task.applet.util.IntValueUtil
import top.xjunz.tasker.task.applet.value.LongDuration
import top.xjunz.tasker.task.applet.value.ScrollMetrics
import top.xjunz.tasker.task.applet.value.SwipeMetrics
import top.xjunz.tasker.task.applet.value.VariantArgType
import top.xjunz.tasker.task.gesture.SerializableInputEvent
import top.xjunz.tasker.task.inspector.FloatingInspector
import top.xjunz.tasker.task.inspector.InspectorMode
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.ui.base.inlineAdapter
import top.xjunz.tasker.ui.common.TextEditorDialog
import top.xjunz.tasker.ui.main.EventCenter
import top.xjunz.tasker.ui.main.EventCenter.doOnEventRoutedWithValue
import top.xjunz.tasker.ui.task.editor.FlowEditorDialog
import top.xjunz.tasker.ui.task.editor.FlowEditorViewModel
import top.xjunz.tasker.ui.task.editor.VarargTextEditorDialog
import top.xjunz.tasker.ui.task.editor.VibrationPatternEditorDialog
import top.xjunz.tasker.ui.task.inspector.FloatingInspectorDialog
import top.xjunz.tasker.ui.task.showcase.TaskCreatorDialog
import top.xjunz.tasker.util.ClickListenerUtil.setNoDoubleClickListener
import java.util.Collections

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

        var updateGesture: ((List<SerializableInputEvent>) -> Unit)? = null

        fun checkForUnspecifiedArgument(): Int {
            option.arguments.forEachIndexed { which, arg ->
                val isValueSet = applet.values[which] != null
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


    @Suppress("UNCHECKED_CAST")
    private fun showValueInputDialog(standalone: Boolean, which: Int, arg: ArgumentDescriptor) {
        val fragmentManager = if (standalone) parentFragmentManager else childFragmentManager

        fun updateValue(newValue: Any?) {
            gvm.referenceEditor.setValue(applet, which, newValue)
            if (standalone) {
                vm.doOnCompletion.invoke()
            } else {
                vm.onItemChanged.value = arg
            }
        }

        val value = applet.values[which]
        val title = option.loadUnspannedTitle(applet)

        fun showSimpleIntRangeEditor(
            limits: IntRange,
            defStart: Int? = null,
            defStop: Int? = null
        ) {
            if (arg.isCollection) {
                value as Collection<Int>?
                RangeEditorDialog().setRange(
                    value?.firstOrNull(), value?.lastOrNull(), defStart, defStop
                ).setType(Applet.ARG_TYPE_INT).setTitle(title).doOnCompletion { start, end ->
                    updateValue(listOf(start, end))
                }.setLimits(limits).show(fragmentManager)
            } else {
                TODO()
            }
        }

        when (arg.variantValueType) {
            VariantArgType.INT_COORDINATE -> {
                val point = value?.let {
                    IntValueUtil.parseXY(it.casted())
                }
                XYEditorDialog().init(arg.name, point) { x, y ->
                    updateValue(IntValueUtil.composeXY(x, y))
                }.show(fragmentManager)
            }

            VariantArgType.INT_INTERVAL -> {
                TimeIntervalEditorDialog().init(
                    arg.name, (value ?: applet.defaultValue) as Int
                ) {
                    updateValue(it)
                }.show(fragmentManager)
            }

            VariantArgType.INT_INTERVAL_XY -> {
                val point = value?.let {
                    IntValueUtil.parseXY(it.casted())
                }
                XYEditorDialog().init(arg.name, point ?: Point(500, 500)) { x, y ->
                    updateValue(IntValueUtil.composeXY(x, y))
                }.setVariantType(
                    VariantArgType.INT_INTERVAL_XY,
                    R.string.idle_threshold,
                    R.string.max_wait_for_idle_duration
                ).setHelp(option.helpText).show(fragmentManager)
            }

            VariantArgType.INT_ROTATION ->
                EnumSelectorDialog().setSingleSelectionMode()
                    .setSpanCount(2)
                    .setInitialSelections(value?.let { Collections.singleton(it as Int) })
                    .init(arg.name, R.array.rotations) {
                        updateValue(it.single())
                    }.show(fragmentManager)

            VariantArgType.INT_TIME_OF_DAY -> {
                if (arg.isCollection) {
                    value as Collection<Int>?
                    TimeRangeEditorDialog().setRange(
                        value?.firstOrNull(), value?.lastOrNull(),
                        0, IntValueUtil.composeTime(23, 59, 59)
                    ).setTitle(title).doOnCompletion { start, end ->
                        updateValue(listOf(start, end))
                    }.show(fragmentManager)
                } else {
                    value as Int?
                    TimeRangeEditorDialog().setRange(
                        value, null,
                        defStart = IntValueUtil.composeTime(8, 0, 0)
                    ).setTitle(title).doOnCompletion { start, _ ->
                        updateValue(start)
                    }.asUnary().setUnarySubtitle(arg.name).show(fragmentManager)
                }
            }

            VariantArgType.INT_DAY_OF_MONTH -> {
                val days = Array(31) { i ->
                    (i + 1).toString()
                }
                EnumSelectorDialog().init(title, days) {
                    updateValue(it)
                }.setSpanCount(4).setInitialSelections(value?.casted()).show(fragmentManager)
            }

            VariantArgType.INT_MONTH ->
                EnumSelectorDialog().init(title, R.array.months) {
                    updateValue(it)
                }.setInitialSelections(value?.casted()).show(fragmentManager)

            VariantArgType.INT_DAY_OF_WEEK ->
                EnumSelectorDialog().init(title, R.array.days_of_week) {
                    updateValue(it)
                }.setInitialSelections(value?.casted()).show(fragmentManager)

            VariantArgType.INT_HOUR_OF_DAY -> showSimpleIntRangeEditor(0..23)

            VariantArgType.INT_MIN_OR_SEC -> showSimpleIntRangeEditor(0..59, 30, 30)

            VariantArgType.INT_PERCENT -> showSimpleIntRangeEditor(0..100)

            VariantArgType.INT_QUANTITY -> showSimpleIntRangeEditor(0..999)

            VariantArgType.LONG_TIME -> {
                if (arg.isCollection) {
                    value as Collection<Long>?
                    DateTimeRangeEditorDialog().setRange(
                        value?.firstOrNull(), value?.lastOrNull(),
                        System.currentTimeMillis(), System.currentTimeMillis()
                    ).setType(Applet.ARG_TYPE_LONG).setTitle(title)
                        .doOnCompletion { start, end ->
                            updateValue(listOf(start, end))
                        }.show(fragmentManager)
                } else {
                    value as Long?
                    DateTimeRangeEditorDialog().setRange(
                        value, null, defStart = System.currentTimeMillis()
                    ).setType(Applet.ARG_TYPE_LONG).setTitle(title)
                        .doOnCompletion { start, _ ->
                            updateValue(start)
                        }.asUnary().setUnarySubtitle(arg.name).show(fragmentManager)
                }
            }

            VariantArgType.BITS_SWIPE ->
                BitsValueEditorDialog().init(arg.name, value as? Long, SwipeMetrics.COMPOSER) {
                    updateValue(it)
                }.setEnums(0, R.array.swipe_directions)
                    .setHints(R.array.swipe_metrics_hints)
                    .show(fragmentManager)

            VariantArgType.BITS_SCROLL ->
                BitsValueEditorDialog().init(
                    arg.name,
                    (value as? Long) ?: ScrollMetrics.COMPOSER.compose(3, 50),
                    ScrollMetrics.COMPOSER
                ) {
                    updateValue(it)
                }.setEnums(0, R.array.swipe_directions)
                    .setHints(R.array.scroll_metrics_hints)
                    .show(fragmentManager)

            VariantArgType.BITS_LONG_DURATION ->
                BitsValueEditorDialog().init(
                    arg.name, (value as? Long) ?: LongDuration.COMPOSER.compose(1, 0, 0, 0),
                    LongDuration.COMPOSER
                ) {
                    updateValue(it)
                }.setHints(R.array.long_duration_units)
                    .show(fragmentManager)

            VariantArgType.BITS_BOUNDS ->
                DistanceEditorDialog().setArguments(title) {
                    updateValue(it)
                }.setDistance(value?.casted())
                    .setDirection((applet as BoundsCriterion<*>).direction)
                    .show(fragmentManager)


            VariantArgType.TEXT_PACKAGE_NAME -> {
                val singleSelection = !arg.isCollection
                val pkgs: Collection<String>? = if (singleSelection) value?.let {
                    Collections.singleton(it as String)
                } else value?.casted()
                ComponentSelectorDialog().setSelectedPackages(pkgs ?: emptyList())
                    .doOnCompleted {
                        updateValue(if (singleSelection) it.single() else it)
                    }
                    .setSingleSelection(singleSelection)
                    .setTitle(title)
                    .show(fragmentManager)
            }

            VariantArgType.TEXT_ACTIVITY -> {
                val singleSelection = !arg.isCollection
                val comps: Collection<String>? = if (singleSelection) value?.let {
                    Collections.singleton(it as String)
                } else value?.casted()
                ComponentSelectorDialog().setTitle(title)
                    .setSelectedActivities(comps ?: emptyList())
                    .doOnCompleted {
                        updateValue(if (singleSelection) it.single() else it)
                    }
                    .setSingleSelection(singleSelection)
                    .setMode(ComponentSelectorDialog.MODE_ACTIVITY)
                    .show(fragmentManager)
            }

            VariantArgType.TEXT_GESTURES -> {
                val events: List<SerializableInputEvent> = value?.casted() ?: emptyList()
                FloatingInspectorDialog().setMode(InspectorMode.GESTURE_RECORDER).doOnSucceeded {
                    if (events.isNotEmpty()) {
                        EventCenter.sendEvent(FloatingInspector.EVENT_REQUEST_EDIT_GESTURES, events)
                    }
                    vm.updateGesture = {
                        updateValue(it)
                    }
                }.show(childFragmentManager)
            }

            VariantArgType.TEXT_FORMAT ->
                VarargTextEditorDialog().init(arg.name, applet, arg) { format, referents ->
                    updateValue(format)
                    gvm.referenceEditor.setVarargReferences(applet, referents)
                }.show(fragmentManager)

            VariantArgType.TEXT_VIBRATION_PATTERN -> {
                VibrationPatternEditorDialog().init(value?.casted(), arg.name) {
                    updateValue(it)
                }.show(fragmentManager)
            }

            else ->
                TextEditorDialog().configEditText { et ->
                    et.configInputType(arg.valueType, true)
                    et.maxLines = 10
                }.setVariantType(arg.variantValueType)
                    .init(title, value?.toString()) init@{
                        updateValue(
                            arg.parseValueFromInput(it) ?: return@init R.string.error_mal_format.str
                        )
                        return@init null
                    }.setHint(if (arg.isAnonymous) null else arg.name)
                    .show(fragmentManager)
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
                gvm.referenceEditor.setReference(applet, whichArg, referent)
                if (standalone) {
                    vm.doOnCompletion.invoke()
                } else {
                    vm.onItemChanged.value = arg
                }
            }.show(fragmentManager)
    }

    private val adapter by lazy {
        inlineAdapter(option.arguments, ItemArgumentEditorBinding::class.java, {
            binding.btnRefer.setNoDoubleClickListener {
                val pos = adapterPosition
                showReferenceSelectorDialog(
                    false,
                    pos,
                    option.arguments[pos],
                    applet.references[pos]
                )
            }
            binding.btnSpecify.setNoDoubleClickListener {
                showValueInputDialog(false, adapterPosition, option.arguments[adapterPosition])
            }
            binding.etValue.setNoDoubleClickListener {
                val position = adapterPosition
                if (applet.values.containsKey(position)) {
                    binding.btnSpecify.performClick()
                    return@setNoDoubleClickListener
                }
                val arg = option.arguments[position]
                val ref = applet.references.getValue(position)
                TextEditorDialog().setCaption(R.string.prompt_set_referent.text).configEditText {
                    it.setMaxLength(Applet.MAX_REFERENCE_ID_LENGTH)
                }.init(R.string.edit_referent_name.text, ref) {
                    if (it == ref) return@init null
                    if (!gvm.isReferentLegalForSelections(it)) {
                        return@init R.string.error_tag_exists.text
                    }
                    // This applet may not be attached to the root
                    gvm.referenceEditor.renameReference(applet, position, it)
                    gvm.renameReferentInRoot(Collections.singleton(ref), it)
                    vm.onItemChanged.value = arg
                    return@init null
                }.show(childFragmentManager)
            }
        }) { binding, pos, arg ->
            binding.tilValue.hint = arg.name
            binding.etValue.isEnabled = true
            val value = applet.values[pos]
            if (!arg.isValueOnly && applet.references.containsKey(pos)) {
                val referent = applet.references.getValue(pos)
                // Not value only and the reference is available
                binding.etValue.setText(referent.underlined().foreColored().italic())
                binding.etValue.setDrawableStart(R.drawable.ic_baseline_link_24)
            } else if (!arg.isReferenceOnly && value != null) {
                // Not reference only and the value is available
                // TODO: More elegant way to show description of multi-arg applets
                binding.etValue.setText(
                    if (applet.argumentTypes.size == 1) option.describe(applet) else value.toString()
                )
                binding.etValue.setDrawableStart(R.drawable.ic_text_fields_24px)
            } else {
                binding.etValue.setText(R.string.unspecified.text.italic())
                binding.etValue.setDrawableStart(View.NO_ID)
                binding.etValue.isEnabled = false
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
        binding.tvTitle.text = option.loadUnspannedTitle(applet)
        binding.tvHelp.isVisible = option.helpText != null
        binding.tvHelp.text = option.helpText
        binding.btnCancel.setOnClickListener {
            gvm.referenceEditor.revokeAll()
            dismiss()
        }
        binding.btnComplete.setNoDoubleClickListener {
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
        doOnEventRoutedWithValue<List<SerializableInputEvent>>(FloatingInspector.EVENT_GESTURES_RECORDED) {
            if (vm.updateGesture == null) {
                val index = vm.option.arguments.indexOfFirst { descriptor ->
                    descriptor.variantValueType == VariantArgType.TEXT_GESTURES
                }
                if (index >= 0) {
                    gvm.referenceEditor.setValue(applet, index, it)
                    vm.onItemChanged.value = vm.option.arguments[index]
                    toast(R.string.gestures_updated)
                }
            } else {
                vm.updateGesture?.invoke(it)
                toast(R.string.gestures_updated)
            }
        }
        when (TaskCreatorDialog.REQUESTED_QUICK_TASK_CREATOR) {
            TaskCreatorDialog.QUICK_TASK_CREATOR_GESTURE_RECORDER -> {
                showValueInputDialog(false, 0, option.arguments[0])
                TaskCreatorDialog.REQUESTED_QUICK_TASK_CREATOR = -1
            }

            TaskCreatorDialog.QUICK_TASK_CREATOR_CLICK_AUTOMATION -> {
                showValueInputDialog(false, 0, option.arguments[0])
                TaskCreatorDialog.REQUESTED_QUICK_TASK_CREATOR = -1
            }
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