import org.apache.tools.ant.taskdefs.condition.Os
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    kotlin("jvm") version ("1.3.21")
    kotlin("plugin.spring") version ("1.3.21")
    id("org.zeroturnaround.gradle.jrebel") version ("1.1.8")
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
            Os.isFamily(Os.FAMILY_UNIX) -> Pair("lib", "so")
            else -> Pair("", "dll")
        }
        val drillDistrDir = "${file("../../distr")}"
        val agentPath = "${file("$drillDistrDir/${pref}main.$ex")}"
        val configDir = "${file("../../resources")}"
        jvmArgs("-agentpath:$agentPath=configsFolder=$configDir,drillInstallationDir=$drillDistrDir")
    }

    
    named<BootRun>("bootRun") {
        jvmArgs("-Xmx2g")
        agentJvmArgs()
    }

    val bootJar by existing(BootJar::class)

    register<JavaExec>("bootJarRun") {
        group = "application"
        classpath(bootJar.map { it.archiveFile })
        main = "org.springframework.boot.loader.JarLauncher"
        agentJvmArgs()
    }
    
    named<Jar>("jar") {
        dependsOn(named("generateRebel"))
    }

    named<Test>("test") {
        jvmArgs("-javaagent:${file("../../distr/drill-core-agent.jar")}")
    }
}

idea {
    module {
        inheritOutputDirs = false
        outputDir = file("build/classes/kotlin/main")
        testOutputDir = file("build/classes/kotlin/test")
    }
}

//if (System.getenv("JREBEL_HOME") != null) {
//
//    val ext = when {
//        Os.isFamily(Os.FAMILY_MAC) -> "dylib"
//        Os.isFamily(Os.FAMILY_UNIX) -> "so"
//        Os.isFamily(Os.FAMILY_WINDOWS) -> "dll"
//        else -> {
//            throw RuntimeException("What is your OS???")
//        }
//    }
//    bootRun.jvmArgs("-agentpath:${System.getenv("JREBEL_HOME")}/lib/${if ("dll" == ext) {
//        ""
//    } else {
//        "lib"
//    }}jrebel64.$ext")
//}
rebel {
    //    showGenerated = true
//    rebelXmlDirectory = "build/classes"
//
//    classpath {
//        resource {
//            directory = "build/classes/kotlin/main"
//            includes = ["**/*"]
//        }
//
//
//        resource {
//            directory = "build/resources/main"
//        }
//    }
}
