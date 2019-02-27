FROM openjdk:8-jre-alpine

ENV APPLICATION_USER ktor
RUN adduser -D -g '' $APPLICATION_USER

RUN mkdir /app
RUN mkdir /app/distr
RUN mkdir /app/distr/adminStorage
RUN chown -R $APPLICATION_USER /app
WORKDIR /app

USER $APPLICATION_USER

COPY /drill-admin/build/libs/drillAdmin.jar /app/drillAdmin.jar

EXPOSE 90

CMD ["java", "-server", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "-XX:InitialRAMFraction=2", "-XX:MinRAMFraction=2", "-XX:MaxRAMFraction=2", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100", "-XX:+UseStringDeduplication", "-jar", "drill-admin-admin-0.0.1.jar"]