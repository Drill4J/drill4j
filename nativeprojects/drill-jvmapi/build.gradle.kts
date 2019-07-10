import com.epam.drill.build.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*

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
                binaries {
                    sharedLib(
                        namePrefix = "drill-jvmapi",
                        buildTypes = setOf(DEBUG)
                    )
                }
                val jvmapi by cinterops.creating
                jvmapi.apply {
                    includeDirs(jvmPaths, "./src/nativeInterop/cpp", "./")
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
    sourceSets.all {
        languageSettings.useExperimentalAnnotation("kotlin.ExperimentalUnsignedTypes")
    }
}

tasks {
    "copyCinteropJvmapiJvmapi"{
        dependsOn("linkDrill-jvmapiDebugSharedJvmapi")
        doFirst {
            arrayOf(rootProject.file("drill-agent"), rootProject.file("drill-agent/subdep")).forEach {
                copy {
                    from(
                        (kotlin.targets["jvmapi"] as KotlinNativeTarget)
                            .binaries
                            .findSharedLib(
                                "drill-jvmapi",
                                NativeBuildType.DEBUG
                            )?.outputFile
                    )
                    into(it)
                }
            }
        }


    }
}