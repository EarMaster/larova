package app.larova.core.domain.model

/**
 * What the user chose in settings, including "follow the phone", which is the default.
 *
 * Distinct from the resolved appearance mode in `:core:ui`: only the resolved mode reaches the
 * colour tables, so night can never be arrived at by accident. It is a deliberate choice for
 * reading a guide aloud in a darkened room, not a system state.
 *
 * [key] is what gets persisted, so these strings are stable.
 */
enum class AppearanceSetting(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
    NIGHT("night"),
    ;

    companion object {
        val DEFAULT = SYSTEM

        /** An unreadable or unknown stored value falls back rather than failing to start. */
        fun fromKey(key: String?): AppearanceSetting =
            entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}
