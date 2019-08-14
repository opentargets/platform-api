FROM openjdk:11
RUN mkdir -p /srv/app

COPY target/universal/ot-platform-api-beta-latest.zip /srv/app/ot-platform-api-beta-latest.zip
COPY production.conf /srv/app/production.conf

WORKDIR /srv/app
RUN unzip ot-platform-api-beta-latest.zip

RUN chmod +x ot-platform-api-beta-latest/bin/ot-platform-api-beta
ENTRYPOINT ot-platform-api-beta-latest/bin/ot-platform-api-beta -J-Xms2g -J-Xmx7g -J-server -Dconfig.file=/srv/app/production.conf
