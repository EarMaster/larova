# Larova · R8 keep rules.
#
# kotlinx.serialization keeps its generated serializers through reflection on the companion, so
# the payload types must survive shrinking — an export written by a release build that a debug
# build cannot read would be the worst kind of regression (docs/technical-notes.md §9).
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class app.larova.** {
    *** Companion;
}
-keepclasseswithmembers class app.larova.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class app.larova.**$$serializer { *; }
