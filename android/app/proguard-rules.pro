# kuromoji は辞書リソースをクラスパスから読み込むため、クラス名とリソースを保持する。
-keep class com.atilika.kuromoji.** { *; }
-keepclassmembers class com.atilika.kuromoji.** { *; }
-dontwarn com.atilika.kuromoji.**

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.kensukeyoshida.onomatopoeiadetector.** {
    *** Companion;
}
-keepclasseswithmembers class com.kensukeyoshida.onomatopoeiadetector.** {
    kotlinx.serialization.KSerializer serializer(...);
}
