/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.base

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 *  A field describe a reference or referent by [which] one it is referred or referring and
 *  what its [name] (user marked) is.
 *
 * @author xjunz 2023/02/12
 */
@Serializable
class Field(
    @SerialName("i")
    val which: Int,
    @SerialName("n")
    val name: String
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString()!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(which)
        parcel.writeString(name)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Field> {
        override fun createFromParcel(parcel: Parcel): Field {
            return Field(parcel)
        }

        override fun newArray(size: Int): Array<Field?> {
            return arrayOfNulls(size)
        }
    }
}