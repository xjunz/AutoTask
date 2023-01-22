/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet

import org.junit.Test
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.applet.base.RootFlow
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2022/11/30
 */
internal class AppletsKtTest {

    private class EmptyApplet : Applet() {

        override val valueType: Int = VAL_TYPE_IRRELEVANT

        override suspend fun apply(runtime: TaskRuntime): AppletResult {
            return AppletResult.SUCCESS
        }

    }

    private fun Flow.getFlow(index: Int): Flow {
        return get(index) as Flow
    }

    @Test
    fun requireChild() {
        val root = RootFlow()
        val d1 = Flow()
        val d1i1 = Flow()
        root.add(d1)
        root.add(d1i1)
        val d2 = Flow()
        d1.add(d2)
        root.buildHierarchy()
        assert(root.requireChild(d2.hierarchy) === d2)
        assert(root.requireChild(d1i1.hierarchy) === d1i1)
        assert(root.requireChild(0) === root)
    }

    @Test
    fun isAheadOf() {
        val root = RootFlow()
        root.add(Flow().apply {
            add(Flow())
        })
        root.add(EmptyApplet())
        root.add(EmptyApplet())
        root.add(Flow().apply {
            add(Flow().apply {
                add(EmptyApplet())
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