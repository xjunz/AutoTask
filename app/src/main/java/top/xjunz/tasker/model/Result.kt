package top.xjunz.tasker.model

import android.graphics.Rect
import android.os.Parcel
import android.os.Parcelable

/**
 * @author xjunz 2021/7/12
 */
data class Result(
    var passed: Boolean = false, var maskedReason: Int = 0, var pkgName: String? = null, var text: String? = null,
    var bounds: Rect? = null, var parentBounds: Rect? = null, var portrait: Boolean? = null, var nodeHash: Int = -1
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readByte() != 0.toByte(),
        parcel.readInt(),
        parcel.readString(),
        parcel.readString(),
        parcel.readParcelable(Rect::class.java.classLoader),
        parcel.readParcelable(Rect::class.java.classLoader),
        parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        parcel.readInt()
    )

    fun getReason() = maskedReason and REASON_MASK_PARENT.inv() and REASON_MASK_PORTRAIT.inv()
        .and(REASON_MASK_TRANSVERSE.inv()) and REASON_MASK_NOT_CLICKABLE.inv()

    private fun isReasonPatriarchal() = maskedReason ushr 31 == 1
    private fun isReasonPortrait() = maskedReason ushr 29 and 0x1 == 1
    private fun isReasonTransverse() = maskedReason ushr 30 and 0x1 == 1
    private fun isChildNotClickable() = maskedReason ushr 28 and 0x1 == 1

    fun getInjectionType(): Int {
        check(passed) { "only passed results have its injection type!" }
        return if (isChildNotClickable() && isReasonPatriarchal()) INJECTION_EVENT else INJECTION_ACTION
    }

    fun maskReason(reason: Int) {
        maskedReason = maskedReason or reason
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("pkgName=$pkgName")
            .append("; passed=$passed")
            .append("; reason=${reasonToString()}")
            .append("; text=$text")
            // a new line here makes it more readable
            .append("; \nbounds=$bounds")
            .append("; parentBounds=$parentBounds")
            .append("; portrait=$portrait")
            .append("; nodeHash=$nodeHash")
        return sb.toString()
    }

    private fun reasonToString(): String {
        val sb = StringBuilder()
        if (isReasonPatriarchal()) {
            sb.append("parent|")
        }
        if (isChildNotClickable()) {
            sb.append("not clickable|")
        }
        sb.append(
            when (getReason()) {
                REASON_ILLEGAL_TEXT -> "illegal text"
                REASON_ILLEGAL_SIZE -> "illegal size"
                REASON_ILLEGAL_LOCATION -> "illegal location"
                REASON_ILLEGAL_TARGET -> "illegal target"
                REASON_INVISIBLE -> "invisible"
                REASON_EDITABLE -> "editable"
                REASON_ERROR -> "error"
                REASON_NONE -> "none"
                else -> throw IllegalArgumentException("no such reason: " + getReason().toString(2))
            }
        )
        if (isReasonPortrait()) {
            sb.append("|portrait")
        }
        if (isReasonTransverse()) {
            sb.append("|transverse")
        }
        return sb.toString()
    }

    fun reset() {
        passed = false
        maskedReason = 0
        pkgName = null
        text = null
        bounds = null
        parentBounds = null
        portrait = null
        nodeHash = -1
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Result

        if (passed != other.passed) return false
        if (maskedReason != other.maskedReason) return false
        if (text != other.text) return false
        if (nodeHash != other.nodeHash) return false

        return true
    }

    override fun hashCode(): Int {
        var result = passed.hashCode()
        result += maskedReason
        result = 31 * result + (text?.hashCode() ?: 0)
        result = 31 * result + nodeHash
        return result
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByte(if (passed) 1 else 0)
        parcel.writeInt(maskedReason)
        parcel.writeString(pkgName)
        parcel.writeString(text)
        parcel.writeParcelable(bounds, flags)
        parcel.writeParcelable(parentBounds, flags)
        parcel.writeValue(portrait)
        parcel.writeInt(nodeHash)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Result> {
        const val REASON_NONE = 0
        const val REASON_MASK_PARENT = 1 shl 31
        const val REASON_MASK_TRANSVERSE = 1 shl 30
        const val REASON_MASK_PORTRAIT = 1 shl 29
        const val REASON_MASK_NOT_CLICKABLE = 1 shl 28
        const val REASON_ILLEGAL_SIZE = 1 shl 1
        const val REASON_ILLEGAL_LOCATION = 1 shl 2
        const val REASON_INVISIBLE = 1 shl 3
        const val REASON_ILLEGAL_TEXT = 1 shl 4
        const val REASON_EDITABLE = 1 shl 5
        const val REASON_ILLEGAL_TARGET = 1 shl 10
        const val REASON_ERROR = 1 shl 11
        const val INJECTION_ACTION = 7
        const val INJECTION_EVENT = 11

        override fun createFromParcel(parcel: Parcel): Result {
            return Result(parcel)
        }

        override fun newArray(size: Int): Array<Result?> {
            return arrayOfNulls(size)
        }
    }

}
