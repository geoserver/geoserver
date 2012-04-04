.. _sec_layer:

Layer security
==============

GeoServer allows access to be determined on a per-layer basis.

.. note::  Layer security and :ref:`sec_service` cannot be combined.  For example, it is not possible to specify access to a specific OWS service only for one specific layer.

Access to layers are linked to :ref:`roles <sec_rolesystem_roles>`.  Layers and roles are linked in a file called ``layers.properties``, which is located in the ``security`` directory in your GeoServer data directory.

The syntax for specifying layer security is as follows::

  namespace.layer.permission=role[,role2,...]

where:

* ``[]`` denotes optional parameters
* ``namespace`` is the name of the namespace. The wildcard ``*`` is used to indicate all namespaces.
* ``layer`` is the name of a featuretype or coverage. The wildcard ``*`` is used to indicate all layers.
* ``permission`` is the type of access permission (``r`` for read access, ``w`` for write access).
* ``role[,role2,...]`` is the name(s) of predefined roles. The wildcard ``*`` is used to indicate the permission is applied to all users, including anonymous users.

.. note:: 

   If a namespace or layer name is supposed to contain dots they can be escaped using double backslashes (``\\``). For example, if a layer is named ``layer.with.dots`` the following syntax for a rule can be used::

     topp.layer\\.with\\.dots.r=role[,role2,...]

Each entry must have a unique combination of namespace, layer, and permission values.  If a permission at the global level is not specified, global permissions are assumed to allow read/write access.  If a permission for a namespace is not specified, it inherits permissions from the global specification.  If a permission for a layer is not specified, it inherits permissions from its namespace specification.  If a user belongs to multiple roles, the **least restrictive** permission they inherit will apply.

The ``layers.properties`` file may contain a further directive that specifies the way in which GeoServer will advertise secured layers and behave when a secured layer is accessed without the necessary privileges. The line is::

   mode=option

where ``option`` can be one of three values:

.. list-table::
   :widths: 20 80

   * - **Option**
     - **Description**
   * - ``hide``
     - *(Default)* Hides layers that the user does not have read access to, and behaves as if a layer is read only if the user does not have write permissions. The capabilities documents will not contain the layers the current user cannot access. This is the highest security mode.  Because of this, it can sometimes not work very well with clients such as uDig or Google Earth.
   * - ``challenge``
     - Allows free access to metadata, but any attempt at accessing actual data is met by a HTTP 401 code (which forces most clients to show an authentication dialog). The capabilities documents contain the full list of layers.  DescribeFeatureType and DescribeCoverage operations work successfully.  This mode works fine with clients such as uDig or Google Earth.
   * - ``mixed``
     - Hides the layers the user cannot read from the capabilities documents, but triggers authentication for any other attempt to access the data or the metadata. This option is useful if you don't want the world to see the existence of some of your data, but you still want selected people to who have data access links to get the data after authentication.



Examples
--------

The following are some examples of desired layer restrictions and the corresponding rules.

Protecting a single namespace and a single layer
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The following entries demonstrate configuring GeoServer so that it is primarily a read-only server::

   *.*.r=*
   *.*.w=NO_ONE
   private.*.r=TRUSTED_ROLE
   private.*.w=TRUSTED_ROLE
   topp.congress_district.w=STATE_LEGISLATORS

In this example, here is the map of roles to permissions:

.. list-table::
   :widths: 20 20 20 20 20
   :header-rows: 1

   * - Role
     - private.*
     - topp.*
     - topp.congress_district
     - (all other namespaces)
   * - ``NO_ONE``
     - (none)
     - w
     - (none)
     - w
   * - ``TRUSTED_ROLE``
     - r/w
     - r
     - r
     - r
   * - ``STATE_LEGISLATURES``
     - (none)
     - r
     - r/w
     - r
   * - (All other users)
     - r
     - r
     - r
     - r

Locking down GeoServer
~~~~~~~~~~~~~~~~~~~~~~

The following entries demonstrate configuring GeoServer so that it is locked down::

   *.*.r=TRUSTED_ROLE
   *.*.w=TRUSTED_ROLE
   topp.*.r=*
   army.*.r=MILITARY_ROLE,TRUSTED_ROLE
   army.*.w=MILITARY_ROLE,TRUSTED_ROLE

In this example, here is the map of roles to permissions:

.. list-table::
   :widths: 25 25 25 25
   :header-rows: 1

   * - Role
     - topp.*
     - army.*
     - (All other namespaces)
   * - ``TRUSTED_ROLE``
     - r/w
     - r/w
     - r/w
   * - ``MILITARY_ROLE``
     - r
     - r/w
     - (none)
   * - (All other users)
     - r
     - (none)
     - (none)

A more complex situation
~~~~~~~~~~~~~~~~~~~~~~~~

The following entries demonstrate configuring GeoServer with global-, namespace--, and layer-level permissions::

   *.*.r=TRUSTED_ROLE
   *.*.w=NO_ONE
   topp.*.r=*
   topp.states.r=USA_CITIZEN_ROLE,LAND_MANAGER_ROLE,TRUSTED_ROLE
   topp.states.w=NO_ONE
   topp.poly_landmarks.w=LAND_MANAGER_ROLE
   topp.military_bases.r=MILITARY_ROLE
   topp.military_bases.w=MILITARY_ROLE

In this example, here is the map of roles to permissions:

.. list-table::
   :widths: 25 15 15 15 15 15
   :header-rows: 1

   * - Role
     - topp.states
     - topp.poly_landmarks
     - topp.military_bases
     - topp.(all other layers)
     - (All other namespaces)
   * - ``NO_ONE``
     - w
     - r
     - (none)
     - w
     - w
   * - ``TRUSTED_ROLE``
     - r
     - r
     - (none)
     - r
     - r
   * - ``MILITARY_ROLE``
     - (none)
     - r
     - r/w
     - r
     - (none)
   * - ``USA_CITIZEN_ROLE``
     - r
     - r
     - (none)
     - r
     - (none)
   * - ``LAND_MANAGER_ROLE``
     - r
     - r/w
     - (none)
     - r
     - (none)
   * - (All other users)
     - (none)
     - r
     - (none)
     - r
     - (none)

.. note:: The entry ``topp.states.w=NO_ONE`` is not actually needed, because this permission would be inherited from the global level, i.e. the line ``*.*.w=NO_ONE``.


Invalid configuration
~~~~~~~~~~~~~~~~~~~~~

The following set of entries would not be valid because the namespace, layer, and permission combinations of the entries are not unique::

   topp.state.rw=ROLE1
   topp.state.rw=ROLE2,ROLE3

