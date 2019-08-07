import com.epam.drill.build.*

plugins {
    `kotlin-multiplatform`
    `kotlinx-serialization`
    `kotlinx-atomicfu`
}

val jacocoVersion = "0.8.3"
val vavrVersion = "0.10.0"
val bcelVersion = "6.3.1"

val commonJarDeps by configurations.creating {}

val agentJarDeps by configurations.creating {
    extendsFrom(commonJarDeps)
}

val adminJarDeps by configurations.creating {
    extendsFrom(commonJarDeps)
}

dependencies {
    commonJarDeps("org.jacoco:org.jacoco.core:$jacocoVersion")
    commonJarDeps("io.vavr:vavr-kotlin:$vavrVersion")
    commonJarDeps("org.apache.bcel:bcel:$bcelVersion")
}


kotlin {
    val jvms = listOf(
        jvm(),
        jvm("adminPart"),
        jvm("agentPart")
    )
    
    

    sourceSets {
        named("commonMain") {
            dependencies {
                implementation("com.epam.drill:drill-common-jvm:$drillCommonVersion")
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
        val jvmMain by getting {
            project.configurations.named(implementationConfigurationName) {
                extendsFrom(commonJarDeps)
            }
        }
        val agentPartMain by getting {
            project.configurations.named(implementationConfigurationName) {
                extendsFrom(agentJarDeps)
            }
            dependencies {
                
                implementation("com.epam.drill:drill-agent-part-jvm:0.2.0")
            }
        }
        agentPartMain.dependsOn(jvmMain)
        val adminPartMain by getting {
            project.configurations.named(implementationConfigurationName) {
                extendsFrom(adminJarDeps)
            }
            dependencies {
                implementation("com.epam.drill:drill-admin-part-jvm:0.2.0")
            }
        }
        adminPartMain.dependsOn(jvmMain)

        //common jvm deps
        jvms.forEach {
            it.compilations["main"].defaultSourceSet {
                dependencies {
                    implementation("com.epam.drill:drill-common:$drillCommonVersion")
                    implementation(kotlin("stdlib-jdk8"))
                    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationRuntimeVersion")
                    compileOnly("org.jetbrains.kotlinx:atomicfu:$atomicFuVersion")
                }
            }
            it.compilations["test"].defaultSourceSet {
                dependencies {
                    implementation("org.jetbrains.kotlinx:atomicfu:$atomicFuVersion")
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
        from(adminJarDeps.flattenJars())
    }
    val agentPartJar by existing(Jar::class) {
        group = "build"
        archiveFileName.set("agent-part.jar")
        from(agentJarDeps.flattenJars())
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
        from(agentPartJar) {
            into("tests/coverage")
        }
        destinationDir = project.rootProject.file("distr")
    }

    register<Copy>("buildCoveragePluginDev") {
        group = "app"
        dependsOn(buildToDistr)
    }
}