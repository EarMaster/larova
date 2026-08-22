package app.larova.core.domain.model

import app.larova.core.domain.serialization.UuidSerializer
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

/**
 * A picture, video or recording.
 *
 * Large files do not belong in a database. The bytes live in app-private storage under
 * `filesDir/media/<uuid>.<ext>` and only this reference is stored, which also means no other app
 * can read them, no storage permission is needed, and uninstalling deletes them.
 *
 * [sha256] is what lets an import tell "the same file again" from "a different file with the same
 * name", and lets an export verify it arrived intact.
 */
@OptIn(ExperimentalUuidApi::class)
@Serializable
data class MediaAsset(
    @Serializable(with = UuidSerializer::class) val id: Uuid,
    /** Relative to the media root, so an export stays portable between installations. */
    val relativePath: String,
    val mimeType: String,
    val sizeBytes: Long,
    val sha256: String,
)
