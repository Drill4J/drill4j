plugins {
    id("kotlin-multiplatform")
}


kotlin {
    targets {
        mingwX64("win")
        linuxX64("linux")
        macosX64("mac")
    }

    sourceSets {
        named("winMain") {
            dependencies {
                implementation("com.epam.drill:drill-jvmapi-windowsx64:$version")
                implementation("com.epam.drill:drill-agent-part-windowsx64:$version")
            }
        }
        named("linuxMain") {
            dependencies {
                implementation("com.epam.drill:drill-jvmapi-linuxx64:$version")
                implementation("com.epam.drill:drill-agent-part-linuxx64:$version")
            }
        }
        named("macMain") {
            dependencies {
                implementation("com.epam.drill:drill-jvmapi-macosx64:$version")
                implementation("com.epam.drill:drill-agent-part-macosx64:$version")
            }
        }

    }
}
