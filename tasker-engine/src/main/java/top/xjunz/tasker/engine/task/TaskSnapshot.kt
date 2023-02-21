/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.task

import android.os.Parcel
import android.os.Parcelable
import android.util.ArrayMap
import androidx.core.os.ParcelCompat
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.applet.base.RootFlow
import top.xjunz.tasker.engine.runtime.AppletIndexer

/**
 * @author xjunz 2023/01/24
 */
class TaskSnapshot(
    val checksum: Long,
    var startTimestamp: Long = -1,
    var endTimestamp: Long = -1,
    var isSuccessful: Boolean = false,
) : Parcelable {

    val isRunning get() = endTimestamp == -1L

    var successes: MutableSet<Long> = mutableSetOf()

    var failures: MutableSet<Failure> = mutableSetOf()

    var current: Long = -1

    val duration: Int get() = (endTimestamp - startTimestamp).toInt()

    lateinit var succeededApplets: List<Applet>

    lateinit var failedApplets: Map<Applet, Failure>

    var currentApplet: Applet? = null

    fun loadApplets(root: RootFlow) {
        succeededApplets = successes.map {
            getAppletWithHierarchy(root, it)
        }
        val failed = ArrayMap<Applet, Failure>()
        failures.forEach {
            failed[getAppletWithHierarchy(root, it.hierarchy)] = it
        }
        failedApplets = failed
        if (current != -1L)
            currentApplet = getAppletWithHierarchy(root, current)
    }

    private fun getAppletWithHierarchy(root: RootFlow, hierarchy: Long): Applet {
        val parsed = AppletIndexer.parse(hierarchy)
        var applet: Applet = root
        parsed.forEach { i ->
            applet = (applet as Flow)[i]
        }
        return applet
    }

    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readByte() != 0.toByte()
    ) {
        successes = parcel.createLongArray()!!.toMutableSet()
        failures = ParcelCompat.readParcelableArray(
            parcel,
            Failure::class.java.classLoader,
            Failure::class.java
        )!!.toMutableSet()
        current = parcel.readLong()
    }


    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(checksum)
        parcel.writeLong(startTimestamp)
        parcel.writeLong(endTimestamp)
        parcel.writeByte(if (isSuccessful) 1 else 0)
        parcel.writeLongArray(successes.toLongArray())
        parcel.writeParcelableArray(failures.toTypedArray(), 0)
        parcel.writeLong(current)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun isRedundantTo(other: TaskSnapshot): Boolean {
        if (this === other) return false
        if (checksum != other.checksum) return false
        if (isSuccessful != other.isSuccessful) return false
        if (isRunning != other.isRunning) return false
        if (!successes.toTypedArray().contentEquals(other.successes.toTypedArray())) return false
        if (!failures.toTypedArray().contentEquals(other.failures.toTypedArray())) return false
        return true
    }

    companion object CREATOR : Parcelable.Creator<TaskSnapshot> {
        override fun createFromParcel(parcel: Parcel): TaskSnapshot {
            return TaskSnapshot(parcel)
        }

        override fun newArray(size: Int): Array<TaskSnapshot?> {
            return arrayOfNulls(size)
        }
    }

    class Failure(
        val hierarchy: Long,
        val actual: String?,
        val exception: String?
    ) : Parcelable {

        constructor(parcel: Parcel) : this(
            parcel.readLong(),
            parcel.readString(),
            parcel.readString()
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeLong(hierarchy)
            parcel.writeString(actual)
            parcel.writeString(exception)
        }

        override fun describeContents(): Int {
            return 0
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Failure

            if (hierarchy != other.hierarchy) return false
            if (actual != other.actual) return false
            if (exception != other.exception) return false

            return true
        }

        override fun hashCode(): Int {
            var result = hierarchy.hashCode()
            result = 31 * result + (actual?.hashCode() ?: 0)
            result = 31 * result + (exception?.hashCode() ?: 0)
            return result
        }

        companion object CREATOR : Parcelable.Creator<Failure> {

            fun fromAppletResult(hierarchy: Long, result: AppletResult): Failure {
                return Failure(
                    hierarchy,
                    result.actual?.toString(),
                    result.throwable?.stackTraceToString()
                )
            }

            override fun createFromParcel(parcel: Parcel): Failure {
                return Failure(parcel)
            }

            override fun newArray(size: Int): Array<Failure?> {
                return arrayOfNulls(size)
            }
        }

    }

}