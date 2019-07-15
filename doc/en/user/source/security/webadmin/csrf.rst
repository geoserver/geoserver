.. _security_webadmin_csrf:

CSRF Protection
===============

The GeoServer web admin employs a CSRF (Cross-Site Request Forgery) protection filter that will block any form submissions that didn't appear to originate from GeoServer. This can sometimes cause problems for certain proxy configurations.

To whitelist your proxy with the CSRF filter, you can use the ``GEOSERVER_CSRF_WHITELIST`` property. This property is a comma-seperated list of domains, of the form ``<domainname>.<TLD>``, and can contain a subdomains.
Alternatively, you can disable the CSRF filter by setting the ``GEOSERVER_CSRF_DISABLED`` property to ``true``.

Each of these properties is set through one of the standard means:

* ``web.xml`` ::

    <context-param>
      <param-name>GEOSERVER_CSRF_WHITELIST</param-name>
      <param-value>example.org</param-value>
    </context-param>
  
* System property ::

    -DGEOSERVER_CSRF_WHITELIST=example.org

* Environment variable ::

    export GEOSERVER_CSRF_WHITELIST=example.org