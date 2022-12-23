package top.xjunz.tasker.engine.runtime

import org.junit.Before
import org.junit.Test
import top.xjunz.tasker.engine.applet.base.Applet

/**
 * @author xjunz 2022/12/23
 */
internal class AppletIndexerTest {

    private val indexer = AppletIndexer()

    @Before
    fun setUp() {
        indexer.jumpIn()
        indexer.moveTo(0)
        indexer.jumpIn()
        indexer.moveTo(12)
        indexer.jumpIn()
    }

    @Test
    fun testOverflow() {
        assert(runCatching { indexer.moveTo(Applet.MAX_FLOW_CHILD_COUNT) }.isFailure)
        assert(runCatching { indexer.moveTo(Applet.MAX_FLOW_CHILD_COUNT - 1) }.isSuccess)
        for (i in 0..6) {
            if (i == 6) {
                assert(runCatching {
                    indexer.jumpIn()
                    indexer.moveTo(i)
                }.isFailure)
            } else {
                indexer.jumpIn()
                indexer.moveTo(i)
            }
        }
        println(indexer.formatHierarchy())
    }

    @Test
    fun getDepth() {
        check(indexer.depth == 3)
    }

    @Test
    fun moveTo() {
        assert(indexer.getIndexInDepth(3) == -1)
        indexer.moveTo(0x32)
        assert(indexer.getIndexInDepth(3) == 0x32)
        indexer.moveTo(0x12)
        assert(indexer.getIndexInDepth(3) == 0x12)
    }

    @Test
    fun jumpIn() {
        indexer.jumpIn()
        assert(indexer.depth == 4)
    }

    @Test
    fun jumpOut() {
        indexer.jumpOut()
        assert(indexer.depth == 2)
    }

    @Test
    fun formatHierarchy() {
        val hierarchy = indexer.formatHierarchy()
        println(hierarchy)
        assert(hierarchy == "0 > 12 > -1")
    }
}