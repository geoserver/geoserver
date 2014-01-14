GEOSERVER 2.5+ SERVICE EXTENSIONS README

This package contains a versioning WFS service implementation as well
as a versioning postgis datastore that is distributed as a separate plug-in.  
This plug-in is still experimental,if you have feedback please let us know.  See 
http://geoserver.org/display/GEOS/Versioning+WFS+-+Extensions for more info

Please report any bugs with jira (http://jira.codehaus.org/browse/GEOS). 

Any other issues can be discussed on the mailing list (http://lists.sourceforge.net/lists/listinfo/geoserver-users).

COMPATIBILITY

This jar should work with any version of GeoServer based on GeoTools 11.x  
Currently this is anything in 2.5.x.

INSTALLATION

1. get postgres and postgis
2. create a "spearfish" superuser with spearfish password: 
   createuser -s -d -P spearfish
3. create a "spearfish" database: 
   createdb spearfish -U spearfish
4. uncompress data_dir/spearfish.sql.gz
   gzip -d spearfish.sql.gz
5. restore the db:
   psql -U spearfish spearfish < spearfish.sql
6. copy all the jars in this extension to geoserver/WEB-INF/lib
7. run geoserver specifying a data dir pointing to the versioning data dir found
   in this demo. 
   If the versioning data dir has been unpacked to ${versioning_data_dir} (you'll
   substituite with your path), the following may work:
   - modify geoserver/WEB-INF/web.xml, uncomment the GEOSERVER_DATA_DIR
     and change it so that it reads:
     <context-param>
       <param-name>GEOSERVER_DATA_DIR</param-name>
        <param-value>${versiong_data_dir}</param-value>
    </context-param> 
   - set and environment variable pointing at the versioning data dir so that
     it enters the web container environment
       - SET GEOSERVER_DATA_DIR=${versiong_data_dir} (on windows)
       - EXPORT GEOSERVER_DATA_DIR=${versiong_data_dir} (on linux)
       
8. start geoserver, you should find only the spearfish data set, in versioning
   configuration. Also have a look at the sample requests for ideas on the
   versioning WFS protocol.
