# Drill4J Project · [![Build Status](https://travis-ci.org/Drill4J/Drill4J.svg?branch=master)](https://travis-ci.org/Drill4J/Drill4J)

<img src="./resources/logo.svg" alt="Logo" width="128" align="right">

Drill4J is a plugin management platform for real-time application profiling and extension that does not affect code base.

Drill4J allows you to:

-   automate and organize data
-   choose the right solutions for specific tasks
-   increase velocity and quality of the team
-   store all plugins in one place
-   flexible configuration of agents and plugins for project needs

## Documentation

Work in progress.

## Development installation

To build agent and runtime:

1.  cd docker-compose
2.  docker-compose run --rm gradle buildAgent buildCoveragePluginDev

To run the demo:

1.  docker-compose -p rp up -d
2.  gradlew runDrillAdmin

## Technology

Used technology stack: [Kotlin-Native](https://kotlinlang.org/docs/reference/native-overview.html)
