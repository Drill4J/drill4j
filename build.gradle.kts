plugins {
    base
}

tasks {

    val runAgent by registering {
        dependsOn(gradle.includedBuild("spring-petclinic-kotlin").task(":bootRun"))
        group = "application"
    }
}

allprojects {

    repositories {
        mavenCentral()
        mavenLocal()
        jcenter()
        maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
        maven(url = "https://kotlin.bintray.com/kotlinx")
    }
}