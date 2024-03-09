# Serving Static Files

You can place static files in the `www` subdirectory of the GeoServer [data directory](../datadirectory/structure.md), and they will be served at `http:/myhost:8080/geoserver/www`. This means you can deploy HTML, images, or JavaScript, and have GeoServer serve them directly on the web.

This approach has some limitations:

-   This approach does not make use of accelerators such as the [Tomcat APR library](http://tomcat.apache.org/tomcat-7.0-doc/apr.html). If you have many static files to be served at high speed, you may wish to create your own web app to be deployed along with GeoServer or use a separate web server to serve the content.

The `GEOSERVER_DISABLE_STATIC_WEB_FILES` property can be set to true convert the `text/html` and `application/javascript` content types to `text/plain` in the `Content-Type` HTTP response header which will prevent web pages from being served through the `www` directory. This will help to prevent stored cross-site scripting vulnerabilities if the `www` directory is not being used at all or if it is only used to serve files other than web pages, such as PDF or Word documents. The default behavior is to **NOT** convert these content types. This property can be set either via Java system property, command line argument (-D), environment variable or web.xml init parameter.
