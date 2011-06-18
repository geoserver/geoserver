curl -u admin:geoserver -XPUT -H 'Content-type: text/xml' -d '<layer><defaultStyle><name>population</name></defaultStyle></layer>' http://localhost:8080/geoserver/rest/layers/topp:states
curl -u admin:geoserver -XPUT -H 'Content-type: text/xml' -d '<layer><defaultStyle><name>capitals</name></defaultStyle></layer>' http://localhost:8080/geoserver/rest/layers/topp:tasmania_cities
curl -u admin:geoserver -XPUT -H 'Content-type: text/xml' -d '<layer><defaultStyle><name>simple_roads</name></defaultStyle></layer>' http://localhost:8080/geoserver/rest/layers/topp:tasmania_roads
curl -u admin:geoserver -XPUT -H 'Content-type: text/xml' -d '<layer><defaultStyle><name>green</name></defaultStyle></layer>' http://localhost:8080/geoserver/rest/layers/topp:tasmania_state_boundaries
curl -u admin:geoserver -XPUT -H 'Content-type: text/xml' -d '<layer><defaultStyle><name>cite_lakes</name></defaultStyle></layer>' http://localhost:8080/geoserver/rest/layers/topp:tasmania_water_bodies

#tiger
curl -u admin:geoserver -XPUT -H 'Content-type: text/xml' -d '<layer><defaultStyle><name>giant_polygon</name></defaultStyle></layer>' http://localhost:8080/geoserver/rest/layers/tiger:giant_polygon
curl -u admin:geoserver -XPUT -H 'Content-type: text/xml' -d '<layer><defaultStyle><name>poi</name></defaultStyle></layer>' http://localhost:8080/geoserver/rest/layers/tiger:poi
curl -u admin:geoserver -XPUT -H 'Content-type: text/xml' -d '<layer><defaultStyle><name>poly_landmarks</name></defaultStyle></layer>' http://localhost:8080/geoserver/rest/layers/tiger:poly_landmarks
curl -u admin:geoserver -XPUT -H 'Content-type: text/xml' -d '<layer><defaultStyle><name>tiger_roads</name></defaultStyle></layer>' http://localhost:8080/geoserver/rest/layers/tiger:tiger_roads

#sf
curl -u admin:geoserver -XPUT -H 'Content-type: text/xml' -d '<layer><defaultStyle><name>capitals</name></defaultStyle></layer>' http://localhost:8080/geoserver/rest/layers/sf:bugsites
curl -u admin:geoserver -XPUT -H 'Content-type: text/xml' -d '<layer><defaultStyle><name>restricted</name></defaultStyle></layer>' http://localhost:8080/geoserver/rest/layers/sf:restricted
curl -u admin:geoserver -XPUT -H 'Content-type: text/xml' -d '<layer><defaultStyle><name>simple_roads</name></defaultStyle></layer>' http://localhost:8080/geoserver/rest/layers/sf:roads
curl -u admin:geoserver -XPUT -H 'Content-type: text/xml' -d '<layer><defaultStyle><name>simple_streams</name></defaultStyle></layer>' http://localhost:8080/geoserver/rest/layers/sf:streams
