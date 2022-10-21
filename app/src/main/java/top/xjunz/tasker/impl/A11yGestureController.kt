package top.xjunz.tasker.impl

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.view.MotionEvent
import androidx.test.uiautomator.GestureController
import androidx.test.uiautomator.PointerGesture
import androidx.test.uiautomator.UiDevice
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.service.A11yAutomatorService
import top.xjunz.tasker.util.GestureGenerator.convertToStrokes
import kotlin.math.ceil

/**
 * @author xjunz 2021/9/20
 */
class A11yGestureController(private val service: A11yAutomatorService, device: UiDevice) :
    GestureController(device) {

    private fun convertGesturesToPointerCoordsSeries(
        vararg gestures: PointerGesture
    ): Array<Array<MotionEvent.PointerCoords>> {
        val pointerCoordsSeries = arrayOfNulls<Array<MotionEvent.PointerCoords>>(gestures.size)
        gestures.forEachIndexed { index, gesture ->
            val touchCount =
                ceil(gesture.duration() / MOTION_EVENT_INJECTION_DELAY_MILLIS.toFloat()).toInt() + 1
            val pointerCoordsSerial = Array(touchCount) array@{
                return@array MotionEvent.PointerCoords().apply {
                    size = 1F
                    pressure = 1F
                    if (it == touchCount - 1) {
                        gesture.end()
                    } else {
                        gesture.pointAt(MOTION_EVENT_INJECTION_DELAY_MILLIS * it)
                    }.let {
                        x = it.x.toFloat()
                        y = it.y.toFloat()
                    }
                }
            }
            pointerCoordsSeries[index] = pointerCoordsSerial
        }
        return pointerCoordsSeries.casted()
    }

    override fun performGesture(vararg gestures: PointerGesture) {
        performSinglePointerGesture(gestures[0])
    }

    private fun GestureDescription.StrokeDescription.buildGesture(): GestureDescription {
        return GestureDescription.Builder().addStroke(this).build()
    }

    private fun Iterable<GestureDescription.StrokeDescription>.buildGesture(): GestureDescription {
        return GestureDescription.Builder().apply {
            forEach { addStroke(it) }
        }.build()
    }

    private fun performSinglePointerGesture(gesture: PointerGesture) {
        val strokes = gesture.convertToStrokes()
        val callback = object : AccessibilityService.GestureResultCallback() {
            var currentIndex = 0
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                if (++currentIndex <= strokes.lastIndex) {
                    service.dispatchGesture(strokes[currentIndex].buildGesture(), this, null)
                }
            }
        }
        service.dispatchGesture(strokes[0].buildGesture(), callback, null)
    }

    private fun performMultiPointerGesture(vararg gesture: PointerGesture) {
        val strokesList = gesture.map { it.convertToStrokes() }
        val strokes = strokesList[0]
        val callback = object : AccessibilityService.GestureResultCallback() {
            var currentIndex = 0
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                if (++currentIndex <= strokes.lastIndex) {
                    service.dispatchGesture(strokes[currentIndex].buildGesture(), this, null)
                }
            }
        }
        service.dispatchGesture(strokes.buildGesture(), callback, null)
    }

}
