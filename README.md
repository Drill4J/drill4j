# [Drill4J Project](https://drill4j.github.io/)

[![License](https://img.shields.io/github/license/Drill4J/drill4j)](LICENSE)
[![Visit the website at drill4j.github.io](https://img.shields.io/badge/visit-website-green.svg?logo=firefox)](https://drill4j.github.io/)
[![Telegram Chat](https://img.shields.io/badge/Chat%20on-Telegram-brightgreen.svg)](https://t.me/drill4j)
![GitHub contributors](https://img.shields.io/github/contributors/Drill4J/drill4j)
![Lines of code](https://img.shields.io/tokei/lines/github/Drill4J/drill4j)
![YouTube Channel Views](https://img.shields.io/youtube/channel/views/UCJtegUnUHr0bO6icF1CYjKw?style=social)


<img src="./resources/logo.svg" alt="Logo" width="128" align="right">

*Drill* is a **“feature-on-demand”** tool for real-time application profiling that **does not affect codebase**.

*Drill* enables white box functional testing, via access to application instructions and memory.

<img  width="750" height="520" src ="https://drill4j.github.io/img/auto-testing-diagram.png" />

## Quick Start

See the [How to Start](https://drill4j.github.io/how-to-start) section of the project site.

## Basic repositories 

### Backend

* [admin](https://github.com/Drill4J/admin) - backend (Ktor)
* [test2code](https://github.com/Drill4J/test2code-plugin) - code coverage plugin
* [state-watcher](https://github.com/Drill4J/state-watcher-plugin) - real-time metrics plugin (in progress)

### Frontend

#### Micro Frontends

* [admin-ui-root-config](https://github.com/Drill4J/admin-ui-root-config)
* [test2code-ui](https://github.com/Drill4J/test2code-ui)
* [state-watcher-ui ](https://github.com/Drill4J/state-watcher-ui)

#### Monolith (Obsolete)

* [admin-ui](https://github.com/Drill4J/admin-ui) - frontend (React)

### Agents

* [java-agent](https://github.com/Drill4J/java-agent) - native agent used to profile a java application
    * [Launch params](https://drill4j.github.io/docs/configuration/launch-parameters#java-agent)
* [autotest-agent](https://github.com/Drill4J/autotest-agent) - native agent for JVM autotest (JUnit, TestNG and Cucumber)
    * [Launch-params](https://drill4j.github.io/docs/configuration/launch-parameters#java-agent)
* [js-agent](https://github.com/Drill4J/js-agent) - agent for frontend applications

#### Other

* [agent-runner](https://github.com/Drill4J/agent-runner) - maven/gradle plugin provides dsl to run java agent and autotest agent
* [browser-extension](https://github.com/Drill4J/browser-extension) - for manual testing with Drill4J
* [pwad](https://github.com/Drill4J/pwad) - wrapper over Newman CLI that enables Drill4J metrics for Postman collection runs.
* [intellij-plugin](https://github.com/Drill4J/intellij-plugin) - intellij-plugin mapping coverage from test to source code

**Full details of the repositories can be found** [here](https://github.com/Drill4J/drill4j/wiki/Contribution#repository-structure) 


## Contribution

There are many ways to contribute to Drill4J's development, just find the one that best fits with your skills. Examples of contributions we would love to receive include:

- **Code patches**
- **Documentation improvements**
- **Bug reports**
- **Patch reviews**
- **UI enhancements**

Big features are also welcome but if you want to see your contributions included in Drill4J codebase we strongly recommend you start by initiating a [telegram chat](https://t.me/drill4j).

[Contribution details](https://github.com/Drill4J/drill4j/wiki/Contribution)

## Documentation
* [User Manual](https://drill4j.github.io/docs/installation/drill-admin)

## Tech Stack

* [Kotlin Native](https://kotlinlang.org/docs/reference/native-overview.html)
* [Kotlin Server-side](https://kotlinlang.org/docs/reference/native-overview.html)
* [Typescript](https://www.typescriptlang.org/)
* [React](https://reactjs.org/)

## Community / Support
[Telegram chat](https://t.me/drill4j)  
[Youtube channel](https://www.youtube.com/watch?v=N_WJYrt5qNc&feature=emb_title)

## License

Drill4j is [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0).
