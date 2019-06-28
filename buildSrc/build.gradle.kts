plugins {
    `kotlin-dsl`
}

repositories {
    jcenter()
}
val kotlinVersion = "1.3.30"
dependencies {
    implementation(kotlin("gradle-plugin", kotlinVersion))
    implementation(kotlin("stdlib-jdk8", kotlinVersion))
    implementation(kotlin("serialization", kotlinVersion))
    implementation(kotlin("reflect", kotlinVersion))
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}