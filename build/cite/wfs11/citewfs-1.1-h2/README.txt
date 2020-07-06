WFS 1.1 H2 Cite Testing README
------------------------------

0. Download the H2 Database from 'http://www.h2database.com' and install it 
   somewhere on your system.

1. Open 'h2.sh': 

   1. Edit the 'H2' variable to point to your H2 installation
   2. Edit the 'M2_REPO' variable to point to your local Maven repository
   3. Edit the 'GT_VERSION' variable to the appropriate GeoTools version for the geoserver release

2. From the command line run 'h2.sh load'

3. Run GeoServer with the 'citewfs-1.1-h2' configuration and the h2 profile, like in:
mvn jetty:run -DGEOSERVER_DATA_DIR=<path to the citewfs-1.1-h2 config> -Ph2.

