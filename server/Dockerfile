FROM eclipse-temurin:18-jre

RUN mkdir /vauhtijuoksu-api

WORKDIR "/vauhtijuoksu-api"
CMD ["server/bin/server"]

ADD build/distributions/server.tar /vauhtijuoksu-api
