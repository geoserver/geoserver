SETTING UP A DEMO SYNCH SETUP
-----------------------------

Setting up a minimal synchronisation demo requires setting up 3 GeoServer instances, all running
at different URLs, and three different PostGIS databases. In particular, 2 will represent
the two remote units to be synchronised, whilst the third will be central.

SETTING UP THE UNITS
--------------------

1) Setup a unit1 postgis database (assuming you have the postgis template db, otherwise go look
   for the setup from sql files in the PostGIS guide)

createdb unit1 -T template_postgis

2) Execute the unit-interactive.sql script in it:

psql unit1 -f /home/aaime/devel/gstrunk/src/community/gss/demo/unit-interactive.sql

3) Make a deep copy of the demo/unit data directory

cp -r unit /path/to/data_dir_folder/unit1

4) Hand modify the /path/to/data_dir_folder/unit1/workspaces/topp/synch so that the connection
   parameters work with the above database
   
5) Start GeoServer on port 8081 pointing GEOSERVER_DATA_DIR at the data directory just setup.
   (for dev testing from Eclipse I just use Start.java adding these JVM params:
    -DGEOSERVER_DATA_DIR=/home/aaime/devel/parkinfo/datadirs/unit1 -Djetty.port=8081)
    
6) Do all of the above again for Unit2. Just replace unit1 with unit2 in the process and
   start the second GeoServer on port 8082
   
SETTING UP CENTRAL
------------------

1) Setup a central postgis database (assuming you have the postgis template db, otherwise go look
   for the setup from sql files in the PostGIS guide)

createdb central -T template_postgis

2) Execute the central-interactive.sql script in it:

psql central -f /home/aaime/devel/gstrunk/src/community/gss/demo/central-interactive.sql

NOTE: the database assumes the two unit services are available at
http://localhost:8081/geoserver/ows
http://localhost:8082/geoserver/ows
Change the sql script accordingly if they were setup on a diffent machine/port/context

3) Make a deep copy of the demo/central data directory

cp -r central /path/to/data_dir_folder/central

4) Hand modify the /path/to/data_dir_folder/central/workspaces/topp/synch so that the connection
   parameters work with the above database
   
5) Start GeoServer on port 8080 pointing GEOSERVER_DATA_DIR at the data directory just setup.
   (for dev testing from Eclipse I just use Start.java adding these JVM params:
    -DGEOSERVER_DATA_DIR=/home/aaime/devel/parkinfo/datadirs/central -Djetty.port=8080)
    
TESTING
-------------

At this point Central should connect once every minutes to the two units and perform the synchronization.
The logs on both Central and Unit side should provide with hints about what's going on.  
The main remaining issue is how to edit the data... standard uDig cannot edit it because of the
UUID primary key usage, you need a build using a recent versioning datastore to deal with that
kind of primary key.