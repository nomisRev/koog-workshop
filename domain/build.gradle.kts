plugins {
    alias(libs.plugins.kotlinJvm)
}

dependencies {
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.sqlite.jdbc)
    testImplementation(libs.kotlin.test)
}