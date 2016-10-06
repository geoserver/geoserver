In order to run the tests in this module you need the geofence
webtests module running with a specific set of data.

To get there:

cd your_geofence_repo/src/services/core/webtest/
mvn jetty:run

At this point you should have the geofence test services running on port
9191 and the tests for GeoFenceAccessManager should be working.

If geofence webtest is not running, tests will be automatically skipped.