import com.epam.drill.build.*


plugins {
    id("kotlin-multiplatform")
}


kotlin {
    targets {
        createNativeTargetForCurrentOs("win")


    }

    sourceSets {
        named("winMain") {
            dependencies {
                implementation(project(":nativeprojects:drill-jvmapi"))
                implementation(project(":drill-plugin-api:drill-agent-part"))
            }
        }

    }
}
