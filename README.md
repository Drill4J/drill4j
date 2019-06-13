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

To launch the development environment, follow these steps:
1. Install [JDK 8](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html). Installation path cannot have space characters.
2. Run gradle tasks 'buildAgent', 'buildCoveragePluginDev','runAgent'.
3. Run gradle task 'runDrillAdmin'.
4. Build frontend, follow this [link](https://github.com/Drill4J/admin-ui).
5. Install [Drill4J extension](https://chrome.google.com/webstore/detail/drill4j-browser-extension/lhlkfdlgddnmbhhlcopcliflikibeplm?hl=ru) for chrome.


#### Deploy Drill4J using Docker
Install docker
Docker is supported by all major Linux distributions, MacOS and Windows.

Note: for Windows users. [Docker for Windows](https://docs.docker.com/docker-for-windows/) requires 64-bit Windows 10 Pro and Microsoft Hyper-V. If your system does not satisfy these requirements, you can install [Docker Toolbox](https://docs.docker.com/toolbox/toolbox_install_windows/), which uses Oracle Virtual Box instead of Hyper-V.

[Download](https://www.docker.com/community-edition) and install Docker (Docker Engine, Compose, etc)

IMPORTANT If you use Docker for Windows or MacOS, make sure there is at least 3gb dedicated for Docker.

Open CMD and enter commands:

1.  cd <path_to_the_folder>\docker-compose 
2.  docker-compose run --rm gradle buildAgent buildCoveragePluginDev
3.  docker-compose -p rp up -d
4.  gradlew runDrillAdmin

## Technology

Used technology stack: [Kotlin-Native](https://kotlinlang.org/docs/reference/native-overview.html)
