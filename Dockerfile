FROM java:openjdk-8
ADD / /drill
WORKDIR drill
RUN ./gradlew --refresh-dependencies
RUN ./gradlew buildAgent
WORKDIR /
RUN rm -r drill