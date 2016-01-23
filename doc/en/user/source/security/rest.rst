.. _security_rest:

REST Security
=============

In addition to providing the ability to secure OWS style services, GeoServer also supports securing RESTful services.

As with layer and service security, RESTful security configuration is based on ``security_roles``. The mapping of request URI to role is defined in a file named ``rest.properties``, located in the ``security`` directory of the GeoServer data directory.

Syntax
------

The following syntax defines access control rules for RESTful services (parameters in brackets [] are optional)::

  uriPattern;method[,method,...]=role[,role,...]

The parameters are:

* **uriPattern**—:ref:`ant pattern <security_rest_ant_patterns>` that matches a set of request URIs
* **method**—HTTP request method, one of ``GET``, ``POST``, ``PUT``, ``POST``, ``DELETE``, or ``HEAD``
* **role**—Name of a predefined role. The wildcard '* is used to indicate the permission is applied to all users, including anonymous users.

.. note::

   * URI patterns should account for the first component of the rest path, usually ``rest`` or ``api``
   * Method and role lists should **not** contain any spaces

.. _security_rest_ant_patterns:

Ant patterns
~~~~~~~~~~~~

Ant patterns are commonly used for pattern matching directory and file paths. The :ref:`examples <security_rest_examples>` section contains some basic instructions. The Apache ant `user manual <http://ant.apache.org/manual/dirtasks.html>`_ contains more sophisticated use cases.

.. _security_rest_examples:

Examples
--------

Most of the examples in this section are specific to the GeoServer :ref:`rest` but any RESTful GeoServer service may be configured in the same manner.

Allowing only authenticated access
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The most secure configuration is one that forces any request to be authenticated. The following example locks down access to all requests::

   /**;GET,POST,PUT,DELETE=ROLE_ADMINISTRATOR

A less restricting configuration locks down access to operations under the path ``/rest``, but will allow anonymous access to requests that fall under other paths (for example ``/api``)::

   /rest/**;GET,POST,PUT,DELETE=ROLE_ADMINISTRATOR

The following configuration is similar to the previous one except it grants access to a specific role rather than the administrator::

   /**;GET,POST,PUT,DELETE=ROLE_TRUSTED

``ROLE_TRUSTED`` is a role defined in ``users.properties``.

Providing anonymous read-only access
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The following configuration allows anonymous access when the ``GET`` (read) method is used but forces authentication for a ``POST``, ``PUT``, or ``DELETE`` (write)::

   /**;GET=IS_AUTHENTICATED_ANONYMOUSLY
   /**;POST,PUT,DELETE=TRUSTED_ROLE

Securing a specific resource
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The following configuration forces authentication for access to a particular resource (in this case a feature type)::

  /rest/**/states*;GET=TRUSTED_ROLE
  /rest/**;POST,PUT,DELETE=TRUSTED_ROLE

The following secures access to a set of resources (in this case all data stores)::

  /rest/**/datastores/*;GET=TRUSTED_ROLE
  /rest/**/datastores/*.*;GET=TRUSTED_ROLE
  /rest/**;POST,PUT,DELETE=TRUSTED_ROLE
