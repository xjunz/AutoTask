/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.base

import androidx.annotation.CallSuper
import kotlinx.coroutines.CancellationException
import top.xjunz.shared.trace.logcatStackTrace
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * A flow is a set of [applets][Applet] but is also a special applet.
 *
 * @author xjunz 2022/08/04
 */
open class Flow(private val elements: MutableList<Applet> = ArrayList()) : Applet(),
    MutableList<Applet> by elements {

    open val minSize = 1

    open val maxSize = MAX_FLOW_CHILD_COUNT

    inline val requiredSize get() = if (minSize == maxSize) minSize else -1

    /**
     * Whether the applets are repeated executed.
     */
    open val isRepetitive: Boolean = false

    protected open suspend fun applyFlow(runtime: TaskRuntime): AppletResult {
        for ((index: Int, applet: Applet) in withIndex()) {
            runtime.ensureActive()
            if (runtime.shouldStop) break
            applet.onPreApply(runtime)
            // Always execute the first applet in a flow and skip an applet if its relation to
            // previous peer applet does not meet the previous execution result.
            if (runtime.shouldSkip()
                || applet.shouldSkip(runtime)
                || !applet.isEnabled
                || (index != 0 && !applet.isAnyway && applet.isAnd != runtime.isSuccessful)
            ) {
                applet.onSkipped(runtime)
                if (!isRepetitive) {
                    runtime.observer?.onAppletSkipped(applet, runtime)
                }
                continue
            }
            applet.parent = this
            applet.index = index
            runtime.currentApplet = applet
            runtime.tracker.moveTo(index)
            applet.onPrepareApply(runtime)
            if (!isRepetitive) {
                runtime.observer?.onAppletStarted(applet, runtime)
            }
            val result = try {
                applet.apply(runtime)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                t.logcatStackTrace()
                if (isRepetitive) {
                    AppletResult.EMPTY_FAILURE
                } else {
                    AppletResult.error(t)
                }
            }
            runtime.isSuccessful = result.isSuccessful
            // For repetitive applets, they may be executed multiple times and we cannot
            // tell which result is the more important one, so just ignore the result.
            if (!isRepetitive || result.isSuccessful) {
                runtime.registerResult(applet, result)
                runtime.observer?.onAppletTerminated(applet, runtime)
            }
            applet.onPostApply(runtime)
            result.recycle()
        }
        return AppletResult.emptyResult(runtime.isSuccessful)
    }

    open fun performStaticCheck(): StaticError? {
        val errorCode = staticCheckMyself()
        if (errorCode != StaticError.ERR_NONE) {
            return StaticError(this, errorCode)
        }
        forEach {
            if (it is Flow) {
                val error = it.performStaticCheck()
                if (error != null) {
                    return error
                }
            }
        }
        return null
    }

    @CallSuper
    protected open fun staticCheckMyself(): Int {
        // Code layer checks: find bugs
        check(size <= maxSize)
        // User layer checks: find improper operations
        if (requiredSize != 0 && isEmpty()) {
            return StaticError.ERR_FLOW_NO_ELEMENT
        }
        return StaticError.ERR_NONE
    }

    final override suspend fun apply(runtime: TaskRuntime): AppletResult {
        runtime.currentFlow = this
        runtime.tracker.jumpIn()
        // Backup the target, because sub-flows may change the target,
        // we don't want the changed target to fall through.
        val backup = runtime.getRawTarget()
        val ifSuccessful = runtime.ifSuccessful
        val loop = runtime.currentLoop
        try {
            return applyFlow(runtime)
        } finally {
            // Restore the target
            runtime.setTarget(backup)
            runtime.ifSuccessful = ifSuccessful
            runtime.currentLoop = loop
            runtime.tracker.jumpOut()
        }
    }
}