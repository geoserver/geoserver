.. _security_tutorials_digest:

Configuring Digest Authentication
=================================

Introduction
------------

Out of the box GeoServer REST and OGC services support authentication via 
`HTTP Basic authentication <http://en.wikipedia.org/wiki/Basic_access_authentication>`_. 
One of the major downsides of basic auth is that it sends user passwords in 
plain text. `HTTP Digest authentication <Digest access authentication>`_ offers
a more secure alternative that applies a cryptographic hash function to 
passwords before sending them over the network.

This tutorial walks through the process of setting up digest authentication.

Prerequisites
-------------

This tutorial uses the `curl <http://curl.haxx.se/>`_ utility to issue HTTP 
request that test authentication. Install curl before proceeding.

.. note::

   Any utility that supports both basic and digest authentication can be used in 
   place of curl. Most modern web browsers support both types of authentication.

Configure the Digest authentication filter
------------------------------------------

#. Start GeoServer and login to the web admin interface as the ``admin`` user.
#. Click the ``Authentication`` link located under the ``Security`` section of
   the navigation sidebar.

    .. figure:: images/digest1.jpg
       :align: center

#. Scroll down to the ``Authentication Filters`` panel and click the ``Add new`` link.

    .. figure:: images/digest2.jpg
       :align: center

#. Click the ``Digest`` link.

    .. figure:: images/digest3.jpg
       :align: center

#. Fill in the fields of the settings form as follows:

   * Set ``Name`` to "digest"
   * Set ``User group service`` to "default"
   
   .. figure:: images/digest4.jpg
      :align: center

#. Save.
#. Back on the authentication page scroll down to the ``Filter Chains`` panel. 
#. Select "Default" from the ``Request type`` drop down.
#. Unselect the ``basic`` filter and select the ``digest`` filter. Position the
   the ``digest`` filter before the ``anonymous`` filter. 

   .. figure:: images/digest5.jpg
      :align: center
#. Save.

Secure OGC service requests
---------------------------

In order to test the authentication settings configured in the previous section
a service or resource must be first secured. The ``Default`` filter chain is the
chain applied to all OGC service requests so a service security rule must be 
configured.

#. From the GeoServer home page and click the ``Services`` link located under the 
   ``Security`` section of the navigation sidebar.
   
   .. figure:: images/digest6.jpg
      :align: center
   
#. On the Service security page click the ``Add new rule`` link and add a catch all
   rule that secures all OGC service requests requiring the ``ROLE_ADMINISTRATOR``
   role.
   
   .. figure:: images/digest7.jpg
      :align: center
      
#. Save.

Test a digest authentication login
----------------------------------

#. Ensure that basic authentication is disabled execute the following curl command::

     curl -v -u admin:geoserver -G "http://localhost:8080/geoserve/wfs?request=getcapabilities"
   
   The result should be a 401 response signaling that authentication is required. The output 
   should look something like the following::

    * About to connect() to localhost port 8080 (#0)
    *   Trying 127.0.0.1... connected
    * Connected to localhost (127.0.0.1) port 8080 (#0)
    * Server auth using Basic with user 'admin'
    > GET /geoserver/wfs?request=getcapabilities HTTP/1.1
    > Authorization: Basic YWRtaW46Z2Vvc2VydmVy
    > User-Agent: curl/7.19.7 (universal-apple-darwin10.0) libcurl/7.19.7 OpenSSL/0.9.8r zlib/1.2.3
    > Host: localhost:8080
    > Accept: */*
    > 
    < HTTP/1.1 401 Full authentication is required to access this resource
    < Set-Cookie: JSESSIONID=1dn2bi8qqu5qc;Path=/geoserver
    < WWW-Authenticate: Digest realm="GeoServer Realm", qop="auth", nonce="MTMzMzQzMDkxMTU3MjphZGIwMWE4MTc1NmRiMzI3YmFiODhmY2NmZGQ2MzEwZg=="
    < Content-Type: text/html; charset=iso-8859-1
    < Content-Length: 1491
    < Server: Jetty(6.1.8)
    < 
    <html>
    <head>
    <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"/>
    <title>Error 401 Full authentication is required to access this resource</title>
    </head>
    ...

#. Execute the same command but specify the ``--digest`` option to tell curl to use
   digest authentication rather than basic authentication::
   
     curl --digest -v -u admin:geoserver -G "http://localhost:8080/geoserve/wfs?request=getcapabilities"
    
   The result should be a successful authentication and contain the normal WFS capabilities response.

