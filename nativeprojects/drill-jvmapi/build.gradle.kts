import com.epam.drill.build.*

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
                    defFile = file("src/nativeInterop/cinterop/jvmapi.def")
                    includeDirs(jvmPaths, "./src/nativeInterop/cpp", "./")
                }
            }
        }
    }

    sourceSets {
        val jvmapiMain by getting
        jvmapiMain.apply {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-native:$serializationNativeVersion")
            }
        }
    }
    sourceSets.all {
        languageSettings.useExperimentalAnnotation("kotlin.ExperimentalUnsignedTypes")
    }
}