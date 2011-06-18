curl -u admin:geoserver -XPOST -d '<namespace><prefix>sf</prefix><uri>http://www.openplans.org/spearfish</uri></namespace>' -H 'Content-type: text/xml' http://localhost:8080/geoserver/rest/namespaces
curl -u admin:geoserver -XPOST -d '<namespace><prefix>sde</prefix><uri>http://www.geoserver.sf.net</uri></namespace>' -H 'Content-type: text/xml' http://localhost:8080/geoserver/rest/namespaces
curl -u admin:geoserver -XPOST -d '<namespace><prefix>nurc</prefix><uri>http://www.nurc.nato.int</uri></namespace>' -H 'Content-type: text/xml' http://localhost:8080/geoserver/rest/namespaces
curl -u admin:geoserver -XPOST -d '<namespace><prefix>tiger</prefix><uri>http://www.cencus.gov</uri></namespace>' -H 'Content-type: text/xml' http://localhost:8080/geoserver/rest/namespaces
curl -u admin:geoserver -XPOST -d '<namespace><prefix>cite</prefix><uri>http://www.opengeospatial.org/cite</uri></namespace>' -H 'Content-type: text/xml' http://localhost:8080/geoserver/rest/namespaces
