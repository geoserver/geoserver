.. _sec_service:

Service-level security
======================

.. note::

   Service-level security and :ref:`sec_layer` cannot be combined.  For example, it is not possible to specify access to a specific OGC service on one specific layer.

GeoServer allows access to be determined on a service level (WFS, WMS).

Access to services is linked to roles.  (See also :ref:`sec_roles`.)  Services and roles are linked in a file called ``services.properties``, which is located in the ``security`` directory in your GeoServer data directory.

Syntax
------

The syntax for setting security is as follows.  (Parameters in brackets are optional.)::

   service[.method]=role[,role2,...]

where:

* **service** can be ``wfs``, ``wms``, or ``wcs``
* **method** can be any method supported by the service. (Ex: GetFeature for WFS, GetMap for WMS)
* **role[,role2,...]** is the name(s) of predefined roles.

.. note::

   Make sure that your role is linked to a user, unless you want to deny access to everyone.  Set this in the ``users.properties`` file.

Examples
--------

By default, no service-level security is set.  Two examples are given in the ``service.properties`` file by default, commented out::

   wfs.GetFeature=ROLE_WFS_READ
   wfs.Transaction=ROLE_WFS_WRITE

The first line will link access to the WFS GetFeature method to the role ROLE_WFS_READ.  The second line will link access to the WFS Transactions to the role ROLE_WFS_WRITE.
