/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.outer

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import top.xjunz.tasker.R
import top.xjunz.tasker.app
import top.xjunz.tasker.databinding.ActivityCrashReportBinding
import top.xjunz.tasker.ktx.indexOf
import top.xjunz.tasker.ktx.launchActivity
import top.xjunz.tasker.ktx.launchIntentSafely
import top.xjunz.tasker.ktx.makeContentUri
import top.xjunz.tasker.ktx.str
import top.xjunz.tasker.ktx.toast
import top.xjunz.tasker.ui.main.MainActivity
import top.xjunz.tasker.util.ClickListenerUtil.setNoDoubleClickListener
import top.xjunz.tasker.util.Feedbacks
import java.io.File

/**
 * @author xjunz 2023/10/28
 */
class CrashReportActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LOG_FILE_PATH = "xjunz.extra.LOG_FILE_PATH"
    }

    private val binding by lazy {
        ActivityCrashReportBinding.inflate(layoutInflater)
    }

    private lateinit var file: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val path = intent?.getStringExtra(EXTRA_LOG_FILE_PATH)
        if (path.isNullOrEmpty() || !File(path).exists()) {
            finishAfterTransition()
            toast("No error log")
        } else {
            file = File(path)
            binding.btnFeedback.isEnabled = true
            binding.tvAttachName.text = file.name
            binding.btnAddGroup.setNoDoubleClickListener {
                Feedbacks.addGroup()
            }
            lifecycleScope.launch(Dispatchers.IO) {
                runCatching {
                    val list = file.parentFile?.listFiles()!!
                    if (list.size > 10) {
                        list.sortBy { it.lastModified() }
                        if (list[0].path != path) list[0].delete()
                    }
                }.onFailure {
                    it.printStackTrace()
                }
            }
            binding.btnFeedback.setOnClickListener { btn ->
                val popup = PopupMenu(this, btn, Gravity.CENTER)
                popup.menu.add(R.string.feedback_email)
                popup.menu.add(R.string.send_to)
                popup.setOnMenuItemClickListener {
                    val uri = file.makeContentUri()
                    when (popup.menu.indexOf(it)) {
                        0 -> Feedbacks.feedbackByEmail(uri)

                        1 -> launchIntentSafely(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND)
                                    .addCategory(Intent.CATEGORY_DEFAULT)
                                    .putExtra(Intent.EXTRA_STREAM, uri)
                                    .setType("text/plain")
                                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                                R.string.send_to.str
                            )
                        )
                    }
                    return@setOnMenuItemClickListener true
                }
                popup.show()
            }
        }
        binding.btnDismiss.setOnClickListener {
            finishAndRemoveTask()
        }
        binding.btnRestart.setOnClickListener {
            app.launchActivity(MainActivity::class.java) {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        }
    }
}