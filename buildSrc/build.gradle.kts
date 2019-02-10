plugins {
    id("org.gradle.kotlin.kotlin-dsl") version "1.1.3"
}

repositories {
    jcenter()
}
dependencies{
    compileOnly(gradleKotlinDsl())

    implementation(kotlin("gradle-plugin"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.squareup:kotlinpoet:1.0.0")
    implementation("org.apache.bcel:bcel:6.0")
    implementation(kotlin("reflect"))
}
