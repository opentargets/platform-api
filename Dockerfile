FROM eclipse-temurin:21.0.4_7-jdk-alpine
RUN mkdir -p /srv/app

RUN apk add --no-cache bash alpine-sdk

COPY target/universal/ot-platform-api-latest.zip /srv/app/ot-platform-api-latest.zip
COPY production.xml /srv/app/production.xml
WORKDIR /srv/app
RUN unzip ot-platform-api-latest.zip

RUN chmod +x ot-platform-api-latest/bin/ot-platform-api

ENTRYPOINT ["bash", "-c", "ot-platform-api-latest/bin/ot-platform-api \
    -J-Xms2g \
    -J-Xmx7g \
    -J-server \
    -Dlogger.file=/srv/app/production.xml \
    -Dlogback.debug=true \
    -Dcom.sun.management.jmxremote \
    -Dcom.sun.management.jmxremote.port=31238 \
    -Dcom.sun.management.jmxremote.ssl=false \
    -Dcom.sun.management.jmxremote.authenticate=false"]