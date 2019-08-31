import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

plugins {
    id("kotlin-multiplatform")
}


kotlin {
    targets {
        mingwX64("win")
        linuxX64("linux")
        macosX64("mac")
    }

    sourceSets {
        named("winMain") {
            dependencies {
                implementation("com.epam.drill:jvmapi-windowsx64:$version")
                implementation("com.epam.drill:drill-agent-part-windowsx64:$version")
            }
        }
        named("linuxMain") {
            dependencies {
                implementation("com.epam.drill:jvmapi-linuxx64:$version")
                implementation("com.epam.drill:drill-agent-part-linuxx64:$version")
            }
        }
        named("macMain") {
            dependencies {
                implementation("com.epam.drill:jvmapi-macosx64:$version")
                implementation("com.epam.drill:drill-agent-part-macosx64:$version")
            }
        }

    }
}

tasks.withType<KotlinNativeCompile> {
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlin.ExperimentalUnsignedTypes"
    kotlinOptions.freeCompilerArgs += "-XXLanguage:+InlineClasses"
}