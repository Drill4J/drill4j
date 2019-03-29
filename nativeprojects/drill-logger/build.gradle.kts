import com.epam.drill.build.*


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
                implementation("com.soywiz:korio:$korioVersion")
                implementation("com.soywiz:klogger:$kloggerVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-native:$serializationNativeVersion")
            }
        }
    }
}
