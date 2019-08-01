import org.jetbrains.kotlin.gradle.tasks.*

plugins {
    base
}

tasks {

    val runAgent by registering {
        dependsOn(gradle.includedBuild("spring-petclinic-kotlin").task(":bootRun"))
        group = "application"
    }
    val runIntegrationTests by registering {
        dependsOn(gradle.includedBuild("integration-tests").task(":test"))
        group = "application"
    }

    val cleanDistr by registering(Delete::class) {
        group = "build"
        delete("distr")
    }

    named("clean") {
        dependsOn(cleanDistr)
    }
}

allprojects {
    tasks.withType<KotlinCompile> {
        kotlinOptions.allWarningsAsErrors = true
    }
    tasks.withType<KotlinNativeCompile> {
        kotlinOptions.allWarningsAsErrors = true
    }
}

allprojects {

    repositories {
        mavenCentral()
        mavenLocal()
        jcenter()
        maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
        maven(url = "https://kotlin.bintray.com/kotlinx")
        maven(url = "https://mymavenrepo.com/repo/OgSYgOfB6MOBdJw3tWuX/")
    }
}