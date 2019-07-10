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
        createNativeTargetForCurrentOs("kjni")
    }

    sourceSets {
        val kjniMain by getting
        kjniMain.apply {
            kotlin.srcDir("src/kjniMain/basicprimitive")
            kotlin.srcDir("src/kjniMain/gen")
            dependencies {
                api(project(":nativeprojects:drill-jvmapi"))
            }
        }
    }
}
