plugins {
  alias(libs.plugins.kotlin.jvm)
}

// read the version from the gradle.properties file
val versionName: String by project
val groupName: String by project

group = groupName
version = versionName

dependencies {
  implementation(project(":common"))
  implementation(libs.ch.qos.logback.classic)
  implementation(libs.io.exoquery.pprint)
  implementation(libs.io.ktor.client.core)
  implementation(libs.org.jetbrains.kotlinx.coroutines.core)
  implementation(libs.org.jetbrains.kotlinx.datetime)
  implementation(libs.io.lindstrom.m3u8.parser)
  testImplementation(libs.bundles.test.jvm)
}