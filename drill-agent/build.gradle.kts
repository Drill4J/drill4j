import com.epam.drill.build.*
import org.apache.tools.ant.taskdefs.condition.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.tasks.*

plugins {
    id("kotlin-multiplatform")
    id("kotlinx-serialization")
}

repositories {
    mavenCentral()
    mavenLocal()
    jcenter()
    maven(url = "https://kotlin.bintray.com/kotlinx")
}

kotlin {
    targets {
        createNativeTargetForCurrentOs("nativeAgent") {
            mainCompilation {
                binaries {
                    sharedLib(
                        namePrefix = "drill-agent",
                        buildTypes = setOf(DEBUG)
                    )
                }
                val drillInternal by cinterops.creating
                drillInternal.apply {
                }
            }
            compilations.getByName("test"){
                val jvmapi by cinterops.creating
                jvmapi.apply {
                    defFile = file("src/nativeInterop/cinterop/testdrillinternal.def")
                }
            }
        }

        jvm("javaAgent")
    }

    sourceSets {
        jvm("javaAgent").compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                implementation(kotlin("reflect")) //TODO jarhell quick fix for kotlin jvm apps
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationRuntimeVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$jvmCoroutinesVersion")
                implementation("com.epam.drill:drill-common-jvm:$drillCommonVersion")
                implementation(project(":drill-plugin-api:drill-agent-part"))
            }
        }
        jvm("javaAgent").compilations["test"].defaultSourceSet {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }

        named("commonMain") {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$serializationRuntimeVersion")
                implementation(project(":drill-plugin-api:drill-agent-part"))
                implementation("com.epam.drill:drill-common:$drillCommonVersion")
            }
        }
        named("commonTest") {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-common")
                implementation("org.jetbrains.kotlin:kotlin-test-annotations-common")
            }
        }

        named("nativeAgentMain") {
            dependencies {
                when {
                    Os.isFamily(Os.FAMILY_MAC) -> implementation("com.soywiz:korio-macosx64:$korioVersion")
                    Os.isFamily(Os.FAMILY_WINDOWS) -> implementation("com.soywiz:korio-mingwx64:$korioVersion")
                    else -> implementation("com.soywiz:korio-linuxx64:$korioVersion")
                }
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-native:$serializationNativeVersion")
                implementation("io.ktor:ktor-utils-native:$ktorVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-io-native:0.1.8")
                implementation(project(":drill-plugin-api:drill-agent-part"))
                implementation("com.epam.drill:drill-jvmapi-${org.jetbrains.kotlin.konan.target.HostManager.simpleOsName()}x64:$drillUtilsVersion")
                implementation("com.epam.drill:drill-common-${org.jetbrains.kotlin.konan.target.HostManager.simpleOsName()}x64:$drillCommonVersion")
                implementation(project(":platformDependent"))
            }
        }
    }

}

tasks.withType<KotlinNativeCompile> {
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlinx.io.core.ExperimentalIoApi"
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlin.ExperimentalUnsignedTypes"
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlinx.coroutines.ExperimentalCoroutinesApi"
}

val staticLibraryName = "${staticLibraryPrefix}drill_jvmapi.$staticLibraryExtension"
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

        dependsOn("linkDrill-agentDebugSharedNativeAgent")
        doFirst {
            delete(file("distr/$staticLibraryName"))
        }
        doLast {
            val binary = (kotlin.targets["nativeAgent"] as KotlinNativeTarget)
                .binaries
                .findSharedLib(
                    "drill-agent",
                    NativeBuildType.DEBUG
                )?.outputFile
            copy {
                from(file("$binary"))
                into(file("../distr"))
            }
        }
    }
    register("buildAgent") {
        dependsOn("metadataJar")
        dependsOn(javaAgentJar)
        dependsOn(deleteAndCopyAgent)
        group = "application"
    }

    "nativeAgentTestProcessResources"(ProcessResources::class) {
        setDestinationDir(file("build/bin/nativeAgent/testDebugExecutable/resources"))
    }


}