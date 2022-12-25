package top.xjunz.shared.ktx

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

inline fun <reified K, reified V> Parcel.readMap(): Map<K, V>? {
    return ParcelCompat.readHashMap(
        this, HashMap::class.java.classLoader, K::class.java, V::class.java
    )
}

fun Parcel.writeStringArgs(vararg args: String) {
    writeStringArray(args)
}

fun Parcel.readBool(): Boolean = ParcelCompat.readBoolean(this)

fun Parcel.writeBool(bool: Boolean) = ParcelCompat.writeBoolean(this, bool)