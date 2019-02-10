plugins {
    id("org.jetbrains.kotlin.jvm")
    id("kotlinx-serialization")
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
//    "implementation"(project(":common"))
    implementation("com.epam:drill4j-drillagentpart:0.0.1")
    "testCompile"(kotlin("test-junit"))
    "testCompile"(kotlin("test"))

//    compile(project(":core:drill-api:drill-common-multiplatform-jvm"))
//    implementation(project(":core:drill-api:drill-agent-part"))
//    implementation(project(":core:drill-api:drill-common"))

}

tasks {
    "jar"(Jar::class) {
        archiveName = "${project.name}.jar"
        from(provider {
            configurations.compile.filter { it.name.contains("natagentw") || it.name.contains("jvm") }.map { if (it.isDirectory) it else zipTree(it) }.toMutableList()
        })


        //todo temp solution for local development
        copy{
            from(archiveFile)
            into(file("../../../composite/spring-petclinic-kotlin/stuff/.drill/except-ions"))
        }
    }
}
