.. _google_earth_overview:

Overview
========

Why use GeoServer with Google Earth?
------------------------------------

GeoServer is useful when one wants to put a lot of data on to Google Earth. GeoServer automatically generates KML that can be easily and quickly served and visualized in Google Earth.  GeoServer operates entirely through a `Network Link <http://code.google.com/apis/kml/documentation/kml_tut.html#network_links>`_, which allows it to selectively return information for the area being viewed.  With GeoServer as a robust and powerful server and Google Earth providing rich visualizations, they are a perfect match for sharing your data.


Standards-based implementation
------------------------------

GeoServer supports Google Earth by providing KML as a `Web Map Service <http://en.wikipedia.org/wiki/Web_Map_Service>`_ (WMS) output format.  This means that adding data published by GeoServer is as simple as constructing a standard WMS request and specifying 
"**application/vnd.google-earth.kml+xml**" as the ``outputFormat``. Since generating KML is just a WMS request, it fully supports :ref:`styling` via SLD. 


See the next section (:ref:`google_earth_quickstart`) to view GeoServer and Google 
Earth in action. 

