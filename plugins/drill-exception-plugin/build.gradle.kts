import org.gradle.util.GFileUtils

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

val pluginsDestination = File("../../distr/drill-plugins")

configure(mainPluginProjects()) {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    tasks {

        "jar"(org.gradle.api.tasks.bundling.Jar::class) {
            doFirst {
                GFileUtils.copyFile(parent!!.file("./plugin_config.json"), File(buildDir, "resources/main/static/plugin_config.json"))
                println("was copied")
            }
        }
    }
}

val qq by configurations.creating

dependencies {
    qq(fileTree(mainPluginProjects().map { (it.tasks["jar"] as Jar).destinationDir }))
}


fun mainPluginProjects(): List<Project> {
    return subprojects.filter { it.name == "agent-part" || it.name == "admin-part" }
}

tasks {
    "jar"(Jar::class) {
        group = LifecycleBasePlugin.BUILD_GROUP

        destinationDir = pluginsDestination
        from(mainPluginProjects().map { it.tasks["jar"] }) {
            include("*.jar")
        }
        include("*.jar")

    }

    "clean"(Delete::class) {
        group = LifecycleBasePlugin.BUILD_GROUP
        delete(pluginsDestination.absolutePath)
    }
}

