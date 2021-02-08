# Usage:
# make        # Show this help to know which commands can run
# make clean  # Will Clean the Environment of previous runs.
# make build # Will Build the GeoServer Docker Image for the Environment.
# make test  # Will Run the Suite test with teamengine.
# make all   # Will Run all the previous steps {clean build test}

.DEFAULT_GOAL := help
.PHONY = help
suite =
war_url = "https://build.geoserver.org/geoserver/master/geoserver-master-latest-war.zip"


help:
	@fgrep -h "##" $(MAKEFILE_LIST) | fgrep -v fgrep | sed -e 's/\\$$//' | sed -e 's/##//'

all: clean build test

clean: $(suite)		## Will Clean the Environment of previous runs.
	@echo Cleaning up previous runs
	docker-compose -f docker-compose.yml -f ./$(suite)/docker-compose.override.yml down --remove-orphans --rmi all -v
	docker-compose -f docker-compose.yml -f ./$(suite)/docker-compose.override.yml rm -vfs

build: $(suite)		## Will Build the GeoServer Docker Image for the Environment.
	@echo Build the GeoServer Docker Image
	docker-compose -f docker-compose.yml -f ./$(suite)/docker-compose.override.yml build --build-arg GEOSERVER_WEBAPP_SRC=${war_url} geoserver

test: $(suite)		## Will running the Suite test with teamengine.
	@echo running the Suite test with teamengine
	docker-compose -f docker-compose.yml -f ./$(suite)/docker-compose.override.yml up --force-recreate --exit-code-from teamengine teamengine
