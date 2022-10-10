package top.xjunz.tasker.task.factory

import org.junit.Test

/**
 * @author xjunz 2022/09/26
 */
internal class AppletRegistryTest {

    @Test
    fun testAppletFactory() {
        val options = PackageAppletFactory().categorizedOptions
        assert(options.size == 3)
    }

}