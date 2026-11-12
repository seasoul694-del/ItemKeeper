import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

/*
 * 签名配置：只从「仓库外的 keystore.properties」或「环境变量」读取。
 * 任何私钥、口令都不要写进本文件，也不要提交到 GitHub。
 * 本地示例见 README；CI 由 workflow 临时生成 keystore.properties。
 */
val keystoreProps = Properties()
val keystorePropsFile = rootProject.file("keystore.properties")
if (keystorePropsFile.exists()) {
    FileInputStream(keystorePropsFile).use { keystoreProps.load(it) }
}

fun signingProp(key: String): String? = keystoreProps.getProperty(key) ?: System.getenv(key)

android {
    namespace = "com.tang.itemkeeper"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.tang.itemkeeper"
        minSdk = 26
        targetSdk = 34
        // 每次发版务必递增 versionCode，否则手机无法覆盖安装升级
        versionCode = 2
        versionName = "1.1.0"
    }

    signingConfigs {
        create("release") {
            val path = signingProp("KEYSTORE_FILE")
            if (!path.isNullOrBlank()) {
                storeFile = rootProject.file(path)
                storePassword = signingProp("KEYSTORE_PASSWORD")
                keyAlias = signingProp("KEY_ALIAS")
                keyPassword = signingProp("KEY_PASSWORD")
            }
        }
    }

    val releaseSigning = signingConfigs.getByName("release")

    buildTypes {
        release {
            // 个人分发场景优先保证构建稳定；需要瘦身时再改为 true 并补充 keep 规则
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (releaseSigning.storeFile != null) {
                signingConfig = releaseSigning
            }
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
    }
    composeOptions {
        // Compose Compiler 必须与 Kotlin 版本匹配：1.9.22 -> 1.5.10
        kotlinCompilerExtensionVersion = "1.5.10"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)

    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
