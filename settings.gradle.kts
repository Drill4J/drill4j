rootProject.name = "drill4j"
include(":drill-admin")

/**plugin's projects*/
include(":plugins:drill-coverage-plugin")
//include(":plugins:drill-exception-plugin")

enableFeaturePreview("GRADLE_METADATA")