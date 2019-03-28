import com.epam.drill.build.createNativeTargetForCurrentOs
import com.epam.drill.build.jvmPaths
import com.epam.drill.build.mainCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeOutputKind

plugins {
    id("kotlin-multiplatform")
    id("kotlinx-serialization")
}

repositories {
    maven(url = "https://dl.bintray.com/soywiz/soywiz")
    maven(url = "https://mymavenrepo.com/repo/OgSYgOfB6MOBdJw3tWuX/")
}

kotlin {
    targets {
        createNativeTargetForCurrentOs("jvmapi") {
            mainCompilation {
                outputKinds(NativeOutputKind.DYNAMIC)
                val jvmapi by cinterops.creating
                jvmapi.apply {
                    includeDirs(jvmPaths, "./src/nativeInterop/cpp")
                }
            }
        }
    }

    sourceSets {
        val jvmapiMain by getting
        jvmapiMain.apply {
            dependencies {
                implementation(project(":nativeprojects:drill-logger"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-native:0.10.0")
            }
        }
    }
}
