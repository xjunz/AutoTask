package top.xjunz.tasker.util

import android.view.View
import android.view.View.OnClickListener

/**
 * @author xjunz 2022/11/29
 */
object AntiMonkey {

    fun View.setAntiMoneyClickListener(moneyClickInterval: Int = 350, listener: OnClickListener) {
        setOnClickListener(object : OnClickListener {

            var prevClickTimestamp = -1L

            override fun onClick(v: View?) {
                if (prevClickTimestamp == -1L
                    || System.currentTimeMillis() - prevClickTimestamp > moneyClickInterval
                ) {
                    listener.onClick(v)
                    prevClickTimestamp = System.currentTimeMillis()
                }
            }
        })
    }
}