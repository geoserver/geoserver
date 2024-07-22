# Application Schema Extension

## User Manual

* [Application Schema Support](https://docs.geotools.org/latest/userguide/extension/app-schema.html) (GeoTools User Guide)
* [Application schemas](https://docs.geoserver.org/latest/en/user/data/app-schema/index.html) (GeoServer User Manual)

  Contains more extensive examples


## Developer Guide

This extension is composed of a number of modules:

* app-schema-core

Integration tests that work offline:

* app-schema-test: integration test definition
* sample-data-acess-test: uses an offline geotools example to test functionality 

And a series of online integration test modules added using profile `app-schema-online-test`:

* app-schema-mongodb-test: requires test fixture in `~/.geoserver/mongodb.properties`
* app-schema-oracle-test: requires test fixture in `~/.geoserver/oracle.properties`
* app-schema-postgis-test: requires test fixture in `~/.geoserver/postgis.properties`
* app-schema-solr-test: requires test fixture in `~/.geoserver/solr.properties`

Reference:

* [App-Schema Online Tests](https://docs.geoserver.org/latest/en/developer/programming-guide/app-schema/index.html) (GeoServer Developer Manual)

