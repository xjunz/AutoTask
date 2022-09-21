package top.xjunz.tasker.model

import android.graphics.Rect
import android.os.Parcel
import android.os.Parcelable
import top.xjunz.tasker.util.Records

/**
 * @author xjunz 2021/8/9
 */
data class Record(
    val pkgName: String, var count: Int = 0, var text: String? = null,
    var portraitBounds: Rect? = null, var landscapeBounds: Rect? = null,
    var firstTimestamp: Long = 0, var latestTimestamp: Long = 0,
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readInt(),
        parcel.readString(),
        parcel.readParcelable(Rect::class.java.classLoader),
        parcel.readParcelable(Rect::class.java.classLoader),
        parcel.readLong(),
        parcel.readLong()
    )

    override fun hashCode(): Int {
        return pkgName.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Record
        if (pkgName != other.pkgName) return false
        return true
    }

    /**
     * @see Records.parse()
     */
    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(pkgName)
            .append(";$count")
            .append(";${text?.replace('\n', '\\')}")
            .append(";${portraitBounds?.flattenToString()}")
            .append(";${landscapeBounds?.flattenToString()}")
            .append(";$firstTimestamp")
            .append(";$latestTimestamp")
        return sb.toString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(pkgName)
        parcel.writeInt(count)
        parcel.writeString(text)
        parcel.writeParcelable(portraitBounds, flags)
        parcel.writeParcelable(landscapeBounds, flags)
        parcel.writeLong(firstTimestamp)
        parcel.writeLong(latestTimestamp)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Record> {
        const val COMPONENT_COUNT = 7

        override fun createFromParcel(parcel: Parcel): Record {
            return Record(parcel)
        }

        override fun newArray(size: Int): Array<Record?> {
            return arrayOfNulls(size)
        }
    }

}
