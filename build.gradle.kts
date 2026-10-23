// Плагины подключаются в модуле app; здесь только объявляются версии.
// kotlin-android объявлен ради версии Kotlin: AGP 9 компилирует Kotlin сам («built-in Kotlin»),
// а этот плагин лишь задаёт версию компилятора на classpath сборки.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
