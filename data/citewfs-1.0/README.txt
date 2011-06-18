There are complete running instructions here:
http://geoserver.org/display/GEOS/Running+Cite+Tests

Here's the summary:
1. get postgis 1.0, make a "cite" super-user
2. run "dropdb cite -U cite" to delete the database (always do this before you run!)
3. run the postgis double-click install and create a "cite" database
4. run "psql -f cite_data.sql cite -U cite"
5. set the build.properties file so it has the line "test.type=citeWFSPostGIS"
6. run geoserver 
7. setup and run a cite test http://cite.occamlab.com/tsOGC/interface/MainMenu

Running tests takes about an hour on my machine with 50kb/s upload speed.
