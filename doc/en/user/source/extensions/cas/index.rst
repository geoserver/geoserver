.. _cas:

CAS integration
===============

The CAS module allows to integrate GeoServer with the `Central Authentication Service (CAS) <https://www.apereo.org/projects/cas>`_ 
Single Sign On (SSO), in particular, using standard tickets and proxy tickets.

Installation
------------

To install the CAS module:

 #. Navigate to the `GeoServer download page <http://geoserver.org/download>`_.

 #. Find the page that matches the version of the running GeoServer.

 #. Download the CAS extension. The download link will be in the :guilabel:`Extensions` section under :guilabel:`Security`.

 #. Extract the files in this archive to the :file:`WEB-INF/lib` directory of your GeoServer installation.

 #. Restart GeoServer

Configuration
-------------

The CAS integration is a authentication filter module, hence in order to use it one has to:

* Go to the authentication page, add the CAS filter and configure it
* Add it to the authentication chains, taking care of removing the other authentication methods
  (or the CAS authentication won't trigger and redirect users to the CAS login page)

This page serves as a reference for configuration options, but a step by step tutorial is also
available, see :ref:`security_tutorials_cas`.

    .. figure:: images/configuration.png
       :align: center

.. list-table::
   :widths: 20 80
   :header-rows: 1

   * - Key
     - Description
   * - Name
     - Name of the filter
   * - CAS server URL including context root
     - The CAS server location (GeoServer will redirect to it, for example, in order to login, adding the necessary extra path elements)
   * - No single sign on
     - If checkecd, will send the "renew=true" options to the CAS server, forcing the user to login on the server at each request (unless session creation is allowed)
   * - Participate in single sign out
     - Whether GeoServer should receive and handle callbacks for Single Sign Out.
   * - URL in CAS loutput page
     - CAS logout page location
   * - Proxy callback URL
     - The URL CAS will call back to after proxy ticket authentication
   * - Role source
     - A choice of role sources for the user authenticated via CAS

Specifically for the **role source**, the followig options are available:

.. list-table::
   :widths: 20 80
   :header-rows: 1

   * - Source
     - Description
   * - User group service
     - Locate the user in a the specified user group service, extract the roles from it.
   * - Role service
     - Locate user roles from the specified role service
   * - Header attribute
     - Look up the roles in the specified HTTP header of the CAS response
   * - Custom CAS attribute
     - Extract the roles from a CAS custom attribute in the ``<cas:serviceResponse>`` received from the server. 
       The attributes must be configured, on the CAS side, via an attribute repository, and then released for publication in service configuration.

Example CAS configuration
-------------------------

In order to use the CAS custom attributes the server must be configured to extract the attributes
from a given attribute repository, and then allow their release in the GeoServer service configuration.

For example, the following ``cas.properties`` file sets up a JDBC user source, as well as as JDBC
attribute repository (this configuration file might useful for testing purposes, but not setup for production):

.. code-block::

    cas.server.name=https://localhost:8443
    cas.server.prefix=${cas.server.name}/cas
    server.ssl.key-store=file:/etc/cas/config/thekeystore
    server.ssl.key-store-password=changeit
    logging.config=file:/etc/cas/config/log4j2.xml
    # cas.authn.accept.users=
    
    cas.authn.jdbc.query[0].driver-class=org.postgresql.Driver
    cas.authn.jdbc.query[0].url=jdbc:postgresql://localhost:5432/cas_users
    cas.authn.jdbc.query[0].dialect=org.hibernate.dialect.PostgreSQL95Dialect
    cas.authn.jdbc.query[0].driver-class=org.postgresql.Driver
    cas.authn.jdbc.query[0].user=theDbUser
    cas.authn.jdbc.query[0].password=theDbPassword
    cas.authn.jdbc.query[0].sql=SELECT * FROM users WHERE email = ?
    cas.authn.jdbc.query[0].password-encoder.type=BCRYPT
    cas.authn.jdbc.query[0].field-password=password
    cas.authn.jdbc.query[0].field-expired=expired
    cas.authn.jdbc.query[0].field-disabled=disabled
    
    
    cas.authn.attributeRepository.jdbc[0].driver-class=org.postgresql.Driver
    cas.authn.attributeRepository.jdbc[0].url=jdbc:postgresql://localhost:5432/cas_users
    cas.authn.attributeRepository.jdbc[0].dialect=org.hibernate.dialect.PostgreSQL95Dialect
    cas.authn.attributeRepository.jdbc[0].driver-class=org.postgresql.Driver
    cas.authn.attributeRepository.jdbc[0].user=theDbUser
    cas.authn.attributeRepository.jdbc[0].password=theDbPassword
    cas.authn.attributeRepository.jdbc[0].attributes.role=role
    cas.authn.attributeRepository.jdbc[0].singleRow=false
    cas.authn.attributeRepository.jdbc[0].columnMappings.attribute=value
    cas.authn.attributeRepository.jdbc[0].sql=SELECT * FROM roles WHERE {0}
    cas.authn.attributeRepository.jdbc[0].username=email
    
    cas.service-registry.json.location=classpath:/services

The database has the following two tables for users and roles:

.. code-block:: sql

    CREATE TABLE public.users (
        id bigint NOT NULL,
        disabled boolean,
        email character varying(40),
        first_name character varying(40),
        last_name character varying(40),
        expired boolean,
        password character varying(100)
    );
    
    CREATE TABLE public.roles (
        email character varying,
        attribute character varying,
        value character varying
    );

A sample service configuration for GeoServer might look as follows (again, setup for testing
and development only):

.. code-block:: json

    {
      "@class" : "org.apereo.cas.services.RegexRegisteredService",
      "serviceId" : "^http(s)?://localhost:[\\d]+/geoserver/.*",
      "name" : "GeoServer",
      "id" : 1002,
      "logoutType" : "BACK_CHANNEL",
      "logoutUrl" : "https://localhost:8442/geoserver",
      "redirectUrl" : "https://localhost:8442/geoserver",
      "proxyPolicy" : {
        "@class" : "org.apereo.cas.services.RegexMatchingRegisteredServiceProxyPolicy",
        "pattern" : "^http(s)?://localhost:[\\d]+/geoserver/.*"
      },
      "attributeReleasePolicy" : {
        "@class" : "org.apereo.cas.services.ReturnAllAttributeReleasePolicy"
      }
    }

