# ------------------------------------------------------------------------ BUILD
FROM eclipse-temurin:21.0.4_7-jdk-jammy AS builder

WORKDIR /build

RUN apt-get update && \
    apt-get install -y --no-install-recommends unzip && \
    rm -rf /var/lib/apt/lists/*

COPY target/universal/ot-platform-api-latest.zip .

RUN unzip ot-platform-api-latest.zip && \
    chmod +x ot-platform-api-latest/bin/ot-platform-api

# ---------------------------------------------------------------------- RUNTIME
FROM eclipse-temurin:21.0.4_7-jre-jammy

ARG USER_ID=10001
ARG GROUP_ID=10001

RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/* && \
    groupadd -g ${GROUP_ID} otapi && \
    useradd -u ${USER_ID} -g otapi -r -s /sbin/nologin otapi

WORKDIR /srv/app

COPY --from=builder --chown=otapi:otapi /build/ot-platform-api-latest ./ot-platform-api-latest
COPY --chown=otapi:otapi production.xml .

USER otapi

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -sf http://localhost:8080/health || exit 1

ENTRYPOINT ["ot-platform-api-latest/bin/ot-platform-api"]

CMD [ \
    "-J-server", \
    "-Dlogger.file=/srv/app/production.xml" \
]
