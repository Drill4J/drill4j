import com.epam.drill.build.jvmCoroutinesVersion
import com.epam.drill.build.serializationRuntimeVersion

plugins {
    `kotlin-multiplatform`
    `kotlinx-serialization`
}

val agentDeps by configurations.creating{}
val adminDeps by configurations.creating{}


kotlin {
    val jvms = listOf(
        jvm("adminPart"),
        jvm("agentPart")
    )
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
                implementation(project(":drill-common"))
                implementation(project(":drill-plugin-api:drill-agent-part"))
                implementation(kotlin("stdlib-jdk8"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationRuntimeVersion")
                project.dependencies.add("agentDeps", "org.jacoco:org.jacoco.core:0.8.3")
                api("org.jacoco:org.jacoco.core:0.8.3")
                api("org.javers:javers-core:5.3.4")
            }
        }
        named("adminPartMain") {
            dependencies {
                implementation(project(":drill-common"))
                implementation(project(":drill-plugin-api:drill-admin-part"))
                implementation(kotlin("stdlib-jdk8"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationRuntimeVersion")
                implementation("org.jacoco:org.jacoco.core:0.8.3")
                implementation("org.javers:javers-core:5.3.4")

                project.dependencies.add("adminDeps","org.jacoco:org.jacoco.core:0.8.3")
                project.dependencies.add("adminDeps","org.javers:javers-core:5.3.4")

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
        from(adminDeps.flattenJars())
    }
    val agentPartJar by existing(Jar::class) {
        group = "build"
        archiveFileName.set("agent-part.jar")
        from(agentDeps.flattenJars())
    }

    val distJar by registering(Jar::class) {
        group = "build"
        from(adminPartJar, agentPartJar)
        from(pluginConfigJson)
    }

    val buildToDistr by registering(Copy::class) {
        group = "app"
        from(distJar) {
            into("adminStorage")
        }
        destinationDir = project.rootProject.file("distr")
    }

    register<Copy>("buildCoveragePluginDev") {
        group = "app"
        dependsOn(buildToDistr)
    }
}