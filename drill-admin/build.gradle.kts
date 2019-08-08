import com.epam.drill.build.*
import org.jetbrains.kotlin.gradle.tasks.*

plugins {
    kotlin("jvm")
    id("kotlinx-serialization")
    id("com.google.cloud.tools.jib") version "1.2.0"
    application
}

repositories {
    mavenCentral()
    jcenter()
    maven(url = "https://dl.bintray.com/kodein-framework/Kodein-DI/")
    mavenLocal()
}

val appMainClassName by extra("io.ktor.server.netty.EngineMain")

val appJvmArgs = listOf(
    "-server",
    "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5006",
    "-Djava.awt.headless=true",
    "-Xms128m",
    "-Xmx2g",
    "-XX:+UseG1GC",
    "-XX:MaxGCPauseMillis=100"
)


application {
    mainClassName = appMainClassName

    applicationDefaultJvmArgs = appJvmArgs
}


dependencies {
    implementation("com.epam.drill:drill-common-jvm:$version")
    implementation("com.epam.drill:drill-admin-part-jvm:$version")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationRuntimeVersion")
    implementation("org.litote.kmongo:kmongo:3.9.0")
    implementation("de.flapdoodle.embed:de.flapdoodle.embed.mongo:2.1.1")
    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("io.ktor:ktor-auth-jwt:$ktorVersion")
    implementation("org.kodein.di:kodein-di-generic-jvm:6.2.0")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-locations:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-websockets:$ktorVersion")
    implementation("io.ktor:ktor-html-builder:$ktorVersion")
    implementation("io.github.microutils:kotlin-logging:1.6.24")
    implementation("org.jetbrains.exposed:exposed:0.13.7")
    implementation("com.h2database:h2:1.4.197")
    implementation("org.postgresql:postgresql:9.4-1200-jdbc41")
    implementation("com.zaxxer:HikariCP:2.7.8")
    implementation("com.hazelcast:hazelcast:3.12")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.testcontainers:testcontainers:1.11.1")

}

jib {
    from {
        image = "gcr.io/distroless/java:8"
    }
    to {
        image = "drill4j/${project.name}"
        tags = mutableSetOf("latest")
    }
    container {
        ports = listOf("8090", "5006")
        mainClass = appMainClassName

        jvmFlags = appJvmArgs
    }
}

sourceSets {
    main {
        output.resourcesDir = file("build/classes/kotlin/main")
    }
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    //TODO Only for backward compatibility, remove after CI/CD has been configured
    register("runDrillAdmin") {
        group = "application"
        dependsOn("run")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=io.ktor.locations.KtorExperimentalLocationsAPI"
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=io.ktor.util.KtorExperimentalAPI"
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlin.Experimental"
}