plugins {
    id("kotlin-multiplatform")
    id("kotlinx-serialization")
}

kotlin {
    targets {
        jvm("drillAdminPart")
    }

    sourceSets {
        val commonMain by getting

        commonMain.apply {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:0.9.1")
                implementation(project(":drill-common"))
            }
        }
        val drillAdminPartMain by getting
        drillAdminPartMain.apply {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
                implementation(project(":drill-common"))
            }
        }
    }
}
