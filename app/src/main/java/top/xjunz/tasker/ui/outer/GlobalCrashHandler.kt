/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.outer

import android.app.ApplicationErrorReport.CrashInfo
import android.util.PrintStreamPrinter
import top.xjunz.shared.trace.logcatStackTrace
import top.xjunz.tasker.app
import top.xjunz.tasker.ktx.launchActivity
import top.xjunz.tasker.service.currentService
import top.xjunz.tasker.service.serviceController
import top.xjunz.tasker.util.Feedbacks
import top.xjunz.tasker.util.formatCurrentTime
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import kotlin.system.exitProcess

object GlobalCrashHandler : Thread.UncaughtExceptionHandler {

    fun init() {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    private const val CRASH_DIR_PATH = "crash"

    override fun uncaughtException(t: Thread, e: Throwable) {
        try {
            e.logcatStackTrace()
            if (serviceController.isServiceRunning) {
                currentService.destroy()
            }
            val date: String = formatCurrentTime()
            val crashLogDir = File(app.cacheDir, CRASH_DIR_PATH)
            val crashLogFile = File(crashLogDir, "$date.txt")
            if ((crashLogDir.exists() || crashLogDir.mkdirs()) && crashLogFile.createNewFile()) {
                FileOutputStream(crashLogFile, true).use {
                    val printer = PrintStreamPrinter(PrintStream(it, true))
                    printer.println(date)
                    printer.println(Feedbacks.dumpEnvInfo())
                    CrashInfo(e).dump(printer, "")
                }
                app.launchActivity(CrashReportActivity::class.java) {
                    putExtra(CrashReportActivity.EXTRA_LOG_FILE_PATH, crashLogFile.path)
                }
            }
        } catch (e: Throwable) {
            e.logcatStackTrace()
        } finally {
            exitProcess(1)
        }
    }
}