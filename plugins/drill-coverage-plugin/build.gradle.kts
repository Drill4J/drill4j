import com.epam.drill.build.jvmCoroutinesVersion

plugins {
    `kotlin-multiplatform`
    `kotlinx-serialization`
}

kotlin {
    val jvms = listOf(
        jvm("adminPart"),
        jvm("agentPart")
    )

    sourceSets {
        named("commonMain") {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:0.9.1")
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
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.9.1")
                api("org.jacoco:org.jacoco.core:0.8.3")
                api("com.google.guava:guava:27.1-jre") //only for loading classes from package
            }
        }
        named("adminPartMain") {
            dependencies {
                implementation(project(":drill-common"))
                implementation(project(":drill-plugin-api:drill-admin-part"))
                implementation(kotlin("stdlib-jdk8"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.9.1")
                api("org.jacoco:org.jacoco.core:0.8.3")
                api("org.javers:javers-core:5.3.4")
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
        from(pluginConfigJson) {
            into("static")
        }
        val adminPartMainApi by configurations
        from(adminPartMainApi.flattenJars())
    }
    val agentPartJar by existing(Jar::class) {
        group = "build"
        archiveFileName.set("agent-part.jar")
        from(pluginConfigJson) {
            into("static")
        }
        val agentPartMainApi by configurations
        from(agentPartMainApi.flattenJars())
    }

    val distJar by registering(Jar::class) {
        group = "build"
        from(adminPartJar, agentPartJar)
    }

    register<Copy>("buildCoveragePluginDev") {
        group = "app"
        from(distJar) {
            into("adminStorage")
        }
        from(agentPartJar) {
            into("drill-plugins/coverage")
        }
        from(file("plugin_config.json")) {
            into("drill-plugins/coverage/static")
        }
        destinationDir = file("../../distr")
    }
}