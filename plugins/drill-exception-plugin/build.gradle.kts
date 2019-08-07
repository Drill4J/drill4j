import com.epam.drill.build.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*

plugins {
    `kotlin-multiplatform`
    `kotlinx-serialization`
}

val agentDeps by configurations.creating {}
val adminDeps by configurations.creating {}


kotlin {
    val jvms = listOf(
        jvm("adminPart"),
        jvm("agentPart")
    )
    createNativeTargetForCurrentOs("nativePart") {
        mainCompilation {
            binaries {
                sharedLib(
                    namePrefix = "native-plugin",
                    buildTypes = setOf(DEBUG)
                )
            }
        }
    }
    sourceSets {
        named("commonMain") {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$serializationRuntimeVersion")
            }
        }
        named("commonTest") {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        named("agentPartMain") {
            dependencies {
                implementation("com.epam.drill:drill-common-jvm:$drillCommonVersion")
                implementation(project(":drill-plugin-api:drill-agent-part"))
                implementation(kotlin("stdlib-jdk8"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationRuntimeVersion")
                project.dependencies.add("agentDeps", "org.jacoco:org.jacoco.core:0.8.3")
                api("org.jacoco:org.jacoco.core:0.8.3")
            }
        }
        named("nativePartMain") {
            dependencies {
                implementation("com.epam.drill:drill-common-${org.jetbrains.kotlin.konan.target.HostManager.simpleOsName()}x64:$drillCommonVersion")
                implementation(project(":drill-plugin-api:drill-agent-part"))
                implementation("com.epam.drill:drill-jvmapi-${org.jetbrains.kotlin.konan.target.HostManager.simpleOsName()}x64:$drillUtilsVersion")
            }
        }
        named("adminPartMain") {
            dependencies {
                implementation("com.epam.drill:drill-common-jvm:$drillCommonVersion")
                implementation(project(":drill-plugin-api:drill-admin-part"))
                implementation(kotlin("stdlib-jdk8"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationRuntimeVersion")
                implementation("org.jacoco:org.jacoco.core:0.8.3")

                project.dependencies.add("adminDeps", "org.jacoco:org.jacoco.core:0.8.3")
            }
        }

        //jvm junit deps
        jvms.forEach {
            it.compilations["test"].defaultSourceSet {
                dependencies {
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$jvmCoroutinesVersion")
                    implementation(kotlin("test-junit"))
                }
            }
        }
    }
}

//TODO replace custom tasks with some standard gradle plugin flow
tasks {
    val pluginConfigJson = file("plugin_config.json")

    fun Configuration.flattenJars() = this.map { if (it.isDirectory) it else zipTree(it) }

    val adminPartJar by existing(Jar::class) {
        group = "build"
        archiveFileName.set("admin-part.jar")
    }
    val agentPartJar by existing(Jar::class) {
        group = "build"
        archiveFileName.set("agent-part.jar")
    }

    val distJar by registering(Jar::class) {
        group = "build"
        doFirst {
            val binary = (kotlin.targets["nativePart"] as KotlinNativeTarget)
                .binaries
                .findSharedLib(
                    "native-plugin",
                    NativeBuildType.DEBUG
                )?.outputFile
            from(adminPartJar, agentPartJar, binary, pluginConfigJson)

        }
    }

    val buildToDistr by registering(Copy::class) {
        group = "app"
        dependsOn("build")
        from(distJar) {
            into("adminStorage")
        }
        destinationDir = project.rootProject.file("distr")
    }

    //TODO Remove after changes in CI/CD
    register<Copy>("buildNativePluginDev") {
        group = "app"
        dependsOn(buildToDistr)
    }
}