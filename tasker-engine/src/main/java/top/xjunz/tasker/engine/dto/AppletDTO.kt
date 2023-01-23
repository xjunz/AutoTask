/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.dto

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import top.xjunz.shared.ktx.readBool
import top.xjunz.shared.ktx.readMap
import top.xjunz.shared.ktx.writeBool
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.applet.factory.AppletFactory
import java.util.zip.CRC32

/**
 * Data Transfer Object for [Applet].
 *
 * @author xjunz 2022/10/28
 */
@Serializable
@SerialName("E") // Element
class AppletDTO(
    private val id: Int,
    @SerialName("a")
    private val isAnd: Boolean = true,
    @SerialName("en")
    private val isEnabled: Boolean = true,
    @SerialName("i")
    private val isInverted: Boolean = false,
    @SerialName("v")
    private val serialized: String? = null,
    @SerialName("c")
    private val comment: String? = null,
    @SerialName("q")
    private val referents: Map<Int, String>? = null,
    @SerialName("r")
    private val references: Map<Int, String>? = null,
) : Parcelable {

    @SerialName("e")
    private var elements: Array<AppletDTO>? = null

    companion object CREATOR : Parcelable.Creator<AppletDTO> {
        override fun createFromParcel(parcel: Parcel): AppletDTO {
            return AppletDTO(parcel)
        }

        override fun newArray(size: Int): Array<AppletDTO?> {
            return arrayOfNulls(size)
        }
    }

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readBool(),
        parcel.readBool(),
        parcel.readBool(),
        parcel.readString(),
        parcel.readString(),
        parcel.readMap(),
        parcel.readMap(),
    ) {
        elements = parcel.createTypedArray(CREATOR)
    }

    private fun Boolean.toInt(): Int = if (this) 1 else 0

    internal fun calculateChecksum(crc32: CRC32) {
        crc32.apply {
            update(id)
            update(isAnd.toInt())
            update(isEnabled.toInt())
            update(isInverted.toInt())
            serialized?.let {
                update(it.toByteArray())
            }
            referents?.forEach { (t, u) ->
                update(t)
                update(u.toByteArray())
            }
            references?.forEach { (t, u) ->
                update(t)
                update(u.toByteArray())
            }
            comment?.let {
                update(it.toByteArray())
            }
            elements?.forEach {
                it.calculateChecksum(crc32)
            }
        }
    }

    object Serializer {

        private fun <K, V> Map<K, V>.emptyToNull(): Map<K, V>? {
            if (isEmpty()) return null
            return this
        }

        /**
         * Convert a normal applet to a serializable applet.
         */
        fun Applet.toDTO(): AppletDTO {
            val dto = AppletDTO(
                id, isAnd, isEnabled, isInverted, serializeValue(), comment,
                referents.emptyToNull(), references.emptyToNull(),
            )
            if (this is Flow) {
                dto.elements = if (size == 0) null else Array(size) {
                    this[it].toDTO()
                }
            }
            return dto
        }
    }

    fun toApplet(registry: AppletFactory): Applet {
        val prototype = registry.createAppletById(id)
        prototype.isAnd = isAnd
        prototype.isEnabled = isEnabled
        prototype.isInverted = isInverted
        prototype.referents = referents ?: emptyMap()
        prototype.references = references ?: emptyMap()
        prototype.comment = comment
        if (prototype is Flow) {
            elements?.forEach {
                prototype.add(it.toApplet(registry))
            }
        }
        if (serialized != null) prototype.deserializeValue(serialized)
        return prototype
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeBool(isAnd)
        parcel.writeBool(isEnabled)
        parcel.writeBool(isInverted)
        parcel.writeString(serialized)
        parcel.writeString(comment)
        parcel.writeMap(referents)
        parcel.writeMap(references)
        parcel.writeTypedArray(elements, flags)
    }

    override fun describeContents(): Int {
        return 0
    }
}