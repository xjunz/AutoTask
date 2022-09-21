package top.xjunz.tasker.engine

import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import top.xjunz.tasker.engine.criterion.BaseCriterion
import top.xjunz.tasker.engine.flow.*

/**
 * A helper class for serialize and deserialize an [Applet] in JSON format.
 *
 * @author xjunz 2022/08/14
 */
object AppletSerializer {

    private val appletModule = SerializersModule {
        polymorphic(Applet::class) {
            subclass(
                BaseCriterion.serializer(
                    PolymorphicSerializer(Any::class), PolymorphicSerializer(Any::class)
                )
            )
            subclass(Flow::class)
            subclass(If::class)
            subclass(When::class)
            subclass(And::class)
            subclass(Or::class)
        }
    }

    private val formatter = Json {
        serializersModule = appletModule
    }

    fun Applet.formatToJson(): String {
        return formatter.encodeToString(this)
    }

    fun decodeFromJson(string: String): Applet {
        return formatter.decodeFromString(string)
    }
}