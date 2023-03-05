/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.main

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.app
import top.xjunz.tasker.ktx.observeTransient
import top.xjunz.tasker.ktx.setValueIfObserved
import top.xjunz.tasker.ui.main.MainViewModel.Companion.peekMainViewModel

/**
 * @author xjunz 2022/10/21
 */
object EventCenter {

    private const val SCHEME = "xtsk"

    private val EVENT = MutableLiveData<Pair<String, Any>>()

    private var transient: Any? = null

    /**
     * Observe when an event is received.
     */
    fun <V : Any> LifecycleOwner.doOnEventReceived(
        eventName: String,
        observer: (V) -> Unit
    ) {
        observeTransient(EVENT) {
            if (it.first == eventName) {
                observer(it.second.casted())
            }
        }
    }

    /**
     * Observe when the activity is launched because an event arrives.
     */
    fun <V> LifecycleOwner.doOnEventRoutedWithValue(
        route: String,
        observer: (V) -> Unit
    ) {
        peekMainViewModel().doOnRouted(this, route, observer)
    }

    /**
     * Observe when the activity is launched because an event arrives.
     */
    fun LifecycleOwner.doOnEventRouted(
        route: String,
        observer: () -> Unit
    ) {
        peekMainViewModel().doOnRouted(this, route, observer)
    }

    fun fetchTransientValue(): Any? {
        val temp = transient
        transient = null
        return temp
    }

    fun launchHost() {
        app.startActivity(
            Intent(Intent.ACTION_VIEW).setClass(app, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /**
     * Launch the activity and send an event to observers.
     */
    fun routeEvent(eventName: String, value: Any) {
        routeTo("$SCHEME://$eventName")
        transient = value
    }

    /**
     * Send an event to observers.
     */
    fun sendEvent(eventName: String, value: Any) {
        EVENT.setValueIfObserved(eventName to value)
    }

    private fun routeTo(url: String) {
        app.startActivity(
            Intent(Intent.ACTION_VIEW).setClass(app, MainActivity::class.java)
                .setData(Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}