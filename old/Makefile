SHELL := /bin/bash

lint-kondo:  ## Lint the OLD Clojure code using clj-kondo
	@clj-kondo --lint src:test

db:  ## Connect to the OLD DB via mysql
	@docker-compose exec mysql -u admin -p db

.spectral.yaml:  ## Generate .spectral.yaml (the spectral OpenAPI ruleset) and write it to ./.spectral.yaml.
	@echo 'extends: ["spectral:oas"]' > .spectral.yaml

lint-openapi: .spectral.yaml  ## Lint the OpenAPI API specification file at resources/public/openapi/api.yaml
	@spectral lint resources/public/openapi/api.yaml

install-swagger-ui:  ## Clone the swagger-ui source and update it to point to our OpenAPI YAML file under resources/public/
	@if [ -d "resources/public/swagger-ui" ]; then echo "The swagger-ui source has already been cloned."; else git clone git@github.com:swagger-api/swagger-ui.git resources/public/swagger-ui; fi
	@sed -i'.bak' 's/https:\/\/petstore.swagger.io\/v2\/swagger.json/\/openapi\/api.yaml/g' resources/public/swagger-ui/dist/swagger-initializer.js
	@echo "The swagger-ui should now be configured for resources/public/openapi/api.yaml."

help:  ## Print this help message.
	@grep -E '^[a-zA-Z_\.-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

.DEFAULT_GOAL := help

