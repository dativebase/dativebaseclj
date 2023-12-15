SHELL := /bin/bash

db:  ## Connect to DativeBase db via psql
	@docker compose exec --user=postgres postgres psql dativebase

.spectral.yaml:  ## Generate .spectral.yaml (the spectral OpenAPI ruleset) and write it to ./.spectral.yaml.
	@echo 'extends: ["spectral:oas"]' > .spectral.yaml

lint-openapi: .spectral.yaml  ## Lint the OpenAPI API specification file at resources/public/openapi/api.yaml
	@spectral lint resources/public/openapi/api.yaml

openapi:  ## Construct the OpenAPI YAML from the OpenAPI EDN source
	@clj -X:openapi

install-swagger-ui:  ## Clone the swagger-ui source and update it to point to our OpenAPI YAML file under resources/public/
	@if [ -d "resources/public/swagger-ui" ]; then echo "The swagger-ui source has already been cloned."; else git clone git@github.com:swagger-api/swagger-ui.git resources/public/swagger-ui; fi
	@sed -i'.bak' 's/https:\/\/petstore.swagger.io\/v2\/swagger.json/\/openapi\/api.yaml/g' resources/public/swagger-ui/dist/swagger-initializer.js
	@echo "The swagger-ui should now be configured for resources/public/openapi/api.yaml."

schema.sql: export CONTAINER ?= dativebaseclj-postgres-1
schema.sql: DATABASE ?= dativebase
schema.sql: PGUSER ?= postgres
schema.sql: PG_DUMP_COMMAND ?= "docker exec ${CONTAINER} pg_dump"
schema.sql: migrator/sql/*  ## Dump the db schema to schema.sql (iff there are migrations newer than existing schema)
	@set -e; \
		eval $(PG_DUMP_COMMAND) \
			--schema-only \
			--no-privileges \
			--no-owner \
			--username="${PGUSER}" \
			"${DATABASE}" | \
			grep --invert-match "\-\-" | \
			tr -d '\r' | \
			cat -s > \
			schema.sql

run:  ## Run the application
	@clj -X:run

tests:  ## Run the tests
	@clj -M:test -m kaocha.runner

help:  ## Print this help message.
	@grep -E '^[a-zA-Z_\.-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

.DEFAULT_GOAL := help

