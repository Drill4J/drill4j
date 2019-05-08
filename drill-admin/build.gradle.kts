import com.epam.drill.build.ktorVersion
import com.epam.drill.build.serializationRuntimeVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("kotlinx-serialization")
    application
}

repositories {
    mavenCentral()
    jcenter()
    maven(url = "https://dl.bintray.com/kodein-framework/Kodein-DI/")
    mavenLocal()
}

val appMainClassName = "io.ktor.server.netty.EngineMain"


application {
    mainClassName = appMainClassName

    applicationDefaultJvmArgs = listOf(
        "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5006",
        "-Djava.awt.headless=true",
        "-Xms128m",
        "-Xmx2g",
        "-XX:+UseG1GC",
        "-XX:MaxGCPauseMillis=100"
    )
}


dependencies {
    implementation(project(":drill-common"))
    implementation(project(":drill-plugin-api:drill-admin-part"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationRuntimeVersion")
    implementation("org.litote.kmongo:kmongo:3.9.0")
    implementation("de.flapdoodle.embed:de.flapdoodle.embed.mongo:2.1.1")
    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("io.ktor:ktor-auth-jwt:$ktorVersion")
    implementation("org.kodein.di:kodein-di-generic-jvm:6.2.0")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-locations:$ktorVersion")
    implementation("io.ktor:ktor-gson:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-websockets:$ktorVersion")
    implementation("io.ktor:ktor-html-builder:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:1.2.1")
    implementation("ch.qos.logback:logback-classic:1.2.1")


    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.testcontainers:testcontainers:1.11.1")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    val run by existing(JavaExec::class)

    //TODO Only for backward compatibility, remove after CI/CD has been configured
    register("runDrillAdmin") {
        group = "application"
        dependsOn(run)
    }
}