plugins {
    alias(libs.plugins.kotlinJvm)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll("-Xcontext-parameters")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }
}

dependencies {
    implementation(libs.compose.runtime)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.kotlinx.datetime)
    implementation(libs.sqlite.jdbc)
    testImplementation(libs.kotlin.test)
}