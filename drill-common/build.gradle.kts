import com.epam.drill.build.*

plugins {
    id("kotlin-multiplatform")
    id("kotlinx-serialization")
}

kotlin {
    targets {
        createNativeTargetForCurrentOs("native")
        jvm()
    }

    sourceSets {
        jvm().compilations["main"].defaultSourceSet {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationRuntimeVersion")
            }
        }

        val commonMain by getting
        commonMain.apply {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$serializationRuntimeVersion")
            }
        }


        val nativeMain by getting
        nativeMain.apply {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-native:$serializationNativeVersion")
            }
        }


    }
}
