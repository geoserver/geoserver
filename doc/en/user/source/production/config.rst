.. _production_config:

Configuration Considerations
============================

General Guidance
----------------

Use production logging
''''''''''''''''''''''

Excessive logging may visibly affect the performance of your server. High logging levels are often necessary to track down issues, but during operation you should run with low levels.  (You can switch the logging levels while GeoServer is running.)

You can change the logging level in the :ref:`config_globalsettings_log_profile`.  You will want to choose the ``PRODUCTION`` logging configuration, where only problems are written to the log files.

Personalize your server
'''''''''''''''''''''''

In order to your GeoServer as welcoming as possible, you should customize the server's metadata to your organization.  It may be tempting to skip some of the configuration steps, and leave in the same keywords and abstract as the defaults, but this will only confuse potential users.

Suggestions:

* Fill out :ref:`service_metadata` sections for WFS, WMS, WCS, WMTS web services.

* Use :ref:`config_contact` to provide service welcome message, contact details and a link to your organisation.

  This message is be shown to visitors at the top of welcome page. The contact details and organisation information are included in the welcome page, and used to describe each web service in the capabilities documents.
  
* When setting up a workspace you can provide more detailed service metadata and contact information.
* Serve your data with your own namespace (and provide a correct URI)
* Remove default layers (such as ``topp:states``)

Configure service limits
''''''''''''''''''''''''

Make sure clients cannot request an inordinate amount of resources from your server.

In particular:

* Set the :ref:`maximum amount of features <services_webadmin_wfs>` returned by each WFS GetFeature request (this can also be set on a per featuretype basis by modifying the :ref:`layer publishing wfs settings <data_webadmin_layers>`).
* Set the :ref:`WMS request limits <wms_configuration>` so that no request will consume too much memory or too much time.
* Set :ref:`WPS limits <webadmin_wps>`, so no process will consume too much memory or too much time. You may also limit the :ref:`size input parameters <wps_security>` for further control.
* Install and configure the :ref:`control_flow` for greater control of client access.

Welcome page selectors
''''''''''''''''''''''

The workspace and layer selectors might take a lot of time to fill up against large catalogs. Because of this, GeoServer tries to limit the time taken to fill them (by default, 5 seconds), and the number of items in them (by default, ``1000``), and will fall back on simple text fields if the time limit is reached.

In some situations, that won't be enough and the page might get stuck anyways. The following properties can be used to tweak the behavior:

*  ``GeoServerHomePage.selectionMode`` : can be set to ``text`` to always use simple text fields, ``dropdown`` to always use dropdowns, or ``auto`` to use the default automatic behavior.
* ``GeoServerHomePage.selectionTimeout`` : the time limit in milliseconds, defaults to ``5000``.
* ``GeoServerHomePage.selectionMaxItems`` : the maximum number of items to show in the dropdowns, defaults to ``1000``.

When using ``text`` selection mode the page description is static, no longer offering of available workspace and layers.

.. figure:: images/selector_text.png
   
   Welcome page text selection mode

Cache your data
'''''''''''''''

Server-side caching of WMS tiles is the best way to increase performance.  In caching, pre-rendered tiles will be saved, eliminating the need for redundant WMS calls.  There are several ways to set up WMS caching for GeoServer.  GeoWebCache is the simplest method, as it comes bundled with GeoServer.  (See the section on :ref:`gwc` for more details.)  Another option is `TileCache <https://tilecache.org>`__.

You can also use a more generic non-spatial caching system, such as `Ehcache <https://www.ehcache.org>`__ (an embedded cache service) or `Squid <http://www.squid-cache.org>`__ (a web cache proxy).

Caching is also possible for WFS layers, in a very limited fashion. For DataStores that don't have a quick way to determine feature counts (e.g. shapefiles), enabling caching can prevent querying a store twice during a single request. To enable caching, set the Java system property ``org.geoserver.wfs.getfeature.cachelimit`` to a positive integer. Any data sets that are smaller than the cache limit will be cached for the duration of a request, which will prevent the dataset from being queried a second time for the feature count. Note that this may adversely affect some types of DataStores, as it bypasses any feature count optimizations that may exist.

