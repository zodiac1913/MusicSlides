import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.9.22"
    id("org.jetbrains.compose") version "1.6.0"
    kotlin("plugin.serialization") version "1.9.22"
}

group = "com.lyricslides"
version = "2026.05.10"

dependencies {
    // Compose Desktop
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // Networking - Ktor
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-cio:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")

    // Apache POI for PPTX generation
    implementation("org.apache.poi:poi-ooxml:5.2.5")

    // Image processing
    implementation("net.coobird:thumbnailator:0.4.20")
}

compose.desktop {
    application {
        mainClass = "com.lyricslides.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "LyricSlides"
            packageVersion = "1.0.0"
        }
    }
}
