package top.xjunz.tasker.task.inspector

import android.graphics.Rect
import android.os.Parcel
import android.os.Parcelable
import android.view.accessibility.AccessibilityNodeInfo
import top.xjunz.tasker.ktx.getVisibleBoundsIn
import top.xjunz.tasker.ktx.readParcelable
import top.xjunz.tasker.ktx.requireParcelable
import java.io.BufferedWriter

/**
 * You can treat this as a special [AccessibilityNodeInfo], whose node info would never
 * become stale. Use [StableNodeUtil.freeze] to construct an instance.
 *
 * @author xjunz 2021/9/22
 */
class StableNode constructor(
    val source: AccessibilityNodeInfo,
    var parent: StableNode? = null, var child: StableNode? = null,
    var prev: StableNode? = null, var next: StableNode? = null
) : Parcelable {

    private var previousGlobalBounds: Rect? = null

    private lateinit var bounds: Rect

    constructor(parcel: Parcel) : this(
        parcel.requireParcelable(),
        parcel.readParcelable(),
        parcel.readParcelable(),
        parcel.readParcelable(),
        parcel.readParcelable()
    )

    fun getVisibleBoundsIn(rect: Rect): Rect {
        if (rect != previousGlobalBounds) {
            bounds = source.getVisibleBoundsIn(rect)
            previousGlobalBounds = rect
        }
        return bounds
    }

    private fun containsPoint(x: Int, y: Int) = bounds.contains(x, y)

    fun findNodeUnder(x: Int, y: Int): StableNode? {
        var targetNode: StableNode? = null
        var nextNode = child
        while (nextNode != null) {
            if (nextNode.containsPoint(x, y)) {
                val possibleNode = nextNode.findNodeUnder(x, y)
                if (possibleNode != null) {
                    // we prefer the smaller one, because it's more likely to be concerned
                    if (targetNode == null || targetNode.bounds.contains(possibleNode.bounds)) {
                        targetNode = possibleNode
                    }
                } else {
                    targetNode = nextNode
                }
            }
            nextNode = nextNode.next
        }
        // neither its brothers nor its children contain the point, check itself then
        if (targetNode == null && containsPoint(x, y)) {
            return this
        }
        return targetNode
    }

    /**
     * Dump the whole node tree with a [writer] in XML style.
     */
    fun dump(writer: BufferedWriter) {
        dump(0, writer)
        writer.flush()
        writer.close()
    }

    private fun dump(indentCount: Int, writer: BufferedWriter) {
        val indent = String(CharArray(indentCount * 2) { ' ' })
        writer.write("<${source.className}")
        if (child != null) {
            writer.write(">")
            writer.newLine()
            child!!.dump(indentCount + 2, writer)
        } else {
            writer.write("/>")
        }
        var next = next
        while (next != null) {
            writer.newLine()
            next.dump(indentCount, writer)
            next = next.next
        }
        if (child != null) {
            writer.newLine()
            writer.write(indent)
            writer.write("</${source.className}>")
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(source, flags)
        parcel.writeParcelable(parent, flags)
        parcel.writeParcelable(child, flags)
        parcel.writeParcelable(prev, flags)
        parcel.writeParcelable(next, flags)
        parcel.writeParcelable(previousGlobalBounds, flags)
        parcel.writeParcelable(bounds, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<StableNode> {
        override fun createFromParcel(parcel: Parcel): StableNode {
            return StableNode(parcel)
        }

        override fun newArray(size: Int): Array<StableNode?> {
            return arrayOfNulls(size)
        }
    }

}
