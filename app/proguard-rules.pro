# Room rules
-keepclassmembers class * extends androidx.room.RoomDatabase {
    static <fields>;
}
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep data entities and embedded relations
-keep class com.homeinventory.data.** { *; }
