package top.xjunz.tasker.ktx

import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.ParcelCompat

/**
 * @author xjunz 2022/10/10
 */

inline fun <reified T : Parcelable> Parcel.readParcelable(): T? {
    val cls = T::class.java
    return ParcelCompat.readParcelable(this, cls.classLoader, cls)
}

inline fun <reified T : Parcelable> Parcel.requireParcelable(): T {
    val cls = T::class.java
    return ParcelCompat.readParcelable(this, cls.classLoader, cls)!!
}