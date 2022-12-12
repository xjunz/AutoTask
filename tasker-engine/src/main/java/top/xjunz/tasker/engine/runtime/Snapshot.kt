package top.xjunz.tasker.engine.runtime

/**
 * @author xjunz 2022/12/04
 */
class Snapshot {

    val registry = mutableMapOf<Int, Any>()

    fun clear() {
        registry.clear()
    }
}