
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm")
}
repositories {
    mavenCentral()
    mavenLocal()
    jcenter()
    maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
    maven(url = "https://kotlin.bintray.com/kotlinx")
}


dependencies {
    "implementation"(enforcedPlatform(kotlin("stdlib")))
    "implementation"(kotlin("reflect"))
    "testCompile"(kotlin("test-junit"))
    "testCompile"(kotlin("test"))
//    "implementation"(project(":common"))
    implementation("com.epam:drill4j-drilladminpart:0.0.1")
//    compileOnly(project(":core:drill-api:drill-admin-part"))
//    compileOnly(project(":core:drill-api:drill-common"))
//    compileOnly(project(":core:drill-api:drill-common-multiplatform-jvm"))
//    compile(project(":plugins:drill-exception-plugin:common:jvm"))
}

tasks {
    "jar"(Jar::class) {
        archiveName = "${project.name}.jar"
//        dependsOn(":plugins:drill-exception-plugin:frontend:jar")
        from(
//                provider {
//                    val jar: Jar by project(":plugins:drill-exception-plugin:frontend").tasks
//                    val frontFiles = zipTree(jar.archivePath)
//                    frontFiles
//                },
                provider { configurations.compile.filter { it.name.contains("jvm") }.map { if (it.isDirectory) it else zipTree(it) }.toMutableList() })
    }

}