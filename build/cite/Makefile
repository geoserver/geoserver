# Usage:
# make        # Show this help to know which commands can run
# make clean suite=<suite test>  # Will Clean the environment of previous runs of a specific suite.
# make build suite=<suite test>  # Will Build the GeoServer docker image of a specific suite.
# make suite=<suite test>        # Will Run the specific test suite via a teamengine container.

.DEFAULT_GOAL := help
.PHONY = help
suite =
war_url = "https://build.geoserver.org/geoserver/main/geoserver-main-latest-war.zip"


help:
	@fgrep -h "##" $(MAKEFILE_LIST) | fgrep -v fgrep | sed -e 's/\\$$//' | sed -e 's/##//'

clean:		## Will Clean the Environment of previous runs.
	@echo Cleaning up previous runs
	docker-compose down --remove-orphans --rmi all -v
	docker-compose rm -vfs
	@echo Cleanning the previous logs.
	rm -Rf logs/*

build: $(suite)		## Will Build the GeoServer Docker Image for the Environment.
	@echo Build the GeoServer Docker Image
	docker-compose -f docker-compose.yml -f ./$(suite)/docker-compose.override.yml \
      build --build-arg GEOSERVER_WEBAPP_SRC=${war_url} geoserver

version: $(suite)   ## Will give the version of the GeoServer on the current docker.
		@echo Getting the GeoServer version.
		docker run -i ogccite/geoserver:$(suite) bash -c 'cd /usr/local/tomcat/webapps/ && if [ -f geoserver.war ]; then unzip -q geoserver.war; cat META-INF/MANIFEST.MF; else cat META-INF/MANIFEST.MF; fi'

test: $(suite) | version		## Will running the Suite test with teamengine.
		@echo running the Suite test with teamengine
		docker-compose -f docker-compose.yml -f ./$(suite)/docker-compose.override.yml \
      up --force-recreate --exit-code-from teamengine teamengine

webUI:		## Will running the Suite test with teamengine.
	@echo running the Suite test with teamengine webUI
	docker-compose -f docker-compose.yml -f ./interactive/docker-compose.override.yml \
      up --force-recreate --no-deps --exit-code-from teamengine teamengine
