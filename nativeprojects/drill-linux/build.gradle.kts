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
                implementation(project(":nativeprojects:drill-jvmapi"))
                implementation(project(":drill-plugin-api:drill-agent-part"))
            }
        }

    }
}
