/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.util

import org.junit.Assert.assertEquals
import org.junit.Test
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.applet.base.RootFlow

/**
 * @author xjunz 2022/11/30
 */
internal class AppletsKtTest {

    private val root = DslRootFlow {
        DslFlow {
            DslApplet {
                WithReferent("A")
                WithReferent("B")
            }
            DslFlow {
                DslApplet()
                DslApplet {
                    ReferTo("A")
                }
                DslFlow {
                    DslApplet(1) {
                        ReferTo("B")
                    }
                }
                DslApplet()
                DslApplet()
                EmptyFlow()
            }
        }
        DslFlow {
            DslApplet()
            DslApplet()
            DslApplet()
        }
        DslApplet(2) {
            ReferTo("A")
            ReferTo("B")
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
        val d1i2 = Flow()
        root.add(d1i2)
        val d2 = Flow()
        d1.add(d2)
        root.buildHierarchy()
        assert(root.findChildByHierarchy(d2.hierarchy) === d2)
        assert(root.findChildByHierarchy(d1i1.hierarchy) === d1i1)
        assert(root.findChildByHierarchy(d1i2.hierarchy) === d1i2)
        assert(root.findChildByHierarchy(0) === root)
    }

    @Test
    fun isAheadOf() {
        val root = RootFlow()
        root.add(Flow().apply {
            add(Flow())
        })
        root.add(DSLApplet())
        root.add(DSLApplet())
        root.add(Flow().apply {
            add(Flow().apply {
                add(DSLApplet())
            })
        })
        root.buildHierarchy()
        // Self
        assert(!root[0].isAheadOf(root[0]))
        // Peer
        assert(root[0].isAheadOf(root[1]))
        // Parent and child
        // assert(root.isAheadOf(root[0]))
        //assert(!root.getFlow(0)[0].isAheadOf(root))
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

    @Test
    fun forEachApplet() {
        root.buildHierarchy()
        assert(root.forEachApplet {
            it.id == 1
        })
        assert(root.forEachApplet {
            it.id == 2
        })
        assert(!root.forEachApplet {
            it.id == 3
        })
    }

    @Test
    fun forEachReferent() {
        assert(root.forEachReferent { applet, which, referent ->
            referent == "A"
        })
        assert(root.forEachReferent { applet, which, referent ->
            referent == "B"
        })
        assert(!root.forEachReferent { applet, which, referent ->
            println(referent)
            referent == "C"
        })
        assert(root.forEachReference { applet, which, referent ->
            referent == "A"
        })
        var count = 0
        root.forEachReference { applet, which, referent ->
            if (referent == "A") {
                count++
            }
            false
        }
        assertEquals(2, count)

    }
}