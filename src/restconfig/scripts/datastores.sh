#topp
curl -u admin:geoserver -XPUT -H 'Content-type: application/zip' --data-binary @tasmania_cities.zip http://localhost:8080/geoserver/rest/workspaces/topp/datastores/tasmania_cities/file.shp
curl -u admin:geoserver -XPUT -H 'Content-type: application/zip' --data-binary @tasmania_roads.zip http://localhost:8080/geoserver/rest/workspaces/topp/datastores/tasmania_roads/file.shp
curl -u admin:geoserver -XPUT -H 'Content-type: application/zip' --data-binary @tasmania_state_boundaries.zip http://localhost:8080/geoserver/rest/workspaces/topp/datastores/tasmania_state_boundaries/file.shp
curl -u admin:geoserver -XPUT -H 'Content-type: application/zip' --data-binary @tasmania_water_bodies.zip http://localhost:8080/geoserver/rest/workspaces/topp/datastores/tasmania_water_bodies/file.shp
curl -u admin:geoserver -XPUT -H 'Content-type: application/zip' --data-binary @states.zip http://localhost:8080/geoserver/rest/workspaces/topp/datastores/states_shapefile/file.shp

#sf
curl -u admin:geoserver -XPUT -H 'Content-type: application/zip' --data-binary @roads.zip http://localhost:8080/geoserver/rest/workspaces/sf/datastores/sfRoads/file.shp
curl -u admin:geoserver -XPUT -H 'Content-type: application/zip' --data-binary @archsites.zip http://localhost:8080/geoserver/rest/workspaces/sf/datastores/sfArchsites/file.shp
curl -u admin:geoserver -XPUT -H 'Content-type: application/zip' --data-binary @bugsites.zip http://localhost:8080/geoserver/rest/workspaces/sf/datastores/sfBugsites/file.shp
curl -u admin:geoserver -XPUT -H 'Content-type: application/zip' --data-binary @streams.zip http://localhost:8080/geoserver/rest/workspaces/sf/datastores/sfStreams/file.shp
curl -u admin:geoserver -XPUT -H 'Content-type: application/zip' --data-binary @restricted.zip http://localhost:8080/geoserver/rest/workspaces/sf/datastores/sfRestricted/file.shp

#tiger
curl -u admin:geoserver -XPUT -H 'Content-type: application/zip' --data-binary @tiger_roads.zip http://localhost:8080/geoserver/rest/workspaces/tiger/datastores/DS_tiger_roads/file.shp
curl -u admin:geoserver -XPUT -H 'Content-type: application/zip' --data-binary @poi.zip http://localhost:8080/geoserver/rest/workspaces/tiger/datastores/DS_poi/file.shp
curl -u admin:geoserver -XPUT -H 'Content-type: application/zip' --data-binary @giant_polygon.zip http://localhost:8080/geoserver/rest/workspaces/tiger/datastores/DS_giant_polygon/file.shp
curl -u admin:geoserver -XPUT -H 'Content-type: application/zip' --data-binary @poly_landmarks.zip http://localhost:8080/geoserver/rest/workspaces/tiger/datastores/DS_poly_landmarks/file.shp
