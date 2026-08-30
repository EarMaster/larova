package app.larova.core.ui.icon

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The symbols a tile can carry.
 *
 * [key] is what `Card.icon` stores and what every export file contains, so these strings are
 * frozen exactly as the colour tokens are. Adding a key later is safe; renaming or removing one
 * means migrating other people's data, including data that exists only in a backup nobody can
 * reach.
 *
 * The drawing carries no such promise, and the sixty-eight here are proof of it: the ten keys that
 * shipped first were hand-drawn and are now Lucide drawings under the same keys. A key can be
 * redrawn a hundred times and the tiles using it keep meaning what they meant — which is the whole
 * reason the key is a string and not a picture. See `core/ui/icons/README.md`.
 *
 * **[label] is English and is not translated.** That is a deliberate exception to the rule that no
 * user-facing string is hardcoded, taken by the maintainer: sixty-eight nouns in fourteen
 * languages is a translation surface as large as the rest of the app put together, for words a
 * parent is reading *next to the picture they describe*. Somebody looking for a car finds it by
 * looking at it; the name is there to be searched and to be read aloud by a screen reader. Do not
 * "fix" this by moving it into `strings.xml` — it was considered.
 */
enum class TileSymbol(
    val key: String,
    val label: String,
    val group: SymbolGroup,
    private val drawingOrNull: String? = null,
) {
    // ---- the ten that shipped first. Their keys can never change. ----
    MOON("moon", "Moon", SymbolGroup.EVERYDAY),
    SUN("sun", "Sun", SymbolGroup.EVERYDAY),
    HEART("heart", "Heart", SymbolGroup.PEOPLE),
    LIST("list", "List", SymbolGroup.NOTES),
    NOTE("note", "Note", SymbolGroup.NOTES, drawingOrNull = "notebook-pen"),
    PHONE("phone", "Phone", SymbolGroup.PEOPLE),
    CLOCK("clock", "Clock", SymbolGroup.EVERYDAY),
    HOME("home", "Home", SymbolGroup.HOME, drawingOrNull = "house"),
    MEAL("meal", "Meal", SymbolGroup.FOOD, drawingOrNull = "utensils"),
    STAR("star", "Star", SymbolGroup.NOTES),

    // ---- everyday ----
    BED("bed", "Bed", SymbolGroup.EVERYDAY),
    BATH("bath", "Bath", SymbolGroup.EVERYDAY),
    SUNRISE("sunrise", "Sunrise", SymbolGroup.EVERYDAY),
    SUNSET("sunset", "Sunset", SymbolGroup.EVERYDAY),
    ALARM("alarm", "Alarm", SymbolGroup.EVERYDAY, drawingOrNull = "alarm-clock"),
    BRUSH("brush", "Brush", SymbolGroup.EVERYDAY),

    // ---- food and drink ----
    CUP("cup", "Cup", SymbolGroup.FOOD, drawingOrNull = "cup-soda"),
    WATER("water", "Water", SymbolGroup.FOOD, drawingOrNull = "glass-water"),
    APPLE("apple", "Apple", SymbolGroup.FOOD),
    MILK("milk", "Milk", SymbolGroup.FOOD),
    COOKIE("cookie", "Biscuit", SymbolGroup.FOOD),
    CARROT("carrot", "Carrot", SymbolGroup.FOOD),
    SANDWICH("sandwich", "Sandwich", SymbolGroup.FOOD),
    CAKE("cake", "Cake", SymbolGroup.FOOD),

    // ---- care ----
    PILL("pill", "Pill", SymbolGroup.CARE),
    THERMOMETER("thermometer", "Thermometer", SymbolGroup.CARE),
    PLASTER("plaster", "Plaster", SymbolGroup.CARE, drawingOrNull = "bandage"),
    STETHOSCOPE("stethoscope", "Stethoscope", SymbolGroup.CARE),
    PULSE("pulse", "Heartbeat", SymbolGroup.CARE, drawingOrNull = "heart-pulse"),
    SYRINGE("syringe", "Syringe", SymbolGroup.CARE),

    // ---- home ----
    KEY("key", "Key", SymbolGroup.HOME, drawingOrNull = "key-round"),
    SHIRT("shirt", "Shirt", SymbolGroup.HOME),
    SHOE("shoe", "Shoes", SymbolGroup.HOME, drawingOrNull = "footprints"),
    WASHING("washing", "Washing", SymbolGroup.HOME, drawingOrNull = "washing-machine"),
    BIN("bin", "Bin", SymbolGroup.HOME, drawingOrNull = "trash-2"),
    LAMP("lamp", "Lamp", SymbolGroup.HOME),
    DOOR("door", "Door", SymbolGroup.HOME, drawingOrNull = "door-open"),
    BAG("bag", "Bag", SymbolGroup.HOME, drawingOrNull = "backpack"),

    // ---- out and about ----
    CAR("car", "Car", SymbolGroup.OUT),
    BUS("bus", "Bus", SymbolGroup.OUT),
    BIKE("bike", "Bike", SymbolGroup.OUT),
    TRAIN("train", "Train", SymbolGroup.OUT, drawingOrNull = "train-front"),
    TREE("tree", "Tree", SymbolGroup.OUT, drawingOrNull = "tree-pine"),
    UMBRELLA("umbrella", "Umbrella", SymbolGroup.OUT),
    MAP("map", "Place", SymbolGroup.OUT, drawingOrNull = "map-pin"),
    SCHOOL("school", "School", SymbolGroup.OUT),

    // ---- play ----
    BOOK("book", "Book", SymbolGroup.PLAY, drawingOrNull = "book-open"),
    MUSIC("music", "Music", SymbolGroup.PLAY),
    PAINT("paint", "Painting", SymbolGroup.PLAY, drawingOrNull = "palette"),
    PUZZLE("puzzle", "Puzzle", SymbolGroup.PLAY),
    GAME("game", "Game", SymbolGroup.PLAY, drawingOrNull = "gamepad-2"),
    BLOCKS("blocks", "Blocks", SymbolGroup.PLAY),
    SCISSORS("scissors", "Scissors", SymbolGroup.PLAY),
    BALL("ball", "Ball", SymbolGroup.PLAY, drawingOrNull = "volleyball"),

    // ---- people ----
    PEOPLE("people", "People", SymbolGroup.PEOPLE, drawingOrNull = "users"),
    SMILE("smile", "Happy", SymbolGroup.PEOPLE),
    SAD("sad", "Sad", SymbolGroup.PEOPLE),
    HAND("hand", "Hand", SymbolGroup.PEOPLE),
    BABY("baby", "Baby", SymbolGroup.PEOPLE),
    DOG("dog", "Dog", SymbolGroup.PEOPLE),
    CAT("cat", "Cat", SymbolGroup.PEOPLE),
    RABBIT("rabbit", "Rabbit", SymbolGroup.PEOPLE),
    MESSAGE("message", "Message", SymbolGroup.PEOPLE, drawingOrNull = "message-circle"),

    // ---- notes ----
    CALENDAR("calendar", "Calendar", SymbolGroup.NOTES),
    CAMERA("camera", "Camera", SymbolGroup.NOTES),
    DONE("done", "Done", SymbolGroup.NOTES, drawingOrNull = "circle-check"),
    WARNING("warning", "Warning", SymbolGroup.NOTES, drawingOrNull = "triangle-alert"),
    PIN("pin", "Pin", SymbolGroup.NOTES),
    ;

    /**
     * The file under `core/ui/icons/` this is drawn from, which is usually the key and sometimes
     * not. `meal` is drawn by `utensils`, `bin` by `trash-2`: the key was ours to choose and is
     * frozen, while the drawing is upstream's to name and can be swapped.
     */
    val drawing: String get() = drawingOrNull ?: key

    companion object {
        val DEFAULT = STAR

        /**
         * Unknown keys resolve to the default rather than failing, which is what lets a tile
         * written by a newer version still draw. The tile keeps its stored key: the fallback is a
         * rendering decision, not an edit.
         */
        fun fromKey(key: String?): TileSymbol = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

/**
 * The shelves of the picker.
 *
 * Sixty-eight symbols in one flat grid is a wall somebody scrolls past rather than reads. The
 * groups are what a family's tiles are actually about — the categories come from the six templates
 * and from `docs/concept.md` §4.1, not from how an icon set happens to file its drawings.
 *
 * [label] is English for the same reason [TileSymbol.label] is.
 */
enum class SymbolGroup(val label: String) {
    EVERYDAY("Everyday"),
    FOOD("Food and drink"),
    CARE("Care"),
    HOME("Home"),
    OUT("Out and about"),
    PLAY("Play"),
    PEOPLE("People"),
    NOTES("Notes"),
}

/**
 * The vector for this symbol, drawn from `core/ui/icons/`.
 *
 * Falls back to the default's drawing and then to an empty mark, so a missing SVG is a tile with a
 * plain symbol rather than a screen that will not compose. A family's board must survive an icon
 * going astray.
 */
val TileSymbol.image: ImageVector get() = symbolImage(key)
