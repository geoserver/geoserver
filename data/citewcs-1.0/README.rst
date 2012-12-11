CITE Data
=========

The Open Geospatial Consortium Compliance and Intolerability Testing Initiative (CITE)
only tests the WCS protocol and does not provide any test data.

For more information:

* http://cite.opengeospatial.org/teamengine/
* http://cite.opengeospatial.org/teamengine/wcs-1.1.1/index.html

Instructions
------------

There are complete running instructions here:

http://geoserver.org/display/GEOS/Running+Cite+Tests

Here's the summary:

First and IMPORTANT, to run WCS 1.0.0 tests you need to take the geoserver WCS 1.1 module out of the classpath.
To do so, if you're running geosever from eclipse just remove the wcs-11 dependency from the web module. If running
geoserver with maven jetty:run, remove the wcs1_1 dependency in the pom.xml file. Then run GeoServer.

The engine will ask you for the capabilities url and other parameters. These are the ones to use:

1. capabilities URL: the getcapabilities url
2. MIME Header Setup: image/tiff
3. UpdateSequence Values: higher: 2, lower: 0
4. The grid resolutions: 0.1 and 0.1
5. Verify that the server supports XML encoding: yes
6. Verify that the server supports range set axis: yes
7. The server implements the original or corrigendum schemas: original

