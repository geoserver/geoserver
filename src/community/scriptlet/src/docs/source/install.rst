Installing the Scriptlet Extension 
==================================

The scriptlet extension uses the normal GeoServer extension installation
process.  The steps are simple:

1. Install GeoServer
2. Locate the GeoServer library directory, which contains several JAR files that
   provide GeoServer's functionality.

   * This is :file:`geoserver-{version}/webapps/geoserver/WEB-INF/lib/` if you
     installed from the GeoServer binary installer.
   * This is :file:`{servlet-container}/webapps/geoserver/WEB-INF/lib/` for the
     default configuration on many servlet containers, including Tomcat and
     Jetty.
3. Extract the scriptlet JAR file and the accompanying :file:`js.jar` into the
   library directory.
4. Restart GeoServer.

.. note:: If you have a development environment available, you can also simply 
    activate the 'scriptlet' profile when building and running GeoServer::
        
        $ mvn install -Pscriptlet
        $ cd web/app/
        $ mvn jetty:run -Pscriptlet

Testing the Installation
------------------------
Scripts based on the scriptlet extension belong in :file:`{DATA_DIR}/scripts/`.
You can test that the extension is working properly by creating a simple "hello,
world" script in :file:`{DATA_DIR}/scripts/hello.js` and verifying that a web
request to http://localhost:8080/geoserver/rest/script produces a listing that
includes that file.

For your sample script, you can copy the following::
    
    var StringRepresentation = Packages.org.restlet.resource.StringRepresentation;
    var MediaType = Packages.org.restlet.data.MediaType;

    response.setEntity(new StringRepresentation(
        "Hello world!", MediaType.TEXT_PLAIN
    ));

If it shows "Hello world!" in your browser when you visit
http://localhost:8080/geoserver/rest/script/hello, congratulations! You have
successfully installed the scriptlet extension!
