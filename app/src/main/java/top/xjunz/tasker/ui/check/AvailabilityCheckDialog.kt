package top.xjunz.tasker.ui.check

import android.content.ClipData
import android.content.ClipDescription
import android.graphics.Canvas
import android.graphics.Point
import android.os.Bundle
import android.view.DragEvent
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.google.android.material.transition.platform.MaterialFadeThrough
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.xjunz.tasker.R
import top.xjunz.tasker.databinding.DialogAvailabilityCheckBinding
import top.xjunz.tasker.databinding.ItemCheckCaseBinding
import top.xjunz.tasker.ktx.*
import top.xjunz.tasker.service.IAvailabilityCheckerCallback
import top.xjunz.tasker.service.currentService
import top.xjunz.tasker.ui.MainViewModel
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.ui.base.inlineAdapter
import java.util.*
import kotlin.random.Random

/**
 * @author xjunz 2022/07/25
 */
class AvailabilityCheckDialog : BaseDialogFragment<DialogAvailabilityCheckBinding>() {

    private val viewModel by viewModels<InnerViewModel>()

    private val mainViewModel by activityViewModels<MainViewModel>()

    override val isFullScreen = true

    private class InnerViewModel : ViewModel() {

        val currentIndex = MutableLiveData(0)

        val currentCheckCase = currentIndex.map { CheckCase.ALL[it] }

        val checkResult = MutableLiveData<String?>()

        val randomText = MutableLiveData<String>()

        val btnPosition = MutableLiveData<Pair<Float, Float>>()

        val showList = MutableLiveData(false)

        private val checker by lazy {
            currentService.createAvailabilityChecker()
        }

        fun checkCurrentCase() {
            viewModelScope.launch {
                try {
                    withContext(Dispatchers.Default) {
                        checker.launchCheck(
                            currentCheckCase.require().nameRes,
                            object : IAvailabilityCheckerCallback.Stub() {
                                override fun onCompleted(result: String?) {
                                    checkResult.postValue(result)
                                }
                            })
                    }
                } catch (t: Throwable) {
                    toast(t.message)
                }
            }
        }

        fun nextCase() {
            if (currentIndex.require() < CheckCase.ALL.lastIndex) {
                currentIndex.inc()
            } else {
                currentIndex.value = 0
            }
        }

        fun generatePosition(random: Boolean) {
            if (random) {
                btnPosition.value = Random.nextFloat() to Random.nextFloat()
            } else {
                btnPosition.value = .5F to .5F
            }
        }

        fun generateRandomText() {
            randomText.value = UUID.randomUUID().toString().substring(0, 8).uppercase()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        CheckCase.ALL.forEach { it.isChecked = null }
        initViews()
        observeLiveData()
        viewModel.generateRandomText()
    }

    private fun initViews() {
        configDragAndDrop()
        binding.btnCheck.setOnClickListener {
            viewModel.checkCurrentCase()
        }
        binding.btnTarget.setOnClickListener {
            toast("Don't touch me~!")
        }
        binding.rvCheckCase.adapter = inlineAdapter(
            CheckCase.ALL.toList(), ItemCheckCaseBinding::class.java, {
                itemView.setOnClickListener {
                    viewModel.currentIndex.value = adapterPosition
                    viewModel.showList.toggle()
                }
            }) { binding, index, data ->
            binding.tvName.text = data.label
            binding.ivState.setImageResource(data.stateImageRes)
            binding.root.isSelected = index == viewModel.currentIndex.require()
        }
        binding.ibAll.setOnClickListener {
            viewModel.showList.toggle()
        }
        binding.mask.setOnClickListener {
            viewModel.showList.value = false
        }
    }

    private fun observeLiveData() {
        observe(viewModel.currentIndex) {
            binding.tvTitle.text =
                R.string.format_check_case_ordinal.format(it + 1, CheckCase.ALL.size)
            binding.rvCheckCase.adapter?.notifyItemRangeChanged(0, CheckCase.ALL.size, true)
        }
        observe(viewModel.currentCheckCase) {
            binding.root.beginAutoTransition()
            binding.btnTarget.isVisible = it.isButtonVisible
            binding.viewDropTarget.isVisible = it.isDropTargetVisible
            binding.tvName.text = it.label
            binding.tvDesc.text = R.string.format_check_case_desc_prefix.format(it.descRes.str)
            viewModel.generatePosition(it.isButtonRandomPosition)
            viewModel.generateRandomText()
        }
        observeTransient(viewModel.checkResult) {
            toast(it)
        }
        observe(viewModel.randomText) {
            binding.btnTarget.text = it
        }
        observe(viewModel.btnPosition) {
            binding.btnTarget.updateLayoutParams<ConstraintLayout.LayoutParams> {
                horizontalBias = it.first
                verticalBias = it.second
            }
        }
        observe(viewModel.showList) {
            binding.root.beginAutoTransition(MaterialFadeThrough())
            binding.cvList.isVisible = it
            binding.mask.isVisible = it
        }
        observe(mainViewModel.isRunning) {
            if (!it) {
                dismiss()
                toast(R.string.service_not_started)
            }
        }
    }

    private fun configDragAndDrop() {
        binding.btnTarget.setOnLongClickListener {
            val item = ClipData.Item(binding.btnTarget.text)
            val dragData = ClipData("label", arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN), item)
            val myShadow = SimpleDragShadowBuilder(it)
            it.startDragAndDrop(dragData, myShadow, null, 0)
            return@setOnLongClickListener true
        }
        binding.viewDropTarget.setOnDragListener { v, event ->
            v as TextView
            when (event.action) {
                DragEvent.ACTION_DRAG_ENTERED -> v.isActivated = true
                DragEvent.ACTION_DRAG_EXITED -> v.isActivated = false
                DragEvent.ACTION_DROP -> {
                    v.isActivated = false
                    v.text = event.clipData.getItemAt(0).text
                }
            }
            return@setOnDragListener true
        }
        binding.btnChecked.setOnClickListener {
            viewModel.currentCheckCase.require().isChecked = true
            viewModel.nextCase()
        }
        binding.btnFailed.setOnClickListener {
            viewModel.currentCheckCase.require().isChecked = false
            viewModel.nextCase()
        }
    }

    private class SimpleDragShadowBuilder(target: View) : View.DragShadowBuilder(target) {

        // Defines a callback that sends the drag shadow dimensions and touch point
        // back to the system.
        override fun onProvideShadowMetrics(size: Point, touch: Point) {
            // Set the width of the shadow to half the width of the original View.
            val width: Int = view.width

            // Set the height of the shadow to half the height of the original View.
            val height: Int = view.height

            // Set the size parameter's width and height values. These get back to
            // the system through the size parameter.
            size.set(width, height)

            // Set the touch point's position to be in the middle of the drag shadow.
            touch.set(width / 2, height / 2)
        }

        // Defines a callback that draws the drag shadow in a Canvas that the system
        // constructs from the dimensions passed to onProvideShadowMetrics().
        override fun onDrawShadow(canvas: Canvas) {
            view.draw(canvas)
        }
    }
}