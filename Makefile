logfile:=""

# This Makefile helper automates bringing up and tearing down a local deployment of Open Targets Platform
 .DEFAULT_GOAL := help

# Targets
help: ## Show this help message
	@awk 'BEGIN {FS = ":.*##"; printf "\nUsage:\n  make <target>\n"} /^[a-zA-Z_-]+:.*?##/ { printf "  %-28s %s\n", $$1, $$2 }' $(MAKEFILE_LIST)

run: ## Runs API
	@sbt run

debug: ## Debugs API
	@sbt -jvm-debug 9999 run

debug_with_standalone: ## Debugs API
	@sbt -jvm-debug 9999 "run 8090" -DPLATFORM_API_IGNORE_CACHE=true

run_log: ## Runs API using the logback file specified in logfile eg: make run_log logfile=./conf/logback.xml
	@sbt run -Dlogback.configurationFile=${logfile}

debug_log: ## Debugs API using the logback file specified in logfile eg: make debug_log logfile=./conf/logback.xml
	@sbt -jvm-debug 9999 run -Dlogback.configurationFile=${logfile}

debug_log_standalone: ## Debugs API using the logback file specified in logfile eg: make debug_log_standalone logfile=./conf/logback.xml
	@sbt -jvm-debug 9999 "run 8090" -Dlogback.configurationFile=${logfile}

es_tunnel: ## Create tunnel connection to ElasticSearch eg make es_tunnel zone europe-west1-d instance trnplt-es-0-esearch-fl6c
	@echo "Connecting to ElasticSearch"
	@gcloud compute ssh --zone "${zone}" ${instance} --tunnel-through-iap -- -L 9200:localhost:9200

ch_tunnel: ## Create tunnel connection to Clickhouse eg. make ch_tunnel zone europe-west1-d instance trnplt-es-0-esearch-fl6c
	@echo "Connecting to Clickhouse"
	@gcloud compute ssh ${instance} --zone="${zone}" --tunnel-through-iap -- -L 8123:localhost:8123

run_with_standalone: ## Runs API with standalone platform
	@sbt "run 8090" -J-Xms2g -J-Xmx7g -J-XX:+UseG1GC -DPLATFORM_API_IGNORE_CACHE=true

debug_with_standalone: ## Runs API with standalone platform
	@sbt -jvm-debug 9999 run 8090 -DSLICK_CLICKHOUSE_URL=jdbc:clickhouse://clickhouse:8123 -DPLATFORM_API_IGNORE_CACHE=true

.PHONY: help run_local debug_local es_tunnel ch_tunnel run_with_standalone