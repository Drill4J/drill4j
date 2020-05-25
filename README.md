# [Drill4J Project](https://drill4j.github.io/)
 [![Build Status](https://travis-ci.org/Drill4J/Drill4J.svg?branch=master)](https://travis-ci.org/Drill4J/Drill4J)
 [![License](https://camo.githubusercontent.com/8e7da7b6b632d5ef4bce9a550a5d5cfe400ca1fe/68747470733a2f2f696d672e736869656c64732e696f2f62616467652f6c6963656e73652d4170616368652532304c6963656e7365253230322e302d626c75652e7376673f7374796c653d666c6174)](http://www.apache.org/licenses/LICENSE-2.0)
<img src="./resources/logo.svg" alt="Logo" width="128" align="right">

*Drill* is **“feature-on-demand”** tool for real-time application profiling that does not affect codebase.
It provides the ability to make white box functional testing, via access to application instructions and memory.

## How to Start

### 1. Install Docker 
> _Whether you have Docker please skip this step_

Docker is supported by all major Linux distributions, MacOS and Windows.

[Download](https://www.docker.com/community-edition) and install Docker (Docker Engine, Compose, etc) 


**IMPORTANT!** Allocate at least 3+Gb of RAM for Docker via Docker Settings > Advanced. 

### 2. Configure and Deploy Drill Admin with Docker-Compose File

[docker-compose file](https://drill4j.github.io/assets/files/stable/docker-compose.yml)

Start Drill using the following command and wait a bit.

```console

docker-compose up -d

```

### 3. Run your Application with Drill Agent

> Now you have 2 typical ways to start your application with drill agent.

#### 3.1 If you use docker images of app you need to share volume with drill agent files and add JAVA parameters:
```yaml
version: '3'

services:
  example-app:
    image: repo/example-app:0.1.0
    ports:
      - 8080:8080
    volumes:
      - agent-files:/data    
    environment:
      - JAVA_TOOL_OPTIONS="-agentpath:/data/libdrill_agent.so=drillInstallationDir=/data,adminAddress=localhost:8090,agentId=ExampleAgentId,buildVersion=0.1.0"

  agent-files:
    image: drill4j/java-agent:0.5.0
    volumes:
      - agent-files:/java-agent

volumes:
  agent-files:
```
> **agent-files** - container with drill agent files.  
> **adminAddress** - host and backend port (**8090** - default port for agent connection) of you drill admin.  
> **agentId** - ID for drill agent of application.  
> **buildVersion** - build version of your application.


#### 3.2 If you use jar for running you can download the archive with the agent distribution for your OS:    
[**java-agent**](https://github.com/Drill4J/java-agent/releases/tag/v0.5.0) and extract files.
#### and start application with follow parameters

> (example for Windows):

```console

-agentpath:distr/drill_agent.dll=drillInstallationDir=distr,adminAddress=localhost:8090,agentId=ExampleAgent,buildVersion=0.1.0

```
> **distr** - folder with drill agent files. Use **.dll** for Windows, **.so** for Linux and **.dylib** agent file fo MacOS 
> **adminAddress** - host and backend port (**8090** - default port for agent connection) of you drill admin.  
> **agentId** - ID for drill agent of application.  
> **buildVersion** - build version of your application. 

### 4. Open Drill
Open new browser tab with Drill Admin [http://localhost:8091](http://localhost:8091)

Drill is ready for login. Press Continue as a guest button to get access.
 
The **last steps** before you can start working with Drill.  
#### Download [**drill-browser-extension**](https://github.com/Drill4J/browser-extension/releases/tag/v0.3.13) and install the browser extension for manual testing.

>Do not forget to register an agent before the start testing and add Test2Code plugin

Press "Register" button and follow registration wizards steps:
  * Fill all necessary general settings
  * Configure all necessary packages of your application
  * Add **Test2code** plugin
  

## Development installation

To launch the development environment, follow these steps:
1. Install JDK 8. **Installation path cannot have space characters**.
2. Run gradle tasks 'buildAgent', 'buildToDistr','runAgent'.
3. Run gradle task 'runDrillAdmin'.
4. Build frontend, follow this [link](https://github.com/Drill4J/admin-ui).
5. Install [Drill4J extension](https://chrome.google.com/webstore/detail/drill4j-browser-extension/lhlkfdlgddnmbhhlcopcliflikibeplm?hl=ru) for chrome.

## Technology

Used technology stack: [Kotlin-Native](https://kotlinlang.org/docs/reference/native-overview.html)

## Community / Support
[Telegram chat](https://t.me/drill4j)
[Youtube channel](https://www.youtube.com/watch?v=N_WJYrt5qNc&feature=emb_title)
