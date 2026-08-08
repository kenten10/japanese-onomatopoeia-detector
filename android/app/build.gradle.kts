plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

/** 辞書は iOS 版の Resources を単一の正とし、ビルド時に assets へ取り込む。 */
val copyDictionary = tasks.register<Copy>("copyDictionary") {
    from(rootProject.file("../OnomatopoeiaDetector/Resources/onomatopoeia_dict.json"))
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
        resourceConfigurations += listOf("en", "ja")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
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
                // 辞書は iOS 版と共有しているものをそのまま読む
                test.systemProperty(
                    "dictionary.path",
                    rootProject.file("../OnomatopoeiaDetector/Resources/onomatopoeia_dict.json").absolutePath
                )
            }
        }
    }

    // コピータスクをそのまま渡すことで、assets を読むタスクへ依存が伝わる
    sourceSets["main"].assets.srcDir(copyDictionary)
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

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // 形態素解析（読み推定・品詞判定）。Web 版の kuromoji と同じ IPADIC を使う。
    implementation("com.atilika.kuromoji:kuromoji-ipadic:0.9.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
