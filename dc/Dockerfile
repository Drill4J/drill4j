FROM java:openjdk-8
ADD ../ /drill
WORKDIR drill
RUN ./gradlew build
WORKDIR /
RUN rm -r drill

