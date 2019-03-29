import com.epam.drill.build.createNativeTargetForCurrentOs
import com.epam.drill.build.korioVersion

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
        createNativeTargetForCurrentOs("kasm")
    }

    sourceSets {
        val kasmMain by getting
        kasmMain.apply {
            dependencies {
                implementation("com.soywiz:korio:$korioVersion")
            }
        }
    }
}
