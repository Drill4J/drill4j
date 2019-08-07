import com.epam.drill.build.*

plugins {
    id("kotlin-multiplatform")
    id("kotlinx-serialization")
}

kotlin {
    targets {
        jvm("drillAdminPart")
    }

    sourceSets {
        val commonMain by getting

        commonMain.apply {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$serializationRuntimeVersion")
                implementation("com.epam.drill:drill-common:$drillCommonVersion")
            }
        }
        val drillAdminPartMain by getting
        drillAdminPartMain.apply {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationRuntimeVersion")
                implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
                implementation("com.epam.drill:drill-common-jvm:$drillCommonVersion")
            }
        }
    }
}