Set security for data modification
''''''''''''''''''''''''''''''''''

GeoServer includes support for WFS-T (transactions) which lets users modify your data.

If you don't want your database modified, you can turn off transactions in the :ref:`services_webadmin_wfs`. Set the :guilabel:`Service Level` to ``Basic``. For extra security, we recommend any database access use datastore credentials providing read-only permissions. This will eliminate the possibility of a SQL injection (though GeoServer is generally not vulnerable to that sort of attack).

If you would like some users to be able to modify data, set the service level :guilabel:`Service Level` to ``Transactional`` (or ``Complete``) and use :ref:`security_service` to limit access to the `WFS.Transaction` operation.

If you would like some users to be able to modify some but not all of your data, set the :guilabel:`Service Level` to ``Transactional`` (or ``Complete``), and use :ref:`security_layer` to limit write access to specific layers. Data security can be used to allow write access based on workspace, datastore, or layer security.

GeoServer Workspace Admin Guidance
----------------------------------

Establishing a workspace administrator user is a recommended configuration providing limited access to the Admin Console to manage the publication of information, but are not intended to be trusted as a GeoServer Administrator with responsibility for the global settings and system integration controls.

1. Create a role to be used for workspace administration.

2. Provide this role to the Users (or Groups) requiring workspace admin access.

3. Provide this role :ref:`data security <security_webadmin_data>` admin access ``a`` to:

   * :ref:`workspace <workspace_security>` administration
   * :ref:`layer <layer_security>` administration

4. Recommendation: The combination of workspace admin permission and GROUP_ADMIN access provides a effective combination for an individual responsible for a workspace. This provides the ability to both manage and control access to the data products in a workspace.

GeoServer Administrator Guidance
--------------------------------

The GeoServer administration console provides a trusted GeoServer Administrator control of the application. This is often the same individual as the System Administrator who has deep knowledge of the operational environment.

In this workflow the Administration Console is used to adapt the application to the operational environment:

* :ref:`proxy_base`
* :ref:`config_globalsettings_log_location`
* ... and many more :ref:`config_globalsettings`.

Management of a web service using an administration console is a more common practice when running GeoServer as a windows web service.

System Administrator Guidance
-----------------------------

In situations where GeoServer is operating in an environment provided by a System Administrator the use of :ref:`application_properties` is available.

* ``PROXY_BASE_URL``
* ``GEOSERVER_LOG_LOCATION``
* ``GEOSERVER_CONSOLE_DISABLED``
* ... and many more :ref:`application_properties`

This approach removes some functionality from the Administration Console and REST API.

Management of web services using environmental variables is standard practice when running GeoServer in a Linux or Docker environment.

Logging configuration hardening
''''''''''''''''''''''''''''''''

For production systems, it is advised to set ``GEOSERVER_LOG_LOCATION`` parameter during startup. The value may be defined as either an environment variable, java system property, or servlet context parameter.

The location set for ``GEOSERVER_LOG_LOCATION`` has priority, causing the setting provided using the Admin Console or REST API to be ignored.

See :ref:`logging_location` for more information.

Disable the GeoServer web administration interface
''''''''''''''''''''''''''''''''''''''''''''''''''

In some circumstances, you might want to completely disable the web administration interface.  There are two ways of doing this:

* Set the Java system property ``GEOSERVER_CONSOLE_DISABLED`` to true by adding ``-DGEOSERVER_CONSOLE_DISABLED=true`` to your container's JVM options
* Remove all of the :file:`gs-web*-.jar` files from :file:`WEB-INF/lib`

.. _module_status_security_environment_vars:

Showing Environment Variables and Java System Properties
''''''''''''''''''''''''''''''''''''''''''''''''''''''''

Module status information is available describing the operational environment.

* The :guilabel:`GeoServer Status` page :ref:`config_serverstatus_module`.
* The REST ``/geoserver/rest/about/status`` endpoint lists module status information

1. By default GeoServer does **not** show Environment Variables and Java System Properties.

2. To show environment variables and Java system properties on the status page and REST API, start GeoServer with these environment variables set to ``true``:

   * `GEOSERVER_MODULE_SYSTEM_ENVIRONMENT_STATUS_ENABLED`
   * `GEOSERVER_MODULE_SYSTEM_PROPERTY_STATUS_ENABLED`

3. In a production system, these should be set to ``false`` (or leave them undefined).

   .. warning::

      While this feature can help an administrator debug a GeoServer instance's configuration, environment variables can include sensitive information such as database passwords and API access keys/tokens, particularly when running in a containerised environment (such as Docker or Kubernetes) or with ``ALLOW_ENV_PARAMETRIZATION=true``.

   .. note:: Linux
   
      Linux administrators can get a list of all environment variables set at GeoServer startup with:

      .. code-block:: bash

         tr '\0' '\n' < /proc/${GEOSERVER_PID}/environ

Application Server Guidance
---------------------------

A few settings are only available by adjusting the Application Server environment :ref:`web context parameters <application_properties>`.

Set a service strategy
''''''''''''''''''''''

