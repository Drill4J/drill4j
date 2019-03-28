import com.epam.drill.build.createNativeTargetForCurrentOs
import com.epam.drill.build.mainCompilation

plugins {
    id("kotlin-multiplatform")
    id("kotlinx-serialization")
}

repositories {
    maven(url = "https://dl.bintray.com/soywiz/soywiz")
    maven(url = "https://mymavenrepo.com/repo/OgSYgOfB6MOBdJw3tWuX/")
}

kotlin {
    targets {
        createNativeTargetForCurrentOs("logger") {
            mainCompilation {
                val storage by cinterops.creating
                storage.apply {

                }
            }
        }
    }

    sourceSets {
        val loggerMain by getting
        loggerMain.apply {
            dependencies {
                implementation("com.soywiz:korio:1.1.6-drill")
                implementation("com.soywiz:klogger:1.2.1")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-native:0.10.0")
            }
        }
    }
}
