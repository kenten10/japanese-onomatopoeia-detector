import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

/** 辞書は shared/ を単一の正とし、ビルド時に assets へ取り込む。 */
val copyDictionary = tasks.register<Copy>("copyDictionary") {
    from(rootProject.file("../shared/onomatopoeia_dict.json"))
    into(layout.buildDirectory.dir("generated/dictionaryAssets"))
}

android {
    namespace = "com.kensukeyoshida.onomatopoeiadetector"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kensukeyoshida.onomatopoeiadetector"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 不具合の送信先。設定しなければ報告は行わない。
        buildConfigField("String", "SENTRY_DSN", "\"${System.getenv("SENTRY_DSN") ?: ""}\"")

        // ご意見フォーム。3 実装で同じファイルを見る。空なら導線を出さない。
        val feedbackFormUrl = rootProject.file("../shared/feedback-form-url.txt")
            .takeIf { it.exists() }?.readText()?.trim().orEmpty()
        buildConfigField("String", "FEEDBACK_FORM_URL", "\"$feedbackFormUrl\"")
    }

    signingConfigs {
        create("release") {
            // 鍵はリポジトリに置かず、環境変数から受け取る。
            // 未設定のときは署名なしで組み上がる（CI のビルド確認はこの経路）。
            val keystore = System.getenv("ANDROID_KEYSTORE_PATH")
            if (keystore != null) {
                storeFile = file(keystore)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (System.getenv("ANDROID_KEYSTORE_PATH") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    bundle {
        // 表示言語はアプリ内で切り替えるため、言語ごとの分割はしない。
        // 分割すると端末の言語ぶんしか配信されず、切り替え先の文言が無くなる。
        language { enableSplit = false }
    }

    androidResources {
        // 同梱する言語。ここを絞らないと依存が持ち込む多数の言語まで抱え込む
        localeFilters += listOf("en", "ja")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
        // kuromoji-core と kuromoji-ipadic が同名で同梱している告知類。
        // Apache-2.0 と IPADIC は再頒布時の表示を求めるため、除外せず結合して APK に残す。
        resources.merges += setOf(
            "META-INF/LICENSE.md",
            "META-INF/NOTICE.md",
            "META-INF/CONTRIBUTORS.md"
        )
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            all { test ->
                // 3 プラットフォームで共有している辞書をそのまま読む
                test.systemProperty(
                    "dictionary.path",
                    rootProject.file("../shared/onomatopoeia_dict.json").absolutePath
                )
            }
        }
    }

    // コピータスクをそのまま渡すことで、assets を読むタスクへ依存が伝わる
    sourceSets["main"].assets.srcDir(copyDictionary)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    ksp("androidx.room:room-compiler:2.7.2")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    // 形態素解析（読み推定・品詞判定）。Web 版の kuromoji と同じ IPADIC を使う。
    implementation("com.atilika.kuromoji:kuromoji-ipadic:0.9.0")

    // 不具合の報告
    implementation("io.sentry:sentry-android:8.52.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
