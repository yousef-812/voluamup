# Keep Telecom and InCallService classes
-keep class com.example.volumeup.service.CustomInCallService { *; }
-keep class com.example.volumeup.audio.AudioBoosterManager { *; }

# Keep Material & ViewBinding
-keep class com.google.android.material.** { *; }
-keepclassmembers class * implements androidx.viewbinding.ViewBinding {
    public static *** bind(android.view.View);
    public static *** inflate(...);
}
