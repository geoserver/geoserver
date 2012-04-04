.. _sec_service:

Service Security
================

GeoServer allows for access control at the service level, allowing for the locking down of service operations to only 
authenticated users who have been granted a particular role. There are two main categories of services present in GeoServer. The first is :ref:`OWS services <services>` such as WMS and WFS. The second are RESTful services, such as :ref:`RESTconfig <rest_extension>`.

.. note:: Service-level security and :ref:`sec_layer` cannot be combined.  For example, it is not possible to specify access to a specific OWS service only for one specific layer.

OWS services
------------

Security on OWS services allows for setting access constraints globally for a particular service, or to a specific operation
within that service.  A few examples would be:

* Securing the entire WFS service so only authenticated users have access to all WFS operations
* Allowing anonymous access to read-only WFS operations such as GetCapabilities, but securing write operations such as Transaction
* Disabling the WFS service in effect by securing all operations and not applying the appropriate roles to any users

OWS service security access rules are specified in a file named :file:`services.properties`, located in the ``security`` directory in the GeoServer data directory. The file contains a list of rules mapping service operations to defined roles. The syntax for specifying rules is as follows::

   <service>.<operation|*>=<role>[,<role2>,...]

where:

* ``[]`` denotes optional parameters
* ``|`` denotes "or"
* ``service`` is the identifier of an OGC service, such as ``wfs``, ``wms``, or ``wcs``
* ``operation`` can be any operation supported by the service, examples include ``GetFeature`` for WFS, ``GetMap`` for WMS, ``*`` for all operations
* ``role[,role2,...]`` is a list of predefined role names

.. note::  It is important that roles specified are actually linked to a user, otherwise the whole service/operation will be 
   accessible to no one except for the :ref:`sec_root`. However in some cases this may be the desired effect.

The default service security configuration in GeoServer contains no rules and allows any anonymous user to access any operation of any service.  The following are some examples of desired security restrictions and the corresponding rules.

Securing the entire WFS service
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This rule allows any WFS operation only to authenticated users that have been granted the ``ROLE_WFS`` role::

  wfs.*=ROLE_WFS

Anonymous WFS access only for read-only operations
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This rule allows for anonymous access to all WFS operations (such as GetCapabilities and GetFeature) but restricts Transaction requests to only authenticated users that have been granted the ``ROLE_WFS_WRITE`` role::

  wfs.Transaction=ROLE_WFS_WRITE


Securing data-accessing WFS operations and write operations
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

These two rules when used together allow anonymous access to GetCapabilities and DescribeFeatureType, force the user to authenticate for 
the GetFeature operation (must be granted the ``ROLE_WFS_READ`` role), and force the user to authenticate to perform transactions (must be granted the ``ROLE_WFS_WRITE`` role::

   wfs.GetFeature=ROLE_WFS_READ
   wfs.Transaction=ROLE_WFS_WRITE

Note that it is not specified in this example whether a user that access to Transactions would also have access to GetFeature.

.. warning:: IS THIS TRUE!?


REST services
-------------

In addition to providing the ability to secure OWS services, GeoServer also allows for the securing of RESTful services.

REST service security access rules are specified in a file named :file:`rest.properties`, located in the ``security`` directory of the GeoServer data directory. This file contains a list of rules mapping request URIs to defined roles. The rule syntax is as follows::

   <uriPattern>;<method>[,<method>,...]=<role>[,<role>,...]

where:

* ``[]`` denote optional parameters
* ``uriPattern`` is the :ref:`ant pattern <ant_patterns>` that matches a set of request URIs 
* ``method`` is an HTTP request method, one of ``GET``, ``POST``, ``PUT``, ``POST``, ``DELETE``, or ``HEAD``
* ``role`` is the name of a predefined role. The wildcard ``*`` is used to indicate all users, including anonymous users.

A few things to note:

* URI patterns should account for the first component of the rest path, usually ``rest`` or ``api``
* ``method`` and ``role`` lists should **not** contain any spaces

.. _ant_patterns:

Ant patterns
~~~~~~~~~~~~

Ant patterns are a commonly used syntax for pattern matching directory and file paths. The examples below contain some basic examples. The apache ant `user manual <http://ant.apache.org/manual/dirtasks.html>`_ contains more sophisticated cases.

The following examples are specific to :ref:`RESTconfig <rest_extension>`, but any RESTful GeoServer service can be configured in the same manner.

Disabling anonymous access to services
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The most secure of configurations is one that forces any request, REST or otherwise, to be authenticated.  The following will lock down access to all requests to users that are granted the ``ROLE_ADMINISTRATOR`` role::

   /**;GET,POST,PUT,DELETE=ROLE_ADMINISTRATOR

A slightly less restricting configuration locks down access to operations under the path ``/rest`` to users granted the ``ROLE_ADMINISTRATOR`` role, but will allow anonymous access to requests that fall under other paths (for example ``/api``)::

   /rest/**;GET,POST,PUT,DELETE=ROLE_ADMINISTRATOR

Allowing anonymous read-only access
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The following configuration allows for anonymous access when the ``GET`` method is used, but forces authentication for a ``POST``, ``PUT``, or ``DELETE`` method::

   /**;GET=IS_AUTHENTICATED_ANONYMOUSLY
   /**;POST,PUT,DELETE=TRUSTED_ROLE

.. warning:: ARE THESE ROLES PREDEFINED?


Securing a specific resource
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The following configuration forces authentication for access to a particular resource (in this case the ``states`` feature type)::

  /rest/**/states*;GET=TRUSTED_ROLE
  /rest/**;POST,PUT,DELETE=TRUSTED_ROLE

The following secures access to a set of resources (in this case all data stores).::

  /rest/**/datastores/*;GET=TRUSTED_ROLE
  /rest/**/datastores/*.*;GET=TRUSTED_ROLE
  /rest/**;POST,PUT,DELETE=TRUSTED_ROLE

.. warning:: ARE THESE ROLES PREDEFINED?

Note the trailing wildcards ``/*`` and ``/*.*``.