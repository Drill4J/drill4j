import org.apache.tools.ant.taskdefs.condition.Os

rootProject.name = "drill4j"
include(":drill-agent")
if (Os.isFamily(Os.FAMILY_UNIX)) {
    include("platformDependent")
    project(":platformDependent").projectDir = file("drill-agent/drill-linux")
} else if (Os.isFamily(Os.FAMILY_WINDOWS)) {
    include("platformDependent")
    project(":platformDependent").projectDir = file("drill-agent/drill-win")
}

include(":drill-admin")
include(":drill-plugin-api:drill-admin-part")
include(":drill-plugin-api:drill-agent-part")

/**plugin's projects*/
include(":plugins:drill-coverage-plugin")
include(":plugins:drill-exception-plugin")

includeBuild("./composite/spring-petclinic-kotlin")
includeBuild("./composite/integration-tests")
enableFeaturePreview("GRADLE_METADATA")