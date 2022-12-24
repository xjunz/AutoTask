package top.xjunz.tasker.ui.task.selector.option

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.RadioButton
import androidx.annotation.GravityInt
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import top.xjunz.shared.utils.illegalArgument
import top.xjunz.tasker.R
import top.xjunz.tasker.bridge.DisplayManagerBridge
import top.xjunz.tasker.databinding.DialogDistanceEditorBinding
import top.xjunz.tasker.engine.value.Distance
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.util.AntiMonkeyUtil.setAntiMoneyClickListener

/**
 * @author xjunz 2022/10/24
 */
class DistanceEditorDialog : BaseDialogFragment<DialogDistanceEditorBinding>() {

    override val isFullScreen: Boolean = false

    private val viewModel by viewModels<InnerViewModel>()

    private class InnerViewModel : ViewModel() {

        var distance = Distance()

        @GravityInt
        var direction: Int = -1

        var title: CharSequence? = null

        var unit: Int = Distance.UNIT_PX

        lateinit var doOnCompletion: (Distance) -> Unit
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvTitle.text = viewModel.title
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        binding.btnNoMinLimit.setOnClickListener {
            binding.etMinimum.text.clear()
        }
        binding.btnNoMaxLimit.setOnClickListener {
            binding.etMaximum.text.clear()
        }
        binding.menuUnit.setEntries(R.array.distance_units, true) {
            if (it == viewModel.unit) return@setEntries
            val max = binding.etMaximum.textString.toFloatOrNull()
            val min = binding.etMinimum.textString.toFloatOrNull()
            if (viewModel.unit == Distance.UNIT_PX && it == Distance.UNIT_DP) {
                if (max != null) binding.etMaximum.setText((max / DisplayManagerBridge.density).asString())
                if (min != null) binding.etMinimum.setText((min / DisplayManagerBridge.density).asString())
            } else if (viewModel.unit == Distance.UNIT_DP && it == Distance.UNIT_PX) {
                if (max != null) binding.etMaximum.setText(max.dp.toString())
                if (min != null) binding.etMinimum.setText(min.dp.toString())
            } else {
                binding.etMaximum.text = null
                binding.etMinimum.text = null
            }
            viewModel.unit = it
        }
        binding.rgScope.setOnCheckedChangeListener { _, checkedId ->
            binding.tvTitle.text = R.string.format_scoped_distance_name.format(
                binding.rgScope.findViewById<RadioButton>(checkedId).text,
                directionToString(viewModel.direction)
            )
        }
        binding.tvSubtitleUnit.setHelp(R.string.help_distance_unit.text)
        binding.btnComplete.setAntiMoneyClickListener {
            val min = binding.etMinimum.textString.toFloatOrNull()
            val max = binding.etMaximum.textString.toFloatOrNull()
            if (min == null && max == null) {
                binding.root.rootView.shake()
                toast(
                    R.string.format_error_no_limit.format(
                        R.string.minimum.str, R.string.maximum.str
                    )
                )
                return@setAntiMoneyClickListener
            }
            if (max != null && min != null && max < min) {
                binding.root.rootView.shake()
                toast(
                    R.string.format_error_min_greater_than_max.format(
                        R.string.minimum.str,
                        R.string.maximum.str
                    )
                )
                return@setAntiMoneyClickListener
            }
            if (viewModel.unit >= 2
                && (min != null && min !in 0F..1F || (max != null && max !in 0F..1F))
            ) {
                toastAndShake(R.string.format_error_min_max_not_in_scope.format(binding.menuUnit.text))
                return@setAntiMoneyClickListener
            }
            viewModel.distance.let {
                it.unit = viewModel.unit
                if (binding.rgScope.isVisible) {
                    it.scope = when (binding.rgScope.checkedRadioButtonId) {
                        R.id.rb_scope_screen -> Distance.SCOPE_SCREEN
                        R.id.rb_scope_parent -> Distance.SCOPE_PARENT
                        else -> illegalArgument()
                    }
                }
                it.rangeStart = min
                it.rangeEnd = max
            }
            viewModel.doOnCompletion(viewModel.distance)
            dismiss()
        }
        viewModel.distance.let {
            binding.tvSubtitleScope.isVisible = it.scope != Distance.SCOPE_NONE
            binding.rgScope.isVisible = it.scope != Distance.SCOPE_NONE
            if (savedInstanceState != null) return
            binding.etMaximum.setText(it.rangeEnd?.toString())
            binding.etMinimum.setText(it.rangeStart?.toString())
            when (it.scope) {
                Distance.SCOPE_PARENT -> binding.rgScope.check(R.id.rb_scope_parent)
                Distance.SCOPE_SCREEN -> binding.rgScope.check(R.id.rb_scope_screen)
            }
            binding.menuUnit.setText(R.array.distance_units.array[it.unit])
        }
    }

    private fun Float.asString(): String {
        if (this < 0.01) {
            return "0"
        }
        return toString()
    }

    private val directions = arrayOf(
        Gravity.START,
        Gravity.TOP,
        Gravity.END,
        Gravity.BOTTOM
    )

    private fun directionToString(dir: Int): CharSequence {
        return R.array.directions.array[directions.indexOf(dir)]
    }

    fun setDirection(@GravityInt direction: Int) = doWhenCreated {
        viewModel.direction = direction
        if (directions.indexOf(direction) == -1) {
            viewModel.distance.scope = Distance.SCOPE_NONE
        }
    }

    fun setDistance(distance: Distance?) = doWhenCreated {
        if (distance != null)
            viewModel.distance = distance
    }

    fun setArguments(title: CharSequence?, block: (Distance) -> Unit) = doWhenCreated {
        viewModel.title = title
        viewModel.doOnCompletion = block
    }
}