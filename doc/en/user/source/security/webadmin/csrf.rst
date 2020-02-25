.. _security_webadmin_csrf:

CSRF Protection
===============

The GeoServer web admin employs a CSRF (Cross-Site Request Forgery) protection filter that will block any form submissions that didn't appear to originate from GeoServer. This can sometimes cause problems for certain proxy configurations.

.. note:: Symptoms of this problem may include the WPS request builder (and
  other wicket pages) failing with an HTTP status of 403 and the message "Origin
  does not correspond to request". However, you may need to view the page
  response in a debugger to see this, to the end user it will appear as if the
  form section of the page is just missing.

To white list your proxy with the CSRF filter, you can use the ``GEOSERVER_CSRF_WHITELIST`` property. This property is a comma-separated list of domains, of the form ``<domainname>.<TLD>``, and can contain a subdomains.
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
