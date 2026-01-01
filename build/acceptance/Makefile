
java-version ?= 17

.PHONY: help
help: ## Display this help message
	@echo "Usage: make <target>"
	@echo
	@echo "Available targets:"
	@grep --extended-regexp --no-filename '^[a-zA-Z_.-]+:.*## ' $(MAKEFILE_LIST) | sort | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "	%-20s%s\n", $$1, $$2}'

.PHONY: war
war: ## Build the geoserver.war file to use for testing and place it in ./geoserver/geoserver.war
	@$(MAKE) build-war
	@$(MAKE) copy-war

.PHONY: build-war
build-war:
	# a single mvn package -pl :gs-web-app -am should work but it seems it doesn't take into account test dependencies
	# so it fails when the version number changes (e.g. 2.28-SNAPSHOT -> 3.0-SNAPSHOT)
	@mvn clean install -f ../../src/pom.xml \
	-DskipTests -Dspotless.apply.skip=true -Parcgrid,image -B -ntp -U -T2C
	@mvn package -f ../../src/pom.xml \
	-DskipTests -Dspotless.apply.skip=true -Parcgrid,image -B -nsu -ntp -T2C -pl :gs-web-app -am

.PHONY: copy-war
copy-war:
	cp ../../src/web/app/target/geoserver.war geoserver/geoserver.war

geoserver/sampledata.tgz: .venv ## Prepare the GeoServer sample data archive
	.venv/bin/copy-test-data geoserver/

geoserver/.image_built_$(java-version): geoserver/sampledata.tgz geoserver/geoserver.war
	@echo Building the GeoServer Docker Image with Java $(java-version)
	@JAVA_VERSION=$(java-version) docker compose -f compose.yml build --no-cache geoserver
	@rm -f geoserver/.image_built_*
	@touch geoserver/.image_built_$(java-version)

.PHONY: build-geoserver-image
build-geoserver-image: geoserver/.image_built_$(java-version)
	@echo "GeoServer Docker image for Java $(java-version) is up to date."

.PHONY: up
up: build-geoserver-image ## Start the GeoServer Docker container
	@echo Starting the GeoServer Docker container
	@docker compose -f compose.yml \
	  up -d --wait

.venv: ## Create a virtual environment for acceptance tests and install dependencies
	@echo Creating and setting up the Python virtual environment for acceptance tests
	@python3 -m venv .venv
	@.venv/bin/pip install --upgrade pip setuptools wheel
	@.venv/bin/pip install -r requirements.txt

.PHONY: test
test: up .venv ## Run acceptance tests
	GEOSERVER_ACCEPTANCE_CONFIG=./config.yaml .venv/bin/pytest --pyargs geoserver_acceptance_tests -vv --color=yes

.PHONY: geoserver-version
geoserver-version: ##	Print the version of the GeoServer on the current docker.
	@echo Getting the full GeoServer/GeoWebCache/Geotools version.
	@curl -u admin:geoserver "http://localhost:19090/geoserver/rest/about/version.xml"
	@echo "" ## newline

.PHONY: down
down: ## Stop the GeoServer Docker container
	@echo Stopping the GeoServer Docker container
	@docker compose -f compose.yml \
	  down -v --remove-orphans
