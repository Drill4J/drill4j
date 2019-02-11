# Drill4J
Next-gen web-profiler

#### Compiling (now only for the Windows)
to build agent and runtime:
    gradlew --refresh-dependencies build
    gradlew buildPlugin

to run the demo:
1) cd ./docker-compose. RUN "docker-compose up". mongodb will be available on 21017 port
2) To run demo app with drillAgent  RUN - "gradlew runAgent". The demo app will be available on localhost:8082
3) To run drillAdmin  RUN - "gradlew runDrillAdmin". The admin will be available on localhost:8090
