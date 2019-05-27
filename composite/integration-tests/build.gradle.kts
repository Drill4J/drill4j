import org.apache.tools.ant.taskdefs.condition.Os
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.31"
}

repositories {
    mavenCentral()
}

dependencies {
    val ktorVersion = "1.2.0"
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-websockets:${ktorVersion}")
    implementation(kotlin("stdlib-jdk8"))
    implementation(fileTree(file("../../distr/drillRuntime.jar")))
    implementation(kotlin("test-junit"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
tasks{
    named<Test>("test"){
        val (pref, ex) = when {
            Os.isFamily(Os.FAMILY_UNIX) -> Pair("lib", "so")
            else -> Pair("", "dll")
        }
        val drillDistrDir = "${file("../../distr")}"
        val agentPath = "${file("$drillDistrDir/${pref}main.$ex")}"
        jvmArgs("-agentpath:$agentPath=drillInstallationDir=$drillDistrDir,adminAddress=host.docker.internal:8090,agentId=Petclinic")
    }
}