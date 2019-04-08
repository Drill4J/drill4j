FROM igorkuzminykh/drill-platform:1.0

RUN mkdir /app
RUN mkdir /app/distr
RUN mkdir /app/distr/adminStorage
RUN chmod -R +r /app
WORKDIR /app

COPY . ./

EXPOSE 90

CMD ["./gradlew", "runDrillAdmin"]

