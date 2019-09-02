import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

plugins {
    base
}

tasks {

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