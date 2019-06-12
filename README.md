# Drill4J Project 
 [![Build Status](https://travis-ci.org/Drill4J/Drill4J.svg?branch=master)](https://travis-ci.org/Drill4J/Drill4J)
 [![License](https://camo.githubusercontent.com/8e7da7b6b632d5ef4bce9a550a5d5cfe400ca1fe/68747470733a2f2f696d672e736869656c64732e696f2f62616467652f6c6963656e73652d4170616368652532304c6963656e7365253230322e302d626c75652e7376673f7374796c653d666c6174)](http://www.apache.org/licenses/LICENSE-2.0)
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
