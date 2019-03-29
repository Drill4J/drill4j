rootProject.name = "drill4j"
include(":drill-agent")
include(":drill-common")
include(":drill-admin")
include(":drill-plugin-api:drill-admin-part")
include(":drill-plugin-api:drill-agent-part")
include(":nativeprojects:drill-jvmapi")
include(":nativeprojects:drill-kni")
include(":nativeprojects:drill-kasm")
include(":nativeprojects:drill-logger")

/**plugin's projects*/
include(":plugins:drill-exception-plugin-native")
include(":plugins:drill-custom-plugin")
include(":plugins:drill-coverage-plugin")

includeBuild("./composite/spring-petclinic-kotlin")
enableFeaturePreview("GRADLE_METADATA")