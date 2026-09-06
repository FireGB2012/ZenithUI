# Zenith UI keeps no reflection-based entry points beyond the Android framework's own,
# so the default Android + R8 rules cover the app. Compose ships its own rules via AARs.

# Keep the launcher entry point explicit for anyone reading the mapping file.
-keep class com.gabriel.zenithui.MainActivity { *; }

# Line numbers make Play Console crash reports readable after obfuscation.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
