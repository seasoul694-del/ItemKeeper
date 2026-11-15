// 顶层插件版本。铁律（v9/v10 真机验证结论）：
// compose-ui 1.12.0 的 aar-metadata 硬性要求 minCompileSdk=37 / minAndroidGradlePluginVersion=9.1.0；
// AGP 必须 9.3.2（9.1.0 构建的 release 在 Dimensity 9500 上仍触发 drawBackdrop native crash）。
plugins {
    id("com.android.application") version "9.3.2" apply false
    // AGP 9.0 起 Kotlin 支持内置，不再允许/需要 org.jetbrains.kotlin.android
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
}
