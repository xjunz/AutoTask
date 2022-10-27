package top.xjunz.tasker.ui.check

import top.xjunz.tasker.R
import top.xjunz.tasker.model.Labeled

/**
 * @author xjunz 2022/07/27
 */
sealed class CheckCase(
    val nameRes: Int,
    val descRes: Int,
    val isButtonVisible: Boolean = true,
    val isButtonRandomPosition: Boolean = false,
    val isDropTargetVisible: Boolean = false,
) : Labeled(nameRes) {

    /**
     * Whether checked or not. `null` means unknown.
     */
    var isChecked: Boolean? = null

    val stateImageRes
        get() = when (isChecked) {
            null -> R.drawable.ic_baseline_help_outline_24
            true -> R.drawable.ic_twotone_check_circle_24
            false -> R.drawable.ic_twotone_cancel_24
        }

    companion object {
        val ALL = arrayOf(UiObjectRecognition, GlobalAction, DragAndDrop)
    }

    object UiObjectRecognition :
        CheckCase(R.string.case_widget_recognition, R.string.desc_case_widget_recognition)

    object GlobalAction :
        CheckCase(
            R.string.case_global_action, R.string.desc_case_global_action, isButtonVisible = false,
        )

    object DragAndDrop :
        CheckCase(
            R.string.case_drag_and_drop,
            R.string.desc_case_drag_and_drop,
            isButtonRandomPosition = true,
            isDropTargetVisible = true
        )
}