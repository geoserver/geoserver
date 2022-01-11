Java Service Wrapper package for GeoServer
------------------------------------------

This package allows GeoServer to be installed as a service under
Windows NT/2000/XP. The 

The original wrapper package for Jetty was downloaded from Argosy
TelCrest (http://www.argosytelcrest.co.uk/kb/epskb_00003.html)

It uses the Java Service Wrapper from Tanuki Software
(http://wrapper.tanukisoftware.org)

The license for Java Service Wrapper is included in wrapper_license.txt


Files Included
---------------------------
wrapper.exe
wrapper_readme.txt
/bin/wrapper/wrapper.conf
/bin/wrapper/wrapper_license.txt
/bin/wrapper/lib/wrapper.dll
/bin/wrapper/lib/wrapper.jar
/bin/wrapper/lib/wrappertest.jar



Testing GeoServer with Java Service Wrapper
-------------------------------------------

To run GeoServer on the console, use:

wrapper.exe -c ./bin/wrapper/wrapper.conf

GeoServer should start and be accessible at http://localhost:8080/geoserver


Installing GeoServer as a Windows service
-----------------------------------------

Run the following command once:

wrapper.exe -i ./bin/wrapper/wrapper.conf

A "GeoServer" service will be installed; it will be automatically started
when the system is booted.


Removing the GeoServer service
------------------------------

Note that you must run this command before deleting GeoServer and this
directory, in order to remove the GeoServer service:

wrapper.exe -r ./bin/wrapper/wrapper.conf


Packaged by Etienne Dube
etdube@gmail.com

Modified by Brent Owens (TOPP)
bowens@openplans.org
