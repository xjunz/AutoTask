package top.xjunz.tasker.task.applet

import org.junit.Test
import top.xjunz.tasker.engine.applet.base.Flow

/**
 * @author xjunz 2022/11/30
 */
internal class AppletsKtTest {

    private fun Flow.buildHierarchy() {
        forEachIndexed { index, applet ->
            applet.parent = this
            applet.index = index
            if (applet is Flow)
                applet.buildHierarchy()
        }
    }

    private fun Flow.getFlow(index: Int): Flow {
        return get(index) as Flow
    }

    @Test
    fun isAheadOf() {
        val root = Flow()
        root.add(Flow().apply {
            add(Flow())
        })
        root.add(Flow())
        root.add(Flow())
        root.add(Flow().apply {
            add(Flow().apply {
                add(Flow())
            })
        })
        root.buildHierarchy()
        // self
        assert(!root[0].isAheadOf(root[0]))
        // peer
        assert(root[0].isAheadOf(root[1]))
        // parent and child
        assert(root.isAheadOf(root[0]))
        assert(!root.getFlow(0)[0].isAheadOf(root))
        // cross hierarchy
        assert(!root.getFlow(3).getFlow(0)[0].isAheadOf(root.getFlow(0)[0]))
        assert(root.getFlow(0)[0].isAheadOf(root.getFlow(3)[0]))
    }
}