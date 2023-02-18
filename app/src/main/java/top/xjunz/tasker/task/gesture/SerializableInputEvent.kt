/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.gesture

import android.accessibilityservice.AccessibilityService
import android.graphics.Point
import android.media.AudioManager
import android.view.KeyEvent
import androidx.test.uiautomator.PointerGesture
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import top.xjunz.shared.utils.illegalArgument
import top.xjunz.tasker.bridge.ContextBridge
import top.xjunz.tasker.isAppProcess
import top.xjunz.tasker.service.uiAutomation
import top.xjunz.tasker.service.uiAutomatorBridge
import top.xjunz.tasker.uiautomator.CoroutineGestureController

/**
 * @author xjunz 2023/02/17
 */
@Serializable
class SerializableInputEvent(
    @SerialName("t")
    val type: Int = INPUT_TYPE_MOTIONS,
    /**
     * The event serial.
     */
    @SerialName("s")
    val serial: String,
    /**
     * A user readable label.
     */
    @SerialName("l")
    var label: String? = null
) {

    /**
     * Start delay.
     */
    @SerialName("d")
    var delay: Long = 0
        set(value) {
            field = value
            getGesture().setDelay(value)
        }

    companion object {

        const val INPUT_TYPE_MOTIONS = 0
        const val INPUT_TYPE_KEY = 1

        private const val MOTION_FLATTEN_SEPARATOR = ';'

        fun wrap(gesture: PointerGesture, label: String? = null): SerializableInputEvent {
            val instance = SerializableInputEvent(
                INPUT_TYPE_MOTIONS, gesture.flattenToString(), label
            )
            instance.delay = gesture.delay()
            instance.gesture = gesture
            return instance
        }

        fun wrap(keyCode: Int, delay: Long = 0): SerializableInputEvent {
            val instance = SerializableInputEvent(INPUT_TYPE_KEY, keyCode.toString())
            instance.delay = delay
            return instance
        }

        private fun Point.flatten(duration: Long): String {
            return (x.toLong() shl 16 or y.toLong() or (duration shl 32)).toString(16)
        }

        private fun PointerGesture.flattenToString(): String {
            val sb = StringBuilder()
            var point: Point
            actions.forEach {
                point = if (sb.isEmpty()) it.start else it.end
                sb.append(point.flatten(it.duration)).append(MOTION_FLATTEN_SEPARATOR)
            }
            return sb.toString()
        }
    }

    @Transient
    private var gesture: PointerGesture? = null

    fun getGesture(): PointerGesture {
        if (gesture == null) {
            check(type == INPUT_TYPE_MOTIONS)
            serial.splitToSequence(MOTION_FLATTEN_SEPARATOR).filter { it.isNotEmpty() }.forEach {
                val composed = it.toLong(16)
                val x = composed ushr 16 and 0xFFFF
                val y = composed and 0xFFFF
                with(gesture) {
                    if (this == null) {
                        gesture = PointerGesture(Point(x.toInt(), y.toInt()), delay)
                    } else {
                        moveWithDuration(Point(x.toInt(), y.toInt()), composed ushr 32)
                    }
                }
            }
        }
        return requireNotNull(gesture)
    }

    fun getKeyCode(): Int {
        check(type == INPUT_TYPE_KEY)
        return serial.toInt()
    }

    suspend fun execute(): Boolean {
        val successful = if (type == INPUT_TYPE_MOTIONS) {
            (uiAutomatorBridge.gestureController as CoroutineGestureController)
                .performSinglePointerGesture(getGesture())
        } else {
            delay(delay)
            executeKeyCode(getKeyCode())
        }
        if (!successful) return false
        return true
    }

    private fun executeKeyCode(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK -> uiAutomation.performGlobalAction(
                AccessibilityService.GLOBAL_ACTION_BACK
            )
            KeyEvent.KEYCODE_HOME -> uiAutomation.performGlobalAction(
                AccessibilityService.GLOBAL_ACTION_HOME
            )
            KeyEvent.KEYCODE_APP_SWITCH -> uiAutomation.performGlobalAction(
                AccessibilityService.GLOBAL_ACTION_RECENTS
            )
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (isAppProcess) {
                    ContextBridge.getContext().getSystemService(AudioManager::class.java)
                        .adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                    true
                } else {
                    uiAutomation.injectInputEvent(
                        KeyEvent(KeyEvent.ACTION_DOWN, keyCode), true
                    ) && uiAutomation.injectInputEvent(
                        KeyEvent(KeyEvent.ACTION_UP, keyCode), true
                    )
                }
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (isAppProcess) {
                    ContextBridge.getContext().getSystemService(AudioManager::class.java)
                        .adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                    true
                } else {
                    uiAutomation.injectInputEvent(
                        KeyEvent(KeyEvent.ACTION_DOWN, keyCode), true
                    ) && uiAutomation.injectInputEvent(
                        KeyEvent(KeyEvent.ACTION_UP, keyCode), true
                    )
                }
            }
            else -> illegalArgument("keyCode", keyCode)
        }
    }
}