plugins {
    `kotlin-dsl`
}

repositories {
    maven(url = "https://dl.bintray.com/kotlin/kotlinx/")
    jcenter()
}

val kotlinVersion = "1.3.50"
val atomicFuVersion = "0.12.6"
dependencies {
    implementation(kotlin("gradle-plugin", kotlinVersion))
    implementation(kotlin("stdlib-jdk8", kotlinVersion))
    implementation(kotlin("serialization", kotlinVersion))
    implementation(kotlin("reflect", kotlinVersion))
    implementation("org.jetbrains.kotlinx:atomicfu-gradle-plugin:$atomicFuVersion")
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}