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

    //Generating kotlin native classes by java bytecode. TODO extract to a separate plugin
    implementation("com.squareup:kotlinpoet:1.0.0")
    implementation("org.apache.bcel:bcel:6.3.1")
}
