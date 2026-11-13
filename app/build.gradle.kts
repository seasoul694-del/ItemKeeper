import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

/*
 * 签名资产沿用 v1：release.jks 与 keystore.properties 已复制到本工程，
 * applicationId 保持 com.tang.itemkeeper，可直接覆盖安装 v1.x。
 */
val keystoreProps = Properties()
val keystorePropsFile = rootProject.file("keystore.properties")
if (keystorePropsFile.exists()) {
    FileInputStream(keystorePropsFile).use { keystoreProps.load(it) }
}

android {
    namespace = "com.tang.itemkeeper"
    // compose-ui 1.12.0 aar-metadata 硬性要求 minCompileSdk=37
    // AGP 9.3 可直接解析 minor 平台目录（android-37.0），与官方 Catalog 一致用整数
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "com.tang.itemkeeper"
        minSdk = 26
        // 对齐官方 Catalog（targetSdk 37）
        targetSdk = 37
        // v1.2.0 = 3；preview.7 = 10。每次发版务必递增，否则手机拒绝覆盖安装
        versionCode = 25
        versionName = "2.0.2"
    }

    signingConfigs {
        create("release") {
            val path = keystoreProps.getProperty("KEYSTORE_FILE")
            if (!path.isNullOrBlank()) {
                storeFile = rootProject.file(path)
                storePassword = keystoreProps.getProperty("KEYSTORE_PASSWORD")
                keyAlias = keystoreProps.getProperty("KEY_ALIAS")
                keyPassword = keystoreProps.getProperty("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // 铁律：R8 + 资源收缩（debug 构建在该机型触发 native crash，与代码无关）
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    packaging {
        dex {
            useLegacyPackaging = true
        }
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui:1.12.0")
    implementation("androidx.compose.ui:ui-graphics:1.12.0")
    implementation("androidx.compose.foundation:foundation:1.12.0")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // 液态玻璃：backdrop 传递 org.jetbrains.compose.*:1.12.0，其 android 变体对齐 androidx.compose 1.12.0
    implementation("io.github.kyant0:backdrop:2.0.1")
    implementation("io.github.kyant0:shapes:1.2.1")
}
