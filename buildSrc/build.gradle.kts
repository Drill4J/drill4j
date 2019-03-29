plugins {
    `kotlin-dsl`
}

repositories {
    jcenter()
}
val kotlinVersion = "1.3.21"
dependencies {
    implementation(kotlin("gradle-plugin", kotlinVersion))
    implementation(kotlin("stdlib-jdk8", kotlinVersion))
    implementation(kotlin("serialization", kotlinVersion))
    implementation(kotlin("reflect", kotlinVersion))
}
