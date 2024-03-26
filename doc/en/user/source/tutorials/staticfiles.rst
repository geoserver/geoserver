.. _tutorials_staticfiles:

Serving Static Files
====================

You can place static files in the ``www`` subdirectory of the GeoServer :ref:`data directory <datadir_structure>`, and they will be served at ``http:/myhost:8080/geoserver/www``.  This means you can deploy HTML, images, or JavaScript, and have GeoServer serve them directly on the web. 

This approach has some limitations:

* This approach does not make use of accelerators such as the `Tomcat APR library <http://tomcat.apache.org/tomcat-7.0-doc/apr.html>`_. If you have many static files to be served at high speed, you may wish to create your own web app to be deployed along with GeoServer or use a separate web server to serve the content.

The ``GEOSERVER_DISABLE_STATIC_WEB_FILES`` property can be set to true convert the ``text/html`` and ``application/javascript``
content types to ``text/plain`` in the ``Content-Type`` HTTP response header which will prevent web pages from being served
through the ``www`` directory. This will help to prevent stored cross-site scripting vulnerabilities if the ``www`` directory
is not being used at all or if it is only used to serve files other than web pages, such as PDF or Word documents. The default
behavior is to **NOT** convert these content types. This property can be set either via Java system property, command line
argument (-D), environment variable or web.xml init parameter.

Content Security Policy
-----------------------

The ``Content-Security-Policy`` header will allow ``'unsafe-inline'`` and ``'unsafe-eval'`` scripts
by default. If unsafe scripts are not necessary and it is not necessary to load font, image, style
or script resources from remote hosts, the ``GEOSERVER_STATIC_WEB_FILES_SCRIPT`` property can be
set either via Java system property, command line argument (-D), environment variable or web.xml
init parameter. The property can be set to either ``SELF`` or ``UNSAFE`` with ``UNSAFE`` being the
default value.

See :ref:`security_csp` for instructions to modify the CSP header if it is continuing to block
certain functionality of custom HTML pages even with the ``UNSAFE`` property.

.. note::
    It is recommended that static web files be disabled if they are not necessary in order to
    mitigate cross-site scripting attacks. Unsafe scripts should be disabled if it is necessary to
    server static HTML pages but they do not require unsafe scripts.
