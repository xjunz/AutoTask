package top.xjunz.tasker.task.applet

import org.junit.Test
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.applet.base.RootFlow

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
        val root = RootFlow()
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
        // Self
        assert(!root[0].isAheadOf(root[0]))
        // Peer
        assert(root[0].isAheadOf(root[1]))
        // Parent and child
        assert(root.isAheadOf(root[0]))
        assert(!root.getFlow(0)[0].isAheadOf(root))
        // Cross hierarchy
        assert(!root.getFlow(3).getFlow(0)[0].isAheadOf(root.getFlow(0)[0]))
        assert(root.getFlow(0)[0].isAheadOf(root.getFlow(3)[0]))
    }

    @Test
    fun getDepth() {
        val root = RootFlow()
        val d1 = Flow()
        root.add(d1)
        val d2 = Flow()
        d1.add(d2)
        root.buildHierarchy()
        val standalone = Flow()
        // Self
        assert(root.depth == 0)
        // In self
        assert(root.depthInAncestor(root) == 0)
        // Standalone
        assert(standalone.depthInAncestor(root) == -1)
        // Descendant
        println(d1.depth)
        assert(d1.depth == 1)
        assert(d2.depth == 2)
        // Descendant in parent
        assert(d2.depthInAncestor(d1) == 1)
    }
}