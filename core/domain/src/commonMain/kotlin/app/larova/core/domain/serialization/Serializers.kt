package app.larova.core.domain.serialization

import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Identifiers and timestamps are written into export files, so their wire form is a compatibility
 * commitment rather than an implementation detail. Both are spelled out here instead of relying on
 * whichever serializer a library version happens to supply by default: a UUID that starts being
 * written as a byte array, or a timestamp that loses its offset, would make yesterday's backup
 * unreadable.
 */
@OptIn(ExperimentalUuidApi::class)
object UuidSerializer : KSerializer<Uuid> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("app.larova.Uuid", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Uuid) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): Uuid = Uuid.parse(decoder.decodeString())
}

/** ISO-8601 with an offset, which is what `Instant.toString()` produces. */
object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("app.larova.Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())
}
