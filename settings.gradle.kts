import org.apache.tools.ant.taskdefs.condition.Os

rootProject.name = "drill4j"
include(":drill-agent")
include(":drill-agent:util")
include(":drill-admin")

/**plugin's projects*/
include(":plugins:drill-coverage-plugin")
include(":plugins:drill-exception-plugin")

includeBuild("./composite/spring-petclinic-kotlin")
includeBuild("./composite/integration-tests")
enableFeaturePreview("GRADLE_METADATA")