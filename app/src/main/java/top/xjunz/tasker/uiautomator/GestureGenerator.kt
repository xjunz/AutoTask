/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.uiautomator

import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.graphics.Path
import android.graphics.Point
import android.os.Build
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.annotation.RequiresApi
import androidx.test.uiautomator.PointerGesture
import top.xjunz.shared.trace.logcat
import top.xjunz.shared.utils.unsupportedOperation

/**
 * @author xjunz 2021/9/19
 */
object GestureGenerator {

    private const val MOTION_EVENT_INJECTION_DELAY_MILLIS: Long = 5

    private fun makePath(first: Point, vararg rest: Point): Path {
        val path = Path()
        path.moveTo(first.x.toFloat(), first.y.toFloat())
        for (point in rest) {
            path.lineTo(point.x.toFloat(), point.y.toFloat())
        }
        return path
    }

    private fun makePath(points: Array<out Point>): Path {
        val path = Path()
        points.forEachIndexed { i, v ->
            if (i == 0) {
                path.moveTo(v.x.toFloat(), v.y.toFloat())
            } else {
                path.lineTo(v.x.toFloat(), v.y.toFloat())
            }
        }
        return path
    }

    private fun makeStandaloneStroke(path: Path, duration: Long): StrokeDescription {
        return StrokeDescription(path, 0, duration)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun makeContinuableStroke(path: Path, duration: Long): StrokeDescription {
        return StrokeDescription(path, 0, duration, true)
    }

    private fun makeGesture(strokes: Iterable<StrokeDescription>): GestureDescription {
        return GestureDescription.Builder().apply {
            for (stroke in strokes) {
                addStroke(stroke)
            }
        }.build()
    }

    private fun makeGesture(vararg strokes: StrokeDescription): GestureDescription {
        return GestureDescription.Builder().apply {
            for (stroke in strokes) {
                addStroke(stroke)
            }
        }.build()
    }

    private fun makeTapGesture(x: Int, y: Int, timeout: Long) =
        makeGesture(makeStandaloneStroke(makePath(Point(x, y)), timeout))

    fun makeClickGesture(x: Int, y: Int) =
        makeTapGesture(x, y, ViewConfiguration.getTapTimeout().toLong())

    fun makeLongClickGesture(x: Int, y: Int) =
        makeTapGesture(x, y, ViewConfiguration.getLongPressTimeout().toLong())

    fun makeSwipeGesture(downX: Int, downY: Int, upX: Int, upY: Int, steps: Int, drag: Boolean) =
        if (drag) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val f = makeContinuableStroke(
                    makePath(Point(downX, downY)),
                    ViewConfiguration.getLongPressTimeout().toLong()
                )
                val s = f.continueStroke(
                    makePath(Point(downX, downY), Point(upX, upY)), 0,
                    MOTION_EVENT_INJECTION_DELAY_MILLIS * steps, true
                )
                val t = s.continueStroke(
                    makePath(Point(downX, downY)),
                    0,
                    ViewConfiguration.getTapTimeout().toLong(),
                    false
                )
                makeGesture(f, s, t)
            } else {
                unsupportedOperation("Drag & swipe is not supported under API 26")
            }
        } else {
            makeGesture(
                makeStandaloneStroke(
                    makePath(Point(downX, downY), Point(upX, upY)),
                    steps * MOTION_EVENT_INJECTION_DELAY_MILLIS
                )
            )
        }

    fun makeSwipeGesture(segments: Array<out Point>?, segmentSteps: Int): GestureDescription {
        check(segments != null && segments.isNotEmpty())
        return makeGesture(
            makeStandaloneStroke(
                makePath(segments),
                segments.size * segmentSteps * MOTION_EVENT_INJECTION_DELAY_MILLIS
            )
        )
    }

    /**
     * Convert [PointerGesture]s to a [GestureDescription] for the accessibility service to perform.
     */
    fun PointerGesture.convertToStrokes(): List<StrokeDescription> {
        val strokes = mutableListOf<StrokeDescription>()
        val iterator = actions.iterator()
        var stroke: StrokeDescription? = null
        var duration = 0L
        var prev: Point? = null
        while (iterator.hasNext()) {
            val action = iterator.next()
            // ignore actions with no duration
            if (action.duration == 0L) {
                logcat("ignored")
                continue
            }
            val end = action.end
            if (prev == null) {
                prev = action.start
            }
            checkNotNull(prev)
            val path: Path
            if (end == prev) {
                // make 1px offset
                val offset = Point(end.x + 1, end.y)
                path = makePath(prev, offset)
                prev = offset
            } else {
                path = makePath(prev, end)
                prev = end
            }
            stroke = if (stroke == null) {
                if (iterator.hasNext()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        StrokeDescription(path, 0, action.duration, true)
                    } else {
                        unsupportedOperation("Continuable stroke is not supported under Android O")
                    }
                } else {
                    StrokeDescription(path, 0, action.duration)
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    stroke.continueStroke(path, 0, action.duration, iterator.hasNext())
                } else {
                    unsupportedOperation("Continuable stroke is not supported under Android O")
                }
            }
            duration += action.duration
            strokes.add(requireNotNull(stroke))
        }
        return strokes
    }

    fun convertToStrokes(vararg touches: Array<out MotionEvent.PointerCoords>): GestureDescription {
        val strokes = Array(touches.size) {
            val coordsSerial = touches[it]
            val path = Path()
            coordsSerial.forEachIndexed { i, v ->
                if (i == 0) {
                    path.moveTo(v.x, v.y)
                } else {
                    path.lineTo(v.x, v.y)
                }
            }
            makeStandaloneStroke(path, coordsSerial.size * MOTION_EVENT_INJECTION_DELAY_MILLIS)
        }
        return makeGesture(*strokes)
    }
}
