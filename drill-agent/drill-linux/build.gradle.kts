import com.epam.drill.build.*


plugins {
    id("kotlin-multiplatform")
}


kotlin {
    targets {
        createNativeTargetForCurrentOs("linux")
    }

    sourceSets {
        named("linuxMain") {
            dependencies {
                implementation("com.epam.drill:drill-jvmapi-${org.jetbrains.kotlin.konan.target.HostManager.simpleOsName()}x64:$drillUtilsVersion")
                implementation("com.epam.drill:drill-agent-part:0.2.0")
            }
        }

    }
}
