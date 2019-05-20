[![Build Status](https://travis-ci.org/Drill4J/Drill4J.svg?branch=master)](https://travis-ci.org/Drill4J/Drill4J)

# Drill4J
Next-gen web-profiler

#### Compiling
to build agent and runtime:
    cd docker-compose
    docker-compose run --rm gradle buildAgent buildCoveragePluginDev


to run the demo:
    docker-compose -p rp up -d
    gradlew runDrillAdmin
