# Drill4J

[![License](https://img.shields.io/github/license/Drill4J/drill4j)](LICENSE)
[![Visit the website at drill4j.github.io](https://img.shields.io/badge/visit-website-green.svg?logo=firefox)](https://drill4j.github.io/)
[![Telegram Chat](https://img.shields.io/badge/Chat%20on-Telegram-brightgreen.svg)](https://t.me/drill4j)
![GitHub contributors](https://img.shields.io/github/contributors/Drill4J/drill4j)
![Lines of code](https://img.shields.io/tokei/lines/github/Drill4J/drill4j)
![YouTube Channel Views](https://img.shields.io/youtube/channel/views/UCJtegUnUHr0bO6icF1CYjKw?style=social)

Drill4J is an open-source tool[\*](#license) to identify testing gaps and reduce time spent on regression testing.

Drill4J provides a straight path to incorporating **Test Gap Analysis** and **Test Impact Analysis** into SDLC.

- It integrates with all parts of system under test, including backend and frontend services;
- It tracks all types of tests, including automated and manual;
- It detects changes in application code;
- It tracks code execution.

Based on that Drill4J:

- shows code not touched by any tests. Both in percentage and down to exact place in code.
- detects risks - modified or new code which needs to be tested;
- recommends minimal and sufficient set of tests;
- tracks if tests really covered aformentioned risks;
- provides comprehensive metrics which can be easily integrated to automated release pipeline with straightforward Quality Gate API.

All the above allows to speed up testing and development cycle, eliminate guesswork and back tests results with hard data.


## Know more

1. Try Drill4J in action with our [demo project](https://github.com/Drill4J/realworld-java-and-js-coverage) -  the examplary "RealWorld" application integration featuring:
   - Coverage, Risks and Test Recommendations metrics for Java backend and web frontend (at the same time)
   - E2E automated UI tests integration (Selenium)
   - Manual tests integration (requires [Browser Extension](https://github.com/Drill4J/browser-extension/releases) installation)

2. See our [conference talk](https://www.youtube.com/watch?v=U6vOJnzbReM)

3. Checkout our website [drill4j.github.io](drill4j.github.io)

## Main Components Overview

- Core 
   * [Admin Backend](https://github.com/Drill4J/admin) - central data aggregation point. Accepts data from _Agents_ to compute _Metrics_ (Coverage, Risks, Test Recommendations)
   * Admin UI Panel - web UI to view and manage data provided by _Admin Backend_

- Agents (for specific platforms)
   * [Java Agent](https://github.com/Drill4J/java-agent) - collects data from _Application Under Test_. Supports Java and other JVM languages.
   * [.NET agent](https://github.com/Drill4J/dotnet) - set of components to enable integration with .NET applications (see the [installation docs](https://drill4j.github.io/docs/drill4net/drill4net-apps))
   * JavaScript Agent - set of components ([Agent](https://github.com/Drill4J/js-agent), [Parser](https://github.com/Drill4J/js-parser), [Chrome DevTools proxy](https://github.com/Drill4J/devtools-proxy)) to collect data from JavaScript executed in web browsers. Supports only Google Chrome and Chromium-based browsers at the moment. Works with Chrome launched locally, remotely and in containerized environments (i.e. via [Selenoid](https://aerokube.com/selenoid/latest/)).

- Tests (for specific frameworks/tools)
   * [Java Auto Test Agent](https://github.com/Drill4J/autotest-agent) - extracts metadata from Java/JVM-based tests. Supports plethora of popular test frameworks (JUnit, JMeter, TestNG) and tools (Selenium, [Selenoid](https://aerokube.com/selenoid/latest/))
   * [Browser Extension](https://github.com/Drill4J/browser-extension) - allows to collect metrics for _Manual tests_
   * Drill4J is somewhat test-platform-agnostic - while we have tools and example integrations for other platforms (e.g. [Postman](https://github.com/Drill4J/pwad), [Cypress](https://github.com/Drill4J/cypress-example-integration)), it can be integrated (with some efforts) in almost any testing solution/setup

## Community / Support
[Telegram chat](https://t.me/drill4j)  
[Youtube channel](https://www.youtube.com/watch?v=N_WJYrt5qNc&feature=emb_title)

## License

<p id="license">
  <b>*License</b>: Drill4J is open-source and licensed under
  <a target="_blank" rel="noreferrer noopener" href="http://www.apache.org/licenses/LICENSE-2.0">
    Apache 2.0
  </a>
  .
</p>

