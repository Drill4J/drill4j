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
                implementation("com.epam.drill:drill-jvmapi-$preset:$version")
                implementation(project(":drill-plugin-api:drill-agent-part"))
            }
        }

    }
}
