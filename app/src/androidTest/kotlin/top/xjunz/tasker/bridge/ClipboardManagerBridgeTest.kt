package top.xjunz.tasker.bridge

import org.junit.Test

/**
 * @author xjunz 2022/11/16
 */
internal class ClipboardManagerBridgeTest {

    @Test
    fun copyToClipboard() {
        ClipboardManagerBridge.copyToClipboard("hello")
    }
}