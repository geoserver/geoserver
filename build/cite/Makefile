# Usage:
# make        # Show this help to know which commands can run
# make clean suite=<suite test>  # Will Clean the environment of previous runs of a specific suite.
# make build suite=<suite test>  # Will Build the GeoServer docker image of a specific suite.
# make suite=<suite test>        # Will Run the specific test suite via a teamengine container.

.DEFAULT_GOAL := help
.PHONY = help
suite =
# `make war` will build the war from ../src/web/app and copy it here
war_url = ./geoserver/geoserver.war

# alternatively you can use one in .war or .zip format from an URL, for example:
# war_url = "https://build.geoserver.org/geoserver/main/geoserver-main-latest-war.zip"
# but it may not have all the required extensions so make sure you know what you're doing


.PHONY: help
help:
	@echo Usage:
	@echo
	@echo \# Main targets in suggested order:
	@echo
	@grep -h '\s##\s' $(MAKEFILE_LIST) | fgrep -v fgrep | sed -e 's/\\$$//' | sed -e 's/##//'
	@echo
	@echo \# Additional helper targets:
	@echo
	@grep -h '\s###\s' $(MAKEFILE_LIST) | fgrep -v fgrep | sed -e 's/\\$$//' | sed -e 's/###//'

.PHONY: war
war:	## 					Build the geoserver.war file to use for testing and place it in ./geoserver/geoserver.war
	@$(MAKE) build-war
	@$(MAKE) copy-war

# calls build-ets-image and build-geoserver-image
# the CI job may call them independently for the sake of using separate job steps
.PHONY: build
build: ##	suite=<suite>				Build the GeoServer Docker Image for the Environment.
ifndef suite
	@echo "Error: suite is not specified. Please specify the suite to build the GeoServer Docker image for."
	@$(MAKE) help
	@exit 1
endif
	@$(MAKE) build-ets-image
	@$(MAKE) build-geoserver-image

# preparing the geoserver image is performed with docker-compose build including $suite/compose-override.yml
.PHONY: build-geoserver-image
build-geoserver-image:
ifndef suite
	@echo "Error: suite is not specified. Please specify the suite to build the GeoServer Docker image for."
	@exit 1
endif
ifndef java-version
	@echo "Error: java-version is not specified. Please specify the Java version to build the GeoServer Docker image for."
	@exit 1
endif
	@echo Building the GeoServer Docker Image for the $(suite) test suite with Java $(java-version)
	@JAVA_VERSION=$(java-version) docker compose -f compose.yml -f ./$(suite)/compose.override.yml \
	  build --no-cache --build-arg SOURCE_FILE=${war_url} geoserver

# If there's a $suite/build-ets.sh file calls it, otherwise exists silently
.PHONY: build-ets-image
build-ets-image:
ifndef suite
	@echo "Error: suite is not specified. Please specify the suite to build the GeoServer Docker image for."
	@exit 1
endif
	@if [ -f "$(suite)/build-ets.sh" ]; then \
		(cd $(suite) && ./build-ets.sh); \
	else \
		echo "$(suite) has no special script to build the ETS docker image."; \
	fi

.PHONY: geoserver-version
geoserver-version: ##	Print the version of the GeoServer on the current docker.
	@echo Getting the full GeoServer/GeoWebCache/Geotools version.
	@curl -u admin:geoserver "http://localhost:18090/geoserver/rest/about/version.xml"
	@echo "" ## newline

.PHONY: test
test: ##	suite=<suite>				Run the Test Suite with teamengine and GeoServer on docker compose.
ifndef suite
	@echo "Error: suite is not specified. Please specify a suite to run the tests."
	@$(MAKE) help
	@exit 1
endif
ifneq (,$(filter $(suite),ogcapi-features10 ogcapi-tiles10 wms11 wms13 wmts10 wfs10 wfs20 wcs11 wcs20 geotiff11 gpkg12))
	@$(MAKE) test-rest suite=$(suite)
else
	@$(MAKE) test-cli suite=$(suite)
endif


.PHONY: clean
clean:	## 					Clean the Environment of previous runs.
	@$(MAKE) stop
	@$(MAKE) cleanlogs


.PHONY: test-localhost
test-localhost: ### suite=<suite>			Run the Test Suite against a local host GeoServer instance
ifndef suite
	@echo "Error: suite is not specified. Please specify a suite to run the tests."
	@$(MAKE) help
	@exit 1
endif
# for the time being the only suite run through teamengine's rest api is ogcapi-features10 
ifneq ($(suite), ogcapi-features10)
	@echo "Error: This target only supports the 'ogcapi-features10' suite."
	@exit 1
endif
	@if [ "$(shell uname)" = "Darwin" ]; then \
		echo "Detected a MacOS host Operating System. Using host.docker.internal as the host name"; \
		iut="http://host.docker.internal:8080/geoserver/ogc/features/v1"; \
	else \
		echo "Detected a Linux host Operating System. Using 172.17.0.1 as the host IP address"; \
		iut="http://172.17.0.1:8080/geoserver/ogc/features/v1"; \
	fi; \
	$(MAKE) test-external suite=$(suite) iut=$$iut

# This target runs the specified test suite against a GeoServer instance provided by the iut parameter.
# It performs the following actions:
# 1. Checks if the required 'suite' and 'iut' parameters are provided, displaying an error and help message if not.
# 2. Validates the 'iut' parameter based on the operating system:
#    - On MacOS, it prints an error and exits if 'iut' contains '172.17.0.1'.
#    - On Linux, it prints an error and exits if 'iut' contains 'host.docker.internal'.
# 3. Runs either 'test-rest' or 'test-cli' based on the specified suite.
.PHONY: test-external
test-external: ### suite=<suite> iut=<landing URL>	Run the Test Suite against a GeoServer instance at a provided URL 
ifndef suite
	@echo "Error: suite is not specified. Please specify a suite to run the tests."
	@$(MAKE) help
	@exit 1
endif
ifndef iut
	@echo "** Error: iut is not specified. Specify the iut URL. e.g., make test-external suite=ogcapi-features10 iut=http://test.example.com/geoserver/ogc/features/v1"
	@$(MAKE) help
	@exit 1
endif
	@if [ "$(shell uname)" = "Darwin" ] && echo "$(iut)" | grep -q "172.17.0.1"; then \
		echo "Error: Using '172.17.0.1' as the iut value is not supported on MacOS. Please use 'host.docker.internal'."; \
		exit 1; \
	fi
	@if [ "$(shell uname)" = "Linux" ] && echo "$(iut)" | grep -q "host.docker.internal"; then \
		echo "Error: Using 'host.docker.internal' as the iut value is not supported on Linux. Please use '172.17.0.1'."; \
		exit 1; \
	fi
ifeq ($(filter $(suite), ogcapi-features10 ogcapi-tiles10),$(suite))
	@echo "Running REST-based tests for suite: $(suite) with iut: $(iut)"
	@$(MAKE) test-rest suite=$(suite) iut=$(iut)
else
	@echo "Running CLI-based tests for suite: $(suite)"
	@$(MAKE) test-cli suite=$(suite)
endif


.PHONY: ogcapi-features10-localhost
ogcapi-features10-localhost: ###			Shortcut for make test-localhost suite=ogcapi-features10
	@$(MAKE) test-localhost suite=ogcapi-features10


.PHONY: cleanlogs
cleanlogs:
	@echo Cleanning the previous logs.
	@rm -Rf logs/*


.PHONY: test-cli
test-cli: $(suite)
	@echo Running the $(suite) test suite with teamengine CLI
	@docker compose -f compose.yml -f ./$(suite)/compose.override.yml \
	  up --force-recreate --exit-code-from teamengine teamengine

.PHONY: test-rest
test-rest: $(suite)
ifdef iut
	@echo "Running the $(suite) test suite with the teamengine REST API against $(iut)"
	@$(MAKE) start suite=$(suite) services=teamengine
	@$(MAKE) run suite=$(suite) iut=$(iut)
else
	@echo "Running the $(suite) test suite with the teamengine REST API"
	@$(MAKE) start suite=$(suite)
	@$(MAKE) run suite=$(suite)
endif
	@$(MAKE) validate-rest-results

# Starts service containers
# Usage: make start suite=<suite> [services=<services>]
# <services>: if not given, starts all the services in the docker composition for <suite>,
# otherwise just the specified ones (e.g. make start suite=ogcapi-features10 services=teamengine)
.PHONY: start
start: ### suite=<suite> [services=<s1 s2..>]	Start the docker composition for suite. Optionally limit which services.
ifndef suite
	@echo "Error: suite is not specified. Please specify a suite to start the containers."
	@echo "Usage: make start suite=<suite> [services=<services>]"
	@exit 1
endif
	@echo Starting containers
	@if [ -n "$(services)" ]; then \
		echo "Starting services: $(services)"; \
		docker compose -f compose.yml -f ./$(suite)/compose.override.yml \
			up -d --force-recreate --no-deps $(services); \
	else \
		docker compose -f compose.yml -f ./$(suite)/compose.override.yml \
			up -d --force-recreate; \
	fi

.PHONY: stop
stop: ###						Shuts down the docker composition. Deos not remove logs/
	@echo Cleaning up previous runs
	@docker compose down --remove-orphans -v
	@docker compose rm -vfs


.PHONY: run
run: $(suite)
ifndef iut
	@$(MAKE) wait-for url="http://localhost:18090/geoserver/web/wicket/resource/org.geoserver.web.GeoServerBasePage/img/logo.png" timeout=60
endif
	@$(MAKE) wait-for url="http://localhost:18080/teamengine/site/logo.png" timeout=60
	@docker compose -f compose.yml -f ./$(suite)/compose.override.yml \
	  exec teamengine /bin/bash -c "/run-test.sh $(suite) $(iut)"


# first prints out a human readable report of test failures, if xmlstarlet is installed,
# and then a summary of passed and failed test counts. This last step will result in a
# non zero exit code if there are test failures.
.PHONY: validate-rest-results
validate-rest-results: $(suite)
	@if [ -f "logs/testng-results.xml" ]; then \
		./testng-results-report.sh logs/testng-results.xml; \
		./testng-results-validate.sh logs/testng-results.xml; \
    elif [ -f "logs/xml-results.xml" ]; then \
		./xml-results-report.sh logs/xml-results.xml; \
		./xml-results-validate.sh logs/xml-results.xml; \
    else \
        @result=0; \
        for file in logs/*-results.xml; do \
        if [ -f "$$file" ]; then \
            case "$$file" in \
            *testng-results.xml) \
                ./testng-results-report.sh "$$file"; \
                ./testng-results-validate.sh "$$file" || result=1; \
                ;; \
            *xml-results.xml) \
                ./xml-results-report.sh "$$file"; \
                ./xml-results-validate.sh "$$file" || result=1; \
                ;; \
            esac; \
        fi; \
        done; \
        exit $$result; \
    fi

.PHONY: print-full-failures
print-full-failures:
	@if [ -f "logs/testng-results.xml" ]; then \
		./testng-results-full-error-report.sh logs/testng-results.xml; \
    elif [ -f "logs/xml-results.xml" ]; then \
		./xml-results-full-error-report.sh logs/xml-results.xml; \
	else \
		echo "The files logs/testng-results.xml and logs/xml-results.xml do not exist. Skipping the report generation."; \
	fi

.PHONY: pull
pull: $(suite)
	@echo starting containers
	@docker compose -f compose.yml -f ./$(suite)/compose.override.yml \
		pull --ignore-buildable

.PHONY: build-war
build-war:
	# a single mvn package -pl :gs-web-app -am should work but it seems it doesn't take into account test dependencies
	# so it fails when the version number changes (e.g. 2.28-SNAPSHOT -> 3.0-SNAPSHOT)
	@mvn clean install -f ../../src/pom.xml \
	-Pogcapi-features,ogcapi-tiles,geopkg-output,app-schema,wcs10,wcs11 \
	-DskipTests  -B -ntp -U -T1C
	@mvn package -f ../../src/pom.xml \
	-Pogcapi-features,ogcapi-tiles,geopkg-output,wcs10,wcs11 \
	-DskipTests -B -nsu -ntp -T1C -pl :gs-web-app -am

.PHONY: copy-war
copy-war:
	cp ../../src/web/app/target/geoserver.war geoserver/geoserver.war

# Waits for an url to be available (returning HTTP status code 200 or 302)
# Usage: make wait-for url=<URL> timeout=<seconds>
.PHONY: wait-for
wait-for:
	@if [ -z "$(url)" ]; then \
		echo "Error: url parameter is required. Usage: make wait-for url=<URL> timeout=<seconds>"; \
		exit 1; \
	fi
	@if [ -z "$(timeout)" ]; then \
		echo "Error: timeout parameter is required. Usage: make wait-for url=<URL> timeout=<seconds>"; \
		exit 1; \
	fi
	@echo "Waiting for $(url) to be available (timeout: $(timeout) seconds)..."
	@start_time=$$(date +%s); \
	while true; do \
		elapsed_time=$$(( $$(date +%s) - $$start_time )); \
		if [ $$elapsed_time -ge $(timeout) ]; then \
			echo "Timeout reached: $(url) is not available after $(timeout) seconds"; \
			exit 1; \
		fi; \
		if curl -s -o /dev/null -w "%{http_code}" $(url) | grep -E "^(200|302)$$" > /dev/null; then \
			echo "$(url) is available!"; \
			break; \
		fi; \
		echo "Waiting for $(url)..."; \
		sleep 5; \
	done

# Prints logs for the specified suite and optionally a specific service.
# Usage:
#   make print-logs suite=<suite-name> [service=<service-name>]
# If 'service' is not specified, logs for all services in the suite will be shown.
.PHONY: print-logs
print-logs:
	@if [ -z "$(suite)" ]; then \
		echo "Error: suite variable must be provided. Usage: make print-logs suite=<suite-name> [service=<service-name>]"; \
		exit 1; \
	fi
	@if [ -n "$(service)" ]; then \
		docker compose -f compose.yml -f ./$(suite)/compose.override.yml logs $(service); \
	else \
		docker compose -f compose.yml -f ./$(suite)/compose.override.yml logs; \
	fi

# Used in CI/CD to print which services and images are going to be run
# Requires `jq`, comes pre-installed in github actions `ubuntu-latest`
.PHONY: print-services
print-services: ### suite=<suite>			Print the service names and docker images used for a given suite
ifndef suite
	@echo "Error: suite is not specified."
	@exit 1
endif
	@if ! command -v jq &> /dev/null; then \
		echo "jq is not installed. Please install jq to use this command."; \
	else \
		echo Services used in the $(suite) test suite:; \
		docker compose -f compose.yml -f $(suite)/compose.override.yml config --format=json | jq -r '.services | to_entries | map("\t\(.key):\n  \t\timage: \(.value.image)")[]'; \
	fi

.PHONY: webUI
webUI: ###						Start teamengine in interactive mode for the OWS services (excludes ogcapi services).
	@echo running the Suite test with teamengine webUI
	@docker compose -f compose.yml -f ./interactive/compose.override.yml \
	up --force-recreate --no-deps --exit-code-from teamengine teamengine

