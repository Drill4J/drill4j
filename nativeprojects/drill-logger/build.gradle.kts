import com.epam.drill.build.createNativeTargetForCurrentOs
import com.epam.drill.build.korioVersion
import com.epam.drill.build.serializationNativeVersion


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
        createNativeTargetForCurrentOs("logger")
    }

    sourceSets {
        val loggerMain by getting
        loggerMain.apply {
            dependencies {
                implementation("com.soywiz:korio:$korioVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-native:$serializationNativeVersion")
            }
        }
    }
}
