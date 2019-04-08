FROM igorkuzminykh/drill-platform:1.0

RUN mkdir /app
RUN mkdir /app/distr
RUN mkdir /app/distr/configs

WORKDIR /app

COPY . ./

EXPOSE 90

RUN ./gradlew buildAgent

CMD ["./gradlew", "runAgent"]
