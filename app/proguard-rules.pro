# NextBus R8 rules.
#
# Most libraries ship their own consumer rules (Room, Hilt, DataStore, kotlinx.serialization,
# Navigation, Maps Compose), so very little is needed here.

# Keep source file and line numbers so release stack traces stay readable;
# rename the source file attribute so file names are not exposed.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Enums persisted by name (Room columns, DataStore, navigation arguments) are restored with valueOf().
-keepclassmembers enum com.ncs.nextbus.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
