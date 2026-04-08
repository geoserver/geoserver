# postgis setup

curl -u admin:geoserver -XDELETE \
  "http://localhost:8080/geoserver/rest/workspaces/cite/datastores/postgis.json?recurse=true"

curl  -u admin:geoserver -XPOST -H "Content-type: application/json" \
  -d @postgis.json \
  "http://localhost:8080/geoserver/rest/workspaces/cite/datastores.json"

# Uploading a CSV file to PostGIS while transforming it
#
echo "csv transform 1"
curl -u admin:geoserver -XPOST -H "Content-type: application/json" \
  -d @import.json \
  "http://localhost:8080/geoserver/rest/imports"
sleep 2

echo "csv transform 2"
curl -u admin:geoserver -F name=test -F filedata=@values.csv \
  "http://localhost:8080/geoserver/rest/imports/0/tasks"
sleep 2  

echo "3 csv transform 3"
curl -u admin:geoserver -XPUT -H "Content-type: application/json" \
  -d @layerUpdate.json \
  "http://localhost:8080/geoserver/rest/imports/0/tasks/0/layer/"
sleep 2  

echo "csv transform 4"
curl -u admin:geoserver -XPOST -H "Content-type: application/json" \
  -d @toPoint.json \
  "http://localhost:8080/geoserver/rest/imports/0/tasks/0/transforms"
sleep 2

echo "csv transform 5"
curl -u admin:geoserver -XPOST \
  "http://localhost:8080/geoserver/rest/imports/0"

# Replacing PostGIS table using the contents of a CSV file
#
echo "csv replace 1"
curl -u admin:geoserver -XPOST -H "Content-type: application/json" \
  -d @import.json "http://localhost:8080/geoserver/rest/imports"
sleep 2

echo "csv replace 2"
curl -u admin:geoserver -XPOST \
  -F filedata=@replace.csv \
  "http://localhost:8080/geoserver/rest/imports/1/tasks"
sleep 2

echo "csv replace 3"
curl -u admin:geoserver -XPUT -H "Content-type: application/json" \
  -d @taskUpdate.json \
  "http://localhost:8080/geoserver/rest/imports/1/tasks/0"
  
sleep 2

echo "csv replace 4"
curl -u admin:geoserver -XPOST -H "Content-type: application/json" \
  -d @toPoint.json \
  "http://localhost:8080/geoserver/rest/imports/1/tasks/0/transforms"
  
sleep 2
echo "csv replace 5"
curl -u admin:geoserver -XGET \
  http://localhost:8080/geoserver/rest/imports/1.json | jq .

curl -u admin:geoserver -XGET \
  http://localhost:8080/geoserver/rest/imports/1/tasks/0.json | jq .

curl -u admin:geoserver -XGET \
  http://localhost:8080/geoserver/rest/imports/1/tasks/0/layer.json | jq .
  
curl -u admin:geoserver -XGET \
  http://localhost:8080/geoserver/rest/imports/1/tasks/0/transforms/0.json | jq .

echo "csv replace 6"
curl -u admin:geoserver -XPOST \
  "http://localhost:8080/geoserver/rest/imports/1"
  
