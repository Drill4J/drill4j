import com.epam.drill.build.*

plugins {
    id("kotlin-multiplatform")
    id("kotlinx-serialization")
}

kotlin {
    targets {
        jvm("drillAgentPart")
        createNativeTargetForCurrentOs("nat")
    }

    sourceSets {
        val commonMain by getting
        commonMain.apply {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationRuntimeVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$jvmCoroutinesVersion")
                implementation("com.epam.drill:drill-common:$drillCommonVersion")
            }
        }
        named("drillAgentPartMain") {
            dependencies {
                implementation("com.epam.drill:drill-common-jvm:$drillCommonVersion")
            }
        }
        named("natMain") {
            dependencies {
                implementation("com.epam.drill:drill-jvmapi-${org.jetbrains.kotlin.konan.target.HostManager.simpleOsName()}x64:$drillUtilsVersion")
                implementation("com.epam.drill:drill-common-${org.jetbrains.kotlin.konan.target.HostManager.simpleOsName()}x64:$drillCommonVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-native:1.2.0")
            }
        }
    }
}