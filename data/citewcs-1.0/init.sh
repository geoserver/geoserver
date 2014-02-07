# wcs 1.0 can't run with wcs 1.1 or wcs 2.0 active
rm $1/webapps/geoserver/WEB-INF/lib/*wcs1_1-*.jar
rm $1/webapps/geoserver/WEB-INF/lib/*wcs2_0-*.jar
rm $1/webapps/geoserver/WEB-INF/lib/*web-wcs-*.jar
