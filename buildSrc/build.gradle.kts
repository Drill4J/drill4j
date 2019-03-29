plugins {
    id("org.gradle.kotlin.kotlin-dsl") version "1.1.3"
}

repositories {
    jcenter()
}
val kotlinVersion = "1.3.21"
dependencies {
    compileOnly(gradleKotlinDsl())
    implementation(kotlin("gradle-plugin", kotlinVersion))
    implementation(kotlin("stdlib-jdk8", kotlinVersion))
    implementation(kotlin("serialization", kotlinVersion))
    implementation("com.squareup:kotlinpoet:1.0.0")
    implementation("org.apache.bcel:bcel:6.0")
    implementation(kotlin("reflect", kotlinVersion))
}
