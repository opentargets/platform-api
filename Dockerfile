FROM sbtscala/scala-sbt:eclipse-temurin-jammy-11.0.17_8_1.8.2_2.13.10 as build
RUN apt-get update \
    && apt-get install -y unzip \
    && rm -rf /var/lib/apt/lists/*

COPY . /platform-api
WORKDIR /platform-api
RUN sbt dist
RUN unzip -q ot-platform-api-latest.zip

FROM eclipse-temurin:11
COPY --from=build /platform-api/ot-platform-api-latest /srv/app/ot-platform-api-latest
COPY production.conf /srv/app/production.conf
COPY production.xml /srv/app/production.xml
WORKDIR /srv/app
ENTRYPOINT ot-platform-api-latest/bin/ot-platform-api \
    -J-Xms2g \
    -J-Xmx7g \
    -J-server \
    -Dconfig.file=/srv/app/production.conf \
    -Dlogger.file=/srv/app/production.xml \
    -Dlogback.debug=true \
    -Dcom.sun.management.jmxremote \
    -Dcom.sun.management.jmxremote.port=31238 \
    -Dcom.sun.management.jmxremote.ssl=false \
    -Dcom.sun.management.jmxremote.authenticate=false \
