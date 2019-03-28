import com.epam.drill.build.createNativeTargetForCurrentOs

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
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.9.1")
            }
        }

        val commonMain by getting
        commonMain.apply {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:0.9.1")
            }
        }


        val nativeMain by getting
        nativeMain.apply {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-native:0.10.0")
            }
        }


    }
}
