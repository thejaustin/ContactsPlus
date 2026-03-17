# ez-vcard
-keep,includedescriptorclasses class ezvcard.property.** { *; }
-keep enum ezvcard.VCardVersion { *; }
-dontwarn ezvcard.io.json.**
-dontwarn freemarker.**

# Sentry
-keep class io.sentry.** { *; }
-keepclassmembers class io.sentry.** { *; }
-dontwarn io.sentry.**
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
