FROM openjdk:11
RUN mkdir -p /srv/app

COPY target/universal/ot-platform-api-latest.zip /srv/app/ot-platform-api-latest.zip
COPY production.conf /srv/app/production.conf
COPY production.xml /srv/app/production.xml
WORKDIR /srv/app
RUN unzip ot-platform-api-latest.zip

RUN chmod +x ot-platform-api-latest/bin/ot-platform-api
ENTRYPOINT ot-platform-api-latest/bin/ot-platform-api -J-Xms2g -J-Xmx7g -J-server -Dconfig.file=/srv/app/production.conf -Dlogger.file=/srv/app/production.xml -Dlogback.debug=true
