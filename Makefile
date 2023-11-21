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

run_log: ## Runs API using the logback file specified in logfile eg: make run logfile=./conf/logback.xml
	@sbt run -Dlogback.configurationFile=${logfile}

debug_log: ## Debugs API using the logback file specified in logfile eg: make run logfile=./conf/logback.xml
	@sbt -jvm-debug 9999 run -Dlogback.configurationFile=${logfile}

es_tunnel: ## Create tunnel connection to ElasticSearch eg make es_tunnel zone europe-west1-d instance trnplt-es-0-esearch-fl6c
	@echo "Connecting to ElasticSearch"
	@gcloud beta compute ssh --zone "${zone}" ${instance} --tunnel-through-iap -- -L 9200:localhost:9200

ch_tunnel: ## Create tunnel connection to Clickhouse eg. make ch_tunnel zone europe-west1-d instance trnplt-es-0-esearch-fl6c
	@echo "Connecting to Clickhouse"
	@gcloud compute ssh ${instance} --zone="${zone}" --tunnel-through-iap -- -L 8123:localhost:8123

.PHONY: help run_local debug_local es_tunnel ch_tunnel