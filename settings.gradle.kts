import org.apache.tools.ant.taskdefs.condition.*

rootProject.name = "drill4j"
include(":drill-agent")
include(":drill-common")
include(":drill-admin")
include(":drill-plugin-api:drill-admin-part")
include(":drill-plugin-api:drill-agent-part")
include(":nativeprojects:drill-jvmapi")

if (Os.isFamily(Os.FAMILY_UNIX)) {
    include("platformDependent")
    project(":platformDependent").projectDir = file("nativeprojects/drill-linux")
} else if (Os.isFamily(Os.FAMILY_WINDOWS)) {
    include("platformDependent")
    project(":platformDependent").projectDir = file("nativeprojects/drill-win")
}

/**plugin's projects*/
include(":plugins:drill-coverage-plugin")
include(":plugins:drill-exception-plugin")

includeBuild("./composite/spring-petclinic-kotlin")
includeBuild("./composite/integration-tests")
enableFeaturePreview("GRADLE_METADATA")