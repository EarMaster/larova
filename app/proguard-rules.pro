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

# Google Play Billing references com.google.android.datatransport, which is deliberately excluded
# in core/billing/build.gradle.kts: it declares INTERNET and ACCESS_NETWORK_STATE and its job is
# uploading telemetry to Google, which AGENTS.md invariant 6 rules out. R8 is told not to warn
# about the resulting dangling references.
#
# This is safe rather than hopeful, and it was checked in the bytecode the same way media3's
# NetworkTypeObserver was (see the manifest comment). billingclient's zzdt constructor wraps its
# whole body in `catch any` — exception table `from 4 to 42 target 43 type any` — and on failure
# sets a disabled flag. Its logging method then returns early with the string "Skipping logging
# since initialization failed." NoClassDefFoundError is a Throwable, so it is caught: the library
# is built to run without this backend, and the only thing lost is Google's own analytics.
#
# Re-check this after every billing bump. If a future version moves the construction outside the
# try, the symptom is a crash the moment BillingClient is built, and the answer is the manifest
# fallback in docs/technical-notes.md §7 rather than deleting the exclusion.
-dontwarn com.google.android.datatransport.**
