# Keep metadata required by Retrofit, Gson, and annotation-driven libraries.
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# Retrofit service interfaces and generic method signatures.
-keep,allowobfuscation interface com.casper.myguide.connection.API { *; }
-keep,allowobfuscation interface com.casper.myguide.service.** { *; }

# Gson serializes these API payload models reflectively.
-keepclassmembers class com.casper.myguide.model.** {
    <fields>;
}
-keep class com.casper.myguide.model.** { *; }

# ObjectBox entities and generated model metadata.
-keep class com.casper.myguide.weathermodel.db.** { *; }
-keep class com.casper.myguide.weathermodel.db.MyObjectBox { *; }
-keep class io.objectbox.** { *; }

# Firebase services are instantiated by Android/Firebase infrastructure.
-keep class com.casper.myguide.fcm.** extends com.google.firebase.messaging.FirebaseMessagingService { *; }
-keep class * extends com.google.firebase.messaging.FirebaseMessagingService { *; }

# Glide discovers generated modules and model loaders reflectively.
-keep class com.bumptech.glide.GeneratedAppGlideModule { *; }
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
