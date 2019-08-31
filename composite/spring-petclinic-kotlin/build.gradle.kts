import org.apache.tools.ant.taskdefs.condition.*
import org.jetbrains.kotlin.gradle.tasks.*
import org.springframework.boot.gradle.tasks.bundling.*
import org.springframework.boot.gradle.tasks.run.*

plugins {
    kotlin("jvm") version ("1.3.30")
    kotlin("plugin.spring") version ("1.3.30")
    id("org.springframework.boot") version ("2.0.0.RELEASE")
    id("io.spring.dependency-management") version ("1.0.4.RELEASE")
    id("idea")
}

val boostrapVersion = "3.3.6"
val jQueryVersion = "2.2.4"
val jQueryUIVersion = "1.11.4"

version = "2.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("javax.cache:cache-api")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.webjars:webjars-locator-core")
    implementation("org.webjars:jquery:$jQueryVersion")
    implementation("org.webjars:jquery-ui:$jQueryUIVersion")
    implementation("org.webjars:bootstrap:$boostrapVersion")

    testCompile("org.springframework.boot:spring-boot-starter-test") {
    }
    testCompile("org.springframework.boot:spring-boot-starter-webflux") {
        exclude(group = "org.junit")
    }
    testCompile("org.springframework.boot:spring-boot-starter-tomcat") {
        exclude(group = "org.junit")
    }
    testCompile("org.mock-server:mockserver-netty:3.9.1")
    runtime("org.hsqldb:hsqldb")
    runtime("mysql:mysql-connector-java")
}


tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    val agentJvmArgs: JavaExec.() -> Unit = {
        jvmArgs("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5007")
        val (pref, ex) = when {
            Os.isFamily(Os.FAMILY_MAC) -> Pair("lib", "dylib")
            Os.isFamily(Os.FAMILY_UNIX) -> Pair("lib", "so")
            else -> Pair("", "dll")
        }
        val drillDistrDir = "${file("../../distr")}"
        val agentPath = "${file("$drillDistrDir/${pref}drill_agent.$ex")}"
        jvmArgs(
            "-agentpath:$agentPath=drillInstallationDir=$drillDistrDir,adminAddress=${project.properties["adminAddress"]
                ?: "localhost:8090"},agentId=${project.properties["agentId"] ?: "Petclinic"}"
        )
    }


    named<Test>("test") {
        testLogging {
            showStandardStreams = true
        }
    }
    named<BootRun>("bootRun") {
        jvmArgs("-Xmx2g")
        agentJvmArgs()
    }

    val bootJar by existing(BootJar::class)
}

idea {
    module {
        inheritOutputDirs = false
        outputDir = file("build/classes/kotlin/main")
        testOutputDir = file("build/classes/kotlin/test")
    }
}
