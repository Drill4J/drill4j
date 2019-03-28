import com.epam.drill.build.createNativeTargetForCurrentOs

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
                implementation("com.soywiz:korio:1.1.6-drill")
            }
        }
    }
}
