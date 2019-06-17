import com.epam.drill.build.createNativeTargetForCurrentOs
import com.epam.drill.build.jvmCoroutinesVersion
import com.epam.drill.build.korioVersion
import com.epam.drill.build.mainCompilation
import com.epam.drill.build.preset
import com.epam.drill.build.serializationNativeVersion
import com.epam.drill.build.serializationRuntimeVersion
import com.epam.drill.build.staticLibraryExtension
import com.epam.drill.build.staticLibraryPrefix
import org.apache.tools.ant.taskdefs.condition.Os
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink

plugins {
    id("kotlin-multiplatform")
    id("kotlinx-serialization")
}

repositories {
    mavenCentral()
    mavenLocal()
    jcenter()
    maven(url = "https://kotlin.bintray.com/kotlinx")
    maven(url = "https://mymavenrepo.com/repo/OgSYgOfB6MOBdJw3tWuX/")
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
        }

        when {
            Os.isFamily(Os.FAMILY_UNIX) -> linuxX64("linuxPart")
            Os.isFamily(Os.FAMILY_WINDOWS) -> mingwX64("windowsPart")
            else -> throw RuntimeException("is not supported yet")
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
                implementation(project(":drill-common"))
                implementation(project(":drill-plugin-api:drill-agent-part"))
            }
        }
        jvm("javaAgent").compilations["test"].defaultSourceSet {
            dependencies {
                implementation(kotlin("test-junit"))
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
                implementation(project(":drill-common"))
            }
        }
        named("commonTest") {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-common")
                implementation("org.jetbrains.kotlin:kotlin-test-annotations-common")
            }
        }

        when {
            Os.isFamily(Os.FAMILY_UNIX) -> {
                @Suppress("UNUSED_VARIABLE") val nativeAgentMain by getting {
                    dependsOn(getByName("linuxPartMain"))
                }
            }
            Os.isFamily(Os.FAMILY_WINDOWS) -> {
                @Suppress("UNUSED_VARIABLE") val nativeAgentMain by getting {
                    dependsOn(getByName("windowsPartMain"))
                }
            }
            else -> throw RuntimeException("is not supported yet")
        }

        named("nativeAgentMain") {
            dependencies {
                when {
                    Os.isFamily(Os.FAMILY_MAC) -> implementation("com.soywiz:korio-macosx64:$korioVersion")
                    Os.isFamily(Os.FAMILY_WINDOWS) -> implementation("com.soywiz:korio-mingwx64:$korioVersion")
                    else -> implementation("com.soywiz:korio-linuxx64:$korioVersion")
                }
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-native:$serializationNativeVersion")
                implementation(project(":drill-plugin-api:drill-agent-part"))
                implementation(project(":nativeprojects:drill-kni"))
                implementation(project(":nativeprojects:drill-jvmapi"))
                implementation(project(":drill-common"))
            }
        }
    }

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

    "linkTestDebugExecutableNativeAgent"(KotlinNativeLink::class) {
        doFirst {
            copy {
                println(file("subdep/$staticLibraryName").exists())
                binary.linkerOpts.add("subdep/$staticLibraryName")
                from(staticLibraryName)
                into(file("build/bin/nativeAgent/testDebugExecutable"))
            }
        }
    }

    "nativeAgentTestProcessResources"(ProcessResources::class) {
        setDestinationDir(file("build/bin/nativeAgent/testDebugExecutable/resources"))
    }


}