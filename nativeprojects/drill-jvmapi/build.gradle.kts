import com.epam.drill.build.createNativeTargetForCurrentOs
import com.epam.drill.build.jvmPaths
import com.epam.drill.build.mainCompilation
import com.epam.drill.build.serializationNativeVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
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
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-native:$serializationNativeVersion")
            }
        }
    }
}

tasks {
    "copyCinteropJvmapiJvmapi"{
        dependsOn("linkMainDebugSharedJvmapi")
        doFirst {

            val binary = (kotlin.targets["jvmapi"].compilations["main"] as KotlinNativeCompilation).getBinary(
                NativeOutputKind.valueOf("DYNAMIC"),
                NativeBuildType.DEBUG
            )
            copy {
                from(binary)
                into(rootProject.file("drill-agent/subdep"))
            }
        }
    }
}