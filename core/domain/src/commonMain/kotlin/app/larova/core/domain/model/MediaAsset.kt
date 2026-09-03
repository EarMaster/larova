package app.larova.core.domain.model

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * A picture, video or recording.
 *
 * Large files do not belong in a database. The bytes live in app-private storage under
 * `filesDir/media/<uuid>.<ext>` and only this reference is stored, which also means no other app
 * can read them, no storage permission is needed, and uninstalling deletes them.
 *
 * [sha256] is what lets an import tell "the same file again" from "a different file with the same
 * name", and lets an export verify it arrived intact.
 *
 * Not `@Serializable`: the file format has its own row types in `export/ExportRows.kt`, for the
 * reason [Card] gives.
 */
@OptIn(ExperimentalUuidApi::class)
data class MediaAsset(
    val id: Uuid,
    /** Relative to the media root, so an export stays portable between installations. */
    val relativePath: String,
    val mimeType: String,
    val sizeBytes: Long,
    val sha256: String,
)
