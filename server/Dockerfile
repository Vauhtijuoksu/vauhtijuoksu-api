FROM eclipse-temurin:17.0.5_8-jre

RUN mkdir /vauhtijuoksu-api

WORKDIR "/vauhtijuoksu-api"
CMD ["server/bin/server"]

ADD build/distributions/server.tar /vauhtijuoksu-api
