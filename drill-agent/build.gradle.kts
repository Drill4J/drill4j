import com.epam.drill.build.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeOutputKind
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink

plugins {
    id("kotlin-multiplatform")
    id("kotlinx-serialization")
}

repositories {
    mavenCentral()
    mavenLocal()
    jcenter()
    maven(url = "https://dl.bintray.com/soywiz/soywiz")
    maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
    maven(url = "https://kotlin.bintray.com/kotlinx")
    maven(url = "https://mymavenrepo.com/repo/OgSYgOfB6MOBdJw3tWuX/")
}

kotlin {
    targets {
        createNativeTargetForCurrentOs("nativeAgent") {
            mainCompilation {
                outputKinds(DYNAMIC)
                val drillInternal by cinterops.creating
                drillInternal.apply {
                }
            }
        }
        jvm("javaAgent")
    }

    sourceSets {
        jvm("javaAgent").compilations["main"].defaultSourceSet {
            dependencies {
                implementation("com.soywiz:klogger:$kloggerVersion")
                implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationRuntimeVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$jvmCoroutinesVersion")
                implementation(project(":drill-common"))
                implementation(project(":drill-plugin-api:drill-agent-part"))
            }
        }

        val commonMain by getting
        commonMain.apply {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$serializationRuntimeVersion")
                implementation(project(":drill-plugin-api:drill-agent-part"))
                implementation(project(":drill-common"))
            }
        }


        val nativeAgentMain by getting
        nativeAgentMain.apply {
            dependencies {
                implementation("com.soywiz:korio:$korioVersion")
                implementation("com.soywiz:klogger:$kloggerVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-native:$serializationNativeVersion")
                implementation(project(":drill-plugin-api:drill-agent-part"))
                implementation(project(":nativeprojects:drill-kni"))
                implementation(project(":nativeprojects:drill-kasm"))
                implementation(project(":nativeprojects:drill-jvmapi"))
                implementation(project(":drill-common"))
            }
        }
    }
}



tasks {
    val javaAgentJar = "javaAgentJar"(Jar::class) {
        destinationDirectory.set(file("../distr"))
        archiveFileName.set("drillRuntime.jar")
        from(provider {
            kotlin.targets["javaAgent"].compilations["main"].compileDependencyFiles.map {
                if (it.isDirectory) it else zipTree(it)
            }
        })
    }


    val deleteAndCopyAgent by registering {

        dependsOn("linkMainDebugSharedNativeAgent")
        doFirst {
            delete(file("distr/${staticLibraryPrefix}main.$staticLibraryExtension"))
        }
        doLast {
            val binary = (kotlin.targets["nativeAgent"].compilations["main"] as KotlinNativeCompilation).getBinary(
                NativeOutputKind.valueOf("DYNAMIC"),
                NativeBuildType.DEBUG
            )
            copy {
                from(file("$binary"))
                into(file("../distr"))
            }
            copy {
                from(file("$binary"))
                into(file("../plugins/drill-exception-plugin-native/drill-api/$preset"))
            }
        }
    }
    register("buildAgent") {
        dependsOn("metadataJar")
        dependsOn(javaAgentJar)
        dependsOn(deleteAndCopyAgent)
        group = "application"

        doLast {
            if (!File("../distr/configs", "drillConfig.json").exists()) {
                copy {
                    from(file("../resources/drillConfig.json"))
                    into(file("../distr/configs"))
                }
            }
            if (!File("../distr/configs", "logger.properties").exists()) {
                copy {
                    from(file("../resources/logger.properties"))
                    into(file("../distr/configs"))
                }
            }
        }
    }

    "linkTestDebugExecutableNativeAgent"(KotlinNativeLink::class) {
        binary.linkerOpts.add("subdep/${staticLibraryPrefix}main.$staticLibraryExtension")
//        binary.linkerOpts.add("subdep/main.dll")
    }


}