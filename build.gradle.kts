import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.0"
    kotlin("jupyter.api") version "0.11.0-327"
}

group = "me.semoro"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:dataframe:0.10.0-dev-1497")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)

}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xrender-internal-diagnostic-names"
        freeCompilerArgs += "-Xcontext-receivers"
    }
}