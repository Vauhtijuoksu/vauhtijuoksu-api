FROM eclipse-temurin:21.0.8_9-jre

RUN mkdir /vauhtijuoksu-api

WORKDIR "/vauhtijuoksu-api"
CMD ["server/bin/server"]

ADD build/distributions/server.tar /vauhtijuoksu-api
