---
name: General Setup Issue
about: Use that template to report issues with Drill4J installation process
title: New Issue
labels: ''
assignees: RomanDavlyatshin

---

# New Issue

<!--
Please follow the template.
Issues not adhering to that won't be addressed.
-->

## Description

Issue:

<!-- A brief description summarizing your issue in 2-3 sentences -->

## Details

1. Your application

   - OS: <!-- e.g. Windows 10, MacOs, Alpine, etc -->
   - Tech stack: <!-- e.g. Java + Spring Boot, or backend with Java + frontend with Typescript & React -->

2. Your tests

   - OS: <!-- e.g. Windows 10, MacOs, Alpine, etc -->
   - Tech stack: <!-- e.g. Java + TestNG, Java + TestNG + Selenium, NodeJS + Jest, Postman -->
   - Other relevant test setup details: <!-- e.g. Selenium & browser version for automated UI tests -->

3. Drill4J services configuration and versions
    <!--
      docker-compose.yml and .env files used to launch Drill4J services
      if you are not using Docker and manage Drill4J services manually provide the appropriate component list with versions
    -->

   - docker-compose.yml
   - .env file

4. Log files (skip if some are unapplicable):
    <!--
      IMPORTANT:
      when writing logs to file make sure to output stderr as well
    -->

   - Drill4J Admin Backend service
      <!--
        Use "docker ps" and "docker logs" commands to get container log
        IMPORTANT: to set the highest log level pass - LOG_LEVEL=trace to "environment" section.
        example: https://github.com/Drill4J/example-configs/blob/main/0.8.0/prerelease/api-tests/docker-compose-admin.yml#L18
      -->

   - Drill4J Java Agent
      <!--
        Drill4J Java Agent writes logs in the same process as your application.
        IMPORTANT: to set the highest log level pass ,logLevel=TRACE at the end of -agentpath string
        example: https://github.com/Drill4J/example-configs/blob/1d079dbd973bb139500d8b4cb8eebe115f63f9e7/0.8.0/prerelease/api-tests/docker-compose-your-app.yml#L11
      -->

   - Drill4J Auto Test Agent
      <!--
        Drill4J Autotest Agent writes logs to the file specified in Drill4J Agent Runner configuration.
        Make sure to set logLevel to TRACE.
        Example:
        - Gradle: https://github.com/Drill4J/example-configs/blob/1d079dbd973bb139500d8b4cb8eebe115f63f9e7/0.8.0/prerelease/api-tests/example--build.gradle#L27
        - Maven: https://github.com/Drill4J/example-configs/blob/1d079dbd973bb139500d8b4cb8eebe115f63f9e7/0.8.0/prerelease/api-tests/example--pom.xml#L23
      -->

   - Drill4J DevTools Proxy (for Selenium-based tests)
     <!-- attach docker container logs -->

   - Drill4J JavaScript Agent (when applicable)
     <!-- attach docker container logs -->

## Screenshots

<!-- If applicable, add screenshots to help explain your problem. -->

## Additional Notes

<!-- Anything you find relevant to the issue -->