A service strategy is the method in which output is served to the client.  This is a balance between proper form (being absolutely sure of reporting errors with the proper OGC codes, etc.) and speed (serving output as quickly as possible).  This is a decision to be made based on the function that GeoServer is providing.

In Apache Tomcat you can provide system property by creating :file:`conf/Catalina/localhost/geoserver.xml`:

.. code-block:: xml
   
   <Context>
     <Parameter name="serviceStrategy"
                value="PARTIAL-BUFFER2" override="false"/>
   </Context>
   

You can configure the default service strategy by modifying the :file:`web.xml` file of your GeoServer instance:

.. code-block:: xml
   
    <context-param>
        <param-name>serviceStrategy</param-name>
        <param-value>PARTIAL-BUFFER2</param-value>
    </context-param>

The possible strategies are:

.. list-table::
   :widths: 20 80

   * - **Strategy**
     - **Description**
   * - ``SPEED``
     - Serves output right away. This is the fastest strategy, but proper OGC errors are usually omitted.
   * - ``BUFFER``
     - Stores the whole result in memory, and then serves it after the output is complete.  This ensures proper OGC error reporting, but delays the response quite a bit and can exhaust memory if the response is large.
   * - ``FILE``
     - Similar to ``BUFFER``, but stores the whole result in a file instead of in memory. Slower than ``BUFFER``, but ensures there won't be memory issues.
   * - ``PARTIAL-BUFFER2`` 
     - A balance between ``BUFFER`` and ``SPEED``, this strategy tries to buffer in memory a few KB of response, then serves the full output.

Security and Service Hardening
------------------------------

The following settings allow administrators to take greater control of the application allowing functionality to be disabled.

Disable the Auto-complete on web administration interface login
'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''

To disable the Auto Complete on Web Admin login form:

* Set the Java system property ``geoserver.login.autocomplete`` to off by adding ``-Dgeoserver.login.autocomplete=off`` to your container's JVM options
* If the browser has already cached the credentials, please consider clearing the cache or form data after setting the JVM option.

Disable anonymous access to the layer preview page
''''''''''''''''''''''''''''''''''''''''''''''''''

In some circumstances, you might want to provide access to the layer preview page to authenticated users only. The solution is based on
adding a new :guilabel:`filter chain` with a rule matching the path of the layer preview page to GeoServer's :ref:`security_auth_chain`. Here are the
steps to reproduce:

