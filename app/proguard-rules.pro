# Gson 通用类型（ApiResponse<T> 等）反射序列化
-keep class com.bilimusic.app.**_02_model.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# bilibili CDN 域名证书链不规范时放宽（可选）
-dontwarn javax.naming.**
