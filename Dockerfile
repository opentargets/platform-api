FROM eclipse-temurin:21.0.4_7-jdk-alpine

ARG USER_ID=10001
ARG GROUP_ID=10001
ARG USER_NAME=nginxuser

RUN addgroup -g ${GROUP_ID} -S otapigroup && \
    adduser -u ${USER_ID} -D -S -G otapigroup ${USER_NAME}

RUN mkdir -p /srv/app

RUN apk add --no-cache bash alpine-sdk

COPY target/universal/ot-platform-api-latest.zip /srv/app/ot-platform-api-latest.zip
COPY production.xml /srv/app/production.xml

RUN chown -R ${USER_NAME}:otapigroup /srv/app

USER ${USER_NAME}

WORKDIR /srv/app

RUN unzip ot-platform-api-latest.zip

RUN chmod +x ot-platform-api-latest/bin/ot-platform-api

ENTRYPOINT ["bash", "-c", "ot-platform-api-latest/bin/ot-platform-api \
    ${JVM_XMS:+-J-Xms${JVM_XMS}} \
    ${JVM_XMX:+-J-Xmx${JVM_XMX}} \
    -J-server \
    -Dlogger.file=/srv/app/production.xml \
    -Dlogback.debug=true \
    -Dcom.sun.management.jmxremote \
    -Dcom.sun.management.jmxremote.port=31238 \
    -Dcom.sun.management.jmxremote.ssl=false \
    -Dcom.sun.management.jmxremote.authenticate=false"]
