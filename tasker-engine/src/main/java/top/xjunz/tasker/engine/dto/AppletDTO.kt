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
import top.xjunz.tasker.engine.dto.AppletArgumentSerializer.deserializeArgumentsPreVersionCode16
import top.xjunz.tasker.engine.dto.AppletArgumentSerializer.deserializeValues
import top.xjunz.tasker.engine.dto.AppletArgumentSerializer.valuesToStringMap
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
    @SerialName("r")
    private val relation: Int = Applet.REL_AND,
    @SerialName("en")
    private val isEnabled: Boolean = true,
    @SerialName("i")
    private val isInverted: Boolean = false,
    @SerialName("v")
    @Deprecated("No longer used after version code 15, preserved for compatibility reason.")
    private val serialized: String? = null,
    @SerialName("c")
    private val comment: String? = null,
    @SerialName("vs")
    private val values: Map<Int, String>? = null,
    @SerialName("rft")
    private val referents: Map<Int, String>? = null,
    @SerialName("rfc")
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
        parcel.readInt(),
        parcel.readBool(),
        parcel.readBool(),
        parcel.readString(),
        parcel.readString(),
        parcel.readMap(),
        parcel.readMap(),
        parcel.readMap(),
    ) {
        elements = parcel.createTypedArray(CREATOR)
    }

    private fun Boolean.toInt(): Int = if (this) 1 else 0

    internal fun calculateChecksum(crc32: CRC32, prevVersionCode16: Boolean) {
        crc32.apply {
            update(id)
            update(relation)
            update(isEnabled.toInt())
            update(isInverted.toInt())
            if (prevVersionCode16) {
                serialized?.let {
                    update(it.toByteArray())
                }
            } else {
                values?.forEach { (t, u) ->
                    update(t)
                    update(u.toByteArray())
                }
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
                it.calculateChecksum(crc32, prevVersionCode16)
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
                id, relation, isEnabled, isInverted, null, comment,
                valuesToStringMap(), referents.emptyToNull(), references.emptyToNull(),
            )
            if (this is Flow) {
                dto.elements = if (size == 0) null else Array(size) {
                    this[it].toDTO()
                }
            }
            return dto
        }
    }

    fun toAppletPreVersionCode16(factory: AppletFactory): Applet {
        val prototype = factory.createAppletById(id, false)
        prototype.relation = relation
        prototype.isEnabled = isEnabled
        prototype.isInverted = isInverted
        prototype.referents = referents ?: emptyMap()
        prototype.references = references ?: emptyMap()
        prototype.comment = comment
        if (serialized != null) prototype.deserializeArgumentsPreVersionCode16(serialized)
        if (prototype is Flow) {
            elements?.forEach {
                prototype.add(it.toAppletPreVersionCode16(factory))
            }
        }
        return prototype
    }

    fun toApplet(factory: AppletFactory, compatMode: Boolean): Applet {
        val prototype = factory.createAppletById(id, compatMode)
        prototype.relation = relation
        prototype.isEnabled = isEnabled
        prototype.isInverted = isInverted
        prototype.referents = referents ?: emptyMap()
        prototype.references = references ?: emptyMap()
        prototype.comment = comment
        prototype.deserializeValues(values)
        if (prototype is Flow) {
            elements?.forEach {
                prototype.add(it.toApplet(factory, compatMode))
            }
        }
        return prototype
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeInt(relation)
        parcel.writeBool(isEnabled)
        parcel.writeBool(isInverted)
        parcel.writeString(null)
        // Ignore comment on transaction
        parcel.writeString(null)
        parcel.writeMap(values)
        parcel.writeMap(referents)
        parcel.writeMap(references)
        parcel.writeTypedArray(elements, flags)
    }

    override fun describeContents(): Int {
        return 0
    }
}