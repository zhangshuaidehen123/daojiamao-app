# 到家保洁App ProGuard规则
-keepattributes Signature
-keepattributes *Annotation*
-keep class kotlinx.serialization.** { *; }
-keep class com.daojia.app.data.api.** { *; }
-keep class com.daojia.app.data.local.** { *; }
-dontwarn kotlinx.serialization.**