* Under :guilabel:`Security` -> :guilabel:`Authentication` -> :guilabel:`Filter Chains`, add a new HTML chain
* Set the new chain's name to ``webLayerPreview`` (or likewise)
* As Ant pattern, enter the path of the layer preview page, which is :file:`/web/wicket/bookmarkable/org.geoserver.web.demo.MapPreviewPage`
  (since it's an Ant pattern, the path could as well be written shorter with wildcards: :file:`/web/**/org.geoserver.web.demo.MapPreviewPage`)
* Check option :guilabel:`Allow creation of an HTTP session for storing the authentication token`
* Under :guilabel:`Chain filters`, add filters ``rememberme`` and ``form`` (in that order) to the :guilabel:`Selected` list on the right side
* Close the dialog by clicking the :guilabel:`Close` button; the new HTML chain has been added to the list of chains as the last entry
* Since all chains are processed in turn from top to bottom, in order to have any effect, the new ``webLayerPreview`` chain must be positioned
  **before** the ``web`` chain (which matches paths :file:`/web/**,/gwc/rest/web/**,/`)
* Use the :guilabel:`Position` arrows on the left side of the list to move the newly added chain upwards accordingly
* Save the changes you've made by clicking the :guilabel:`Save` button at the bottom of the page

With that in place, unauthenticated users now just get forwarded to the login page when they click the layer preview menu item link.

The above procedure could as well be applied to other pages of the web administration interface that one needs to remove anonymous access for. For example:

* :guilabel:`Demos` -> :guilabel:`Demo requests`
  (path: :file:`/web/wicket/bookmarkable/org.geoserver.web.demo.DemoRequestsPage`)
* :guilabel:`Demos` -> :guilabel:`WCS request builder`
  (path: :file:`/web/wicket/bookmarkable/org.geoserver.wcs.web.demo.WCSRequestBuilder`)

.. warning::
    Although disabling anonymous access to the layer preview page **MAY** prevent some unauthenticated users from accessing data with some simple
    clicks, this is **NOT** a security feature. In particular, since other more sophisticated users, having the ability to build OGC requests, **MAY**
    still access critical data through GeoServer's services, this is **NOT** a replacement for a well-designed security concept based on data-level or
    service-level security.

X-Frame-Options Policy
''''''''''''''''''''''

In order to prevent clickjacking attacks GeoServer defaults to setting the X-Frame-Options HTTP 
header to ``SAMEORIGIN``. This prevents GeoServer from being embedded into an iFrame, which prevents certain
kinds of security vulnerabilities. See the `OWASP Clickjacking entry <https://www.owasp.org/index.php/Clickjacking_Defense_Cheat_Sheet>`_ for details.

If you wish to change this behavior you can do so through the following properties:

* ``geoserver.xframe.shouldSetPolicy``: controls whether the X-Frame-Options header should be set at all. Default is true.
* ``geoserver.xframe.policy``: controls what to set the X-Frame-Options header to. Default is ``SAMEORIGIN``. Valid options are ``DENY``, ``SAMEORIGIN`` and ``ALLOW-FROM [uri]``.

These properties can be set either via Java system property, command line argument (-D), environment
variable or :file:`web.xml` init parameter.

.. note::
    The WMS GetMap OpenLayers output format uses iframes to display the WMS GetFeatureInfo output and
    this may not function properly if the policy is set to something other than ``SAMEORIGIN``.

.. warning::
    The ``ALLOW-FROM`` option is not supported by modern browsers and should only be used if you know
    that browsers interacting with your GeoServer will support it. Applying this policy will be treated
    as if no policy was set by browsers that do not support this (i.e., **NO** protection). The
    ``Content-Security-Policy`` header provides more robust support for allowing specific hosts to
    display frames from GeoServer using the ``frame-ancestors`` directive.

If the ``geoserver.csp.frameAncestors`` system property has not been set, the ``frame-ancestors``
directive of the ``Content-Security-Policy`` header will default to being set based on the value of
the ``X-Frame-Options`` header.

* ``SAMEORIGIN`` will be ``frame-ancestors 'self'``
* ``DENY`` will be ``frame-ancestors 'none'``
* if the ``X-Frame-Options`` header is not set or has any other value, the ``frame-ancestors``
  directive will be omitted

When both ``frame-ancestors`` and ``X-Frame-Options`` are present, browsers that support
``frame-ancestors`` should **enforce** the ``frame-ancestors`` policy and **ignore** the
``X-Frame-Options`` policy.

X-Content-Type-Options Policy
'''''''''''''''''''''''''''''

In order to mitigate MIME confusion attacks (which often results in Cross-Site Scripting), GeoServer defaults to setting the ``X-Content-Type-Options: nosniff`` HTTP header.
See the `OWASP X-Content-Type-Options entry <https://cheatsheetseries.owasp.org/cheatsheets/HTTP_Headers_Cheat_Sheet.html#x-content-type-options>`_ for details.

If you wish to change this behavior you can do so through the following property:

* ``geoserver.xContentType.shouldSetPolicy``: controls whether the X-Content-Type-Options header should be set. Default is true.

This property can be set either via Java system property, command line argument (-D), environment
variable or web.xml init parameter.

X-XSS-Protection Policy
'''''''''''''''''''''''

GeoServer supports setting the X-XSS-Protection HTTP header in order to control the built-in reflected XSS filtering that existed in
some older browsers. This header is **NOT** enabled by default since it does not affect modern browsers. Enabling the header without
specifying a policy will default to Spring Security's default of ``0`` (which is also the current OWASP recommendation). See the
`OWASP X-XSS-Protection entry <https://cheatsheetseries.owasp.org/cheatsheets/HTTP_Headers_Cheat_Sheet.html#x-xss-protection>`_ for details.

If you wish to change this behavior you can do so through the following properties:

* ``geoserver.xXssProtection.shouldSetPolicy``: controls whether the X-XSS-Protection header should be set at all. Default is false.
* ``geoserver.xXssProtection.policy``: controls what to set the X-XSS-Protection header to. Default is ``0``. Valid options are ``0``, ``1`` and ``1; mode=block``.

These properties can be set either via Java system property, command line argument (-D), environment
variable or web.xml init parameter.

Strict-Transport-Security Policy
''''''''''''''''''''''''''''''''

In order to reduce the possibility of man-in-the-middle attacks GeoServer supports setting the Strict-Transport-Security HTTP header.
This header is **NOT** enabled by default and, when enabled, this header will only be set on HTTPS requests. If a policy has not been
set, the default policy will be the same as Spring Security's default of ``max-age=31536000 ; includeSubDomains``. See the
`OWASP Strict-Transport-Security entry <https://cheatsheetseries.owasp.org/cheatsheets/HTTP_Headers_Cheat_Sheet.html#strict-transport-security-hsts>`_ for details.

If you wish to change this behavior you can do so through the following properties:

* ``geoserver.hsts.shouldSetPolicy``: controls whether the Strict-Transport-Security header should be set at all. Default is false.
* ``geoserver.hsts.policy``: controls what to set the Strict-Transport-Security header to. Default is ``max-age=31536000 ; includeSubDomains``. Valid options can change the max-age to the desired age in seconds and can omit the includeSubDomains directive.

These properties can be set either via Java system property, command line argument (-D), environment
variable or web.xml init parameter.

.. _production_config_csp:

Content-Security-Policy
'''''''''''''''''''''''

In order to mitigate cross-site scripting and clickjacking attacks GeoServer defaults to setting
the Content-Security-Policy HTTP header based on rules configured by the administrator. See the
:ref:`security_csp` page for more details about this header, GeoServer's default configuration and
how to change the configuration.

OWS ServiceException XML mimeType
'''''''''''''''''''''''''''''''''

By default, OWS Service Exception XML responses have content-type set to ``application/xml``.

In case you want it set to ``text/xml`` instead, you need to setup the Java System properties:

* ``-Dows10.exception.xml.responsetype=text/xml`` for OWS 1.0.0 version
* ``-Dows11.exception.xml.responsetype=text/xml`` for OWS 1.1.0 version

.. _production_config_freemarker_escaping:

FreeMarker Template Auto-escaping
'''''''''''''''''''''''''''''''''

By default, FreeMarker's built-in automatic escaping functionality will be enabled to mitigate potential cross-site scripting
(XSS) vulnerabilities in cases where GeoServer uses FreeMarker templates to generate HTML output and administrators are able
to modify the templates and/or users have significant control over the output through service requests. When the
``GEOSERVER_FORCE_FREEMARKER_ESCAPING`` property is set to false, auto-escaping will delegate either to the feature's default
behavior or other settings which allow administrators to enable/disable auto-escaping on a global or per virtual service
basis. This property can be set to false either via Java system property, command line argument (-D), environment variable or
web.xml init parameter.

This setting currently applies to the WMS GetFeatureInfo HTML output format and may be applied to other applicable GeoServer
functionality in the future.

.. warning::
    While enabling auto-escaping will prevent XSS using the default templates and mitigate many cases where template authors
    are not considering XSS in their template design, it does **NOT** completely prevent template authors from creating
    templates that allow XSS (whether this is intentional or not). Additional functionality may be added in the future to
    mitigate those potential XSS vulnerabilities.

.. _production_config_external_entities:

External Entities Resolution
''''''''''''''''''''''''''''

When processing XML documents from service requests (POST requests, and GET requests with FILTER and SLD_BODY parameters) XML entity resolution is used to obtain any referenced documents. This is most commonly seen when the XML request provides the location of an XSD schema location for validation).

GeoServer provides a number of facilities to control external entity resolution:

* By default `http` and `https` entity resolution is restricted to the following default::
  
     www.w3.org|schemas.opengis.net|www.opengis.net|inspire.ec.europa.eu/schemas
     
  The default list includes the common w3c, ogc, and inspire schema locations required for OGC Web Service operation.
  
  Access is provided to the proxy base url from global settings.
  Access to local `file` references is restricted.

* To allow additional external entity `http` and `https` locations use a comma or bar separated list::

     -DENTITY_RESOLUTION_ALLOWLIST=server1|server2|server3/schemas
  
  These locations are in addition to the default w3c, ogc, and inspire schema locations above.
  Access is provided to the proxy base url from global settings.
  Access to local `file` references remains restricted.

* To allow all `http` and `https` entity resolution use `*` wildcard::

     -DENTITY_RESOLUTION_ALLOWLIST=*
  
  Access to local `file` references remains restricted.

* To turn off all restrictions (allowing ``http``, ``https``, and ``file`` references) use the global setting :ref:`config_globalsettings_external_entities`.
  
  This setting prevents ``ENTITY_RESOLUTION_ALLOWLIST`` from being used.

.. _production_config_spring_firewall:

Spring Security Firewall
''''''''''''''''''''''''

GeoServer defaults to using Spring Security's StrictHttpFirewall to help improve protection against potentially malicious
requests. However, some users will need to disable the StrictHttpFirewall if the names of GeoServer resources (workspaces,
layers, styles, etc.) in URL paths need to contain encoded percent, encoded period or decoded or encoded semicolon characters.
The ``GEOSERVER_USE_STRICT_FIREWALL`` property can be set to false either via Java system property, command line argument
(-D), environment variable or web.xml init parameter to use the more lenient DefaultHttpFirewall.

Static Web Files
''''''''''''''''

GeoServer by default allows administrators to serve static files by simply placing them in the ``www``` subdirectory of the
GeoServer data directory. If this feature is not being used to serve HTML/JavaScript files or is not being used at all, the
``GEOSERVER_DISABLE_STATIC_WEB_FILES`` property can be set to true to mitigate potential stored XSS issues with that directory.
See the :ref:`tutorials_staticfiles` page for more details.

Session Management
------------------

GeoServer defaults to managing user sessions using cookies with the ``HttpOnly`` flag set to prevent attackers from using cross-site scripting (XSS) attacks to steal
a user's session token. You can configure the session behavior by modifying the :file:`web.xml` file of your GeoServer instance.

It is strongly recommended that production environments also set the ``Secure`` flag on session cookies. This can be enabled by uncommenting the following in the :file:`web.xml`
file if the web interface is only being accessed through HTTPS but the flag may need to be set by a proxy server if the web interface needs to support both HTTP and HTTPS.

.. code-block:: xml

   <secure>true</secure>
