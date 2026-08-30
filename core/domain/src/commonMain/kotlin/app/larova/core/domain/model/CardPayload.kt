package app.larova.core.domain.model

import app.larova.core.domain.serialization.UuidSerializer
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * What a tile contains.
 *
 * Serialized into `Card.payload` as JSON, with the type carried in a `type` discriminator that
 * matches [CardType.key]. A new tile type therefore needs a renderer and nothing else: no schema
 * change and no migration.
 *
 * The `@SerialName` strings are export-file values. They are frozen.
 */
@OptIn(ExperimentalUuidApi::class)
@Serializable
sealed interface CardPayload {

    @Serializable
    @SerialName("guide")
    data class Guide(val steps: List<Step> = emptyList()) : CardPayload

    @Serializable
    @SerialName("note")
    data class Note(val text: String = "") : CardPayload

    @Serializable
    @SerialName("checklist")
    data class Checklist(
        val items: List<CheckItem> = emptyList(),
        val resetDaily: Boolean = false,
    ) : CardPayload

    @Serializable
    @SerialName("table")
    data class Table(
        val columns: List<String> = emptyList(),
        val rows: List<List<String>> = emptyList(),
    ) : CardPayload

    @Serializable
    @SerialName("video")
    data class Video(
        @Serializable(with = UuidSerializer::class) val mediaId: Uuid,
        val caption: String? = null,
    ) : CardPayload

    @Serializable
    @SerialName("audio")
    data class Audio(
        @Serializable(with = UuidSerializer::class) val mediaId: Uuid,
        val caption: String? = null,
    ) : CardPayload

    /**
     * A stored number. Larova never dials by itself: tapping hands off to the phone app with the
     * number ready, which needs no permission and is harmless if triggered by mistake.
     *
     * [inHelpSheet] puts it behind the help bar as well as on the grid. Opt-in rather than
     * automatic, because the sheet is read under stress and a list of nine numbers is not a list —
     * two or three is (docs/concept.md §4.3). Defaulted to false so a payload written before this
     * field existed still reads.
     */
    @Serializable
    @SerialName("phone")
    data class Phone(
        val displayName: String = "",
        val number: String = "",
        val relation: String? = null,
        val inHelpSheet: Boolean = false,
        /**
         * Everyone on this tile. Empty in a file written before 0.3.0, which had room for one
         * person and stored them in the four fields above.
         */
        val contacts: List<PhoneEntry> = emptyList(),
    ) : CardPayload {

        /**
         * The tile's people, however the file that held them was written.
         *
         * Read this rather than either half. [contacts] is what this version writes; the flat
         * fields are what an older one wrote and what an older one still reads, and `phoneOf`
         * keeps the two in step so both remain true of the same tile.
         */
        val people: List<PhoneEntry>
            get() = contacts.ifEmpty {
                if (number.isBlank() && displayName.isBlank()) {
                    emptyList()
                } else {
                    listOf(PhoneEntry(displayName, number, relation, inHelpSheet))
                }
            }
    }

    /**
     * [label] is what the tile's button says; [caption] is the line above it, for what the page is
     * actually for. "Line 142 from the corner" is the sort of thing that makes a bookmark usable
     * by somebody who did not save it.
     */
    @Serializable
    @SerialName("web")
    data class Web(
        val url: String,
        val label: String? = null,
        val caption: String? = null,
    ) : CardPayload

    /** [caption] is what the app is for, in the family's own words rather than the store's. */
    @Serializable
    @SerialName("appLink")
    data class AppLink(
        val packageName: String,
        val label: String,
        val deepLink: String? = null,
        val caption: String? = null,
    ) : CardPayload

    /** Points at the board holding the tiles inside it. One level deep, by design. */
    @Serializable
    @SerialName("folder")
    data class Folder(
        @Serializable(with = UuidSerializer::class) val boardId: Uuid,
    ) : CardPayload
}

/**
 * One step of a guide: text, optionally a picture, optionally a recording of a parent reading it
 * aloud.
 */
@OptIn(ExperimentalUuidApi::class)
@Serializable
data class Step(
    val text: String,
    @Serializable(with = UuidSerializer::class) val mediaId: Uuid? = null,
    @Serializable(with = UuidSerializer::class) val audioId: Uuid? = null,
)

@Serializable
data class CheckItem(val text: String, val done: Boolean = false)

/**
 * Every media asset this payload refers to.
 *
 * The reference lives inside the payload JSON, so this is the only way to answer "is that file
 * still needed". Cleaning up media by any other route means either deleting a picture a guide
 * still shows, or keeping every file a user ever picked — which is how a backup ends up larger
 * than the app that made it.
 */
@OptIn(ExperimentalUuidApi::class)
val CardPayload.referencedMediaIds: Set<Uuid>
    get() = when (this) {
        is CardPayload.Guide -> steps.flatMapTo(mutableSetOf()) { listOfNotNull(it.mediaId, it.audioId) }
        is CardPayload.Video -> setOf(mediaId)
        is CardPayload.Audio -> setOf(mediaId)
        is CardPayload.Note,
        is CardPayload.Checklist,
        is CardPayload.Table,
        is CardPayload.Phone,
        is CardPayload.Web,
        is CardPayload.AppLink,
        is CardPayload.Folder,
        -> emptySet()
    }

/**
 * One person a call tile can reach.
 *
 * A name of their own rather than only the tile's, because a tile called "Important numbers" holds
 * four people and a caregiver has to be able to tell which is which. [relation] is free text and
 * Larova never interprets it — "Mum", "the practice", "next door" are all the same kind of thing
 * to the app.
 *
 * [inHelpSheet] is per person, not per tile: one tile can hold the doctor, the neighbour and the
 * grandmother while only two of them belong behind the red bar.
 */
@Serializable
data class PhoneEntry(
    val displayName: String = "",
    val number: String = "",
    val relation: String? = null,
    val inHelpSheet: Boolean = false,
)

/** The type a payload serializes as, so no caller has to hardcode a discriminator string. */
val CardPayload.cardType: CardType
    get() = when (this) {
        is CardPayload.Guide -> CardType.GUIDE
        is CardPayload.Note -> CardType.NOTE
        is CardPayload.Checklist -> CardType.CHECKLIST
        is CardPayload.Table -> CardType.TABLE
        is CardPayload.Video -> CardType.VIDEO
        is CardPayload.Audio -> CardType.AUDIO
        is CardPayload.Phone -> CardType.PHONE
        is CardPayload.Web -> CardType.WEB
        is CardPayload.AppLink -> CardType.APP_LINK
        is CardPayload.Folder -> CardType.FOLDER
    }
