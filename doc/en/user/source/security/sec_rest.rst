.. _sec_rest:

REST Security
=============

.. note::

   RESTful security configuration is available in GeoServer versions greater than 2.0.1.

In addition to providing the ability to secure OWS style services GeoServer also allows for the securing of RESTful services.

As with layer and service security, RESTful security configuration is based on :ref:`sec_roles`. Mappings from request uri to role are defined in a file named ``rest.properties`` located in the ``security`` directory of the GeoServer data directory.

Syntax
------

The following is the syntax for definiing access control rules for RESTful services (parameters in brackets [] are optional)::

  uriPattern;method[,method,...]=role[,role,...]

where:

* **uriPattern** is the :ref:`ant pattern <ant_patterns>` that matches a set of request uri's.. 
* **method** is an HTTP request method, one of ``GET``, ``POST``, ``PUT``, ``POST``, ``DELETE``, or ``HEAD``
* **role** is the name of a predefined role. The wildcard '* is used to indicate the permission is applied to all users, including anonymous users.

A few things to note:

* uri patterns should account for the first component of the rest path, usually ``rest`` or ``api``
* method and role lists should **not** contain any spaces

.. _ant_patterns:

Ant patterns
````````````

Ant patterns are a commonly used syntax for pattern matching directory and file paths. The :ref:`examples <examples>` section contains some basic examples. The apache ant `user manual <http://ant.apache.org/manual/dirtasks.html>`_ contains more sophisticated cases.

.. _examples:

Examples
--------

Most of the examples in this section are specific to the :ref:`rest configuration extension <rest_extension>` but any RESTful GeoServer service can be configured in the same manner.

Allowing only autenticated access to services
`````````````````````````````````````````````

The most secure of configurations is one that forces any request to be authenticated. The following will lock down access to all requests::

   /**;GET,POST,PUT,DELETE=ROLE_ADMINISTRATOR

A slightly less restricting configuration locks down access to operations under the path ``/rest``, but will allow anonymous access to requests that fall under other paths (for example ``/api``)::

   /rest/**;GET,POST,PUT,DELETE=ROLE_ADMINISTRATOR

The following configuration is like the previous except it grants access to a specific role rather than the administrator::

   /**;GET,POST,PUT,DELETE=ROLE_TRUSTED

Where ``ROLE_TRUSTED`` is a role defined in ``users.properties``.

Providing anonymous read-only access
````````````````````````````````````

The following configuration allows anonymous access when the ``GET`` (read) method is used but forces authentication for a ``POST``, ``PUT``, or ``DELETE`` (write)::

   /**;GET=IS_AUTHENTICATED_ANONYMOUSLY
   /**;POST,PUT,DELETE=TRUSTED_ROLE

Securing a specific resource
````````````````````````````

The following configuration forces authentication for access to a particular resource (in this case a feature type)::

  /rest/**/states*;GET=TRUSTED_ROLE
  /rest/**;POST,PUT,DELETE=TRUSTED_ROLE

The following secures access to a set of resources (in this case all data stores)::

  /rest/**/datastores/*;GET=TRUSTED_ROLE
  /rest/**/datastores/*.*;GET=TRUSTED_ROLE
  /rest/**;POST,PUT,DELETE=TRUSTED_ROLE
