import com.epam.drill.build.createNativeTargetForCurrentOs
import com.epam.drill.build.mainCompilation
import com.epam.drill.build.serializationRuntimeVersion

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
                implementation(project(":drill-common"))
            }
        }
        named("natMain") {
            dependencies {
                implementation(project(":nativeprojects:drill-jvmapi"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-native:1.2.0")
            }
        }
    }
}

//tasks.withType<KotlinCompile>().all {
//    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlinx.serialization.UnstableDefault"
//}
