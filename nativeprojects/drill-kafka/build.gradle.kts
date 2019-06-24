import com.epam.drill.build.createNativeTargetForCurrentOs
import com.epam.drill.build.mainCompilation


plugins {
    id("kotlin-multiplatform")
}


kotlin {
    targets {
        createNativeTargetForCurrentOs("kafka") {
            mainCompilation {
                val kafka by cinterops.creating
                kafka.apply {
                    //                linkerOpts("-L${file("./").resolve("kafka")}")
                    includeDirs("../../rdkafka-distr")
                }
            }
        }
    }

    sourceSets {
        named("kafkaMain") {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-native:1.2.0")
            }
        }

    }
}
