.. _security_layer:

Layer security
==============

GeoServer allows access to be determined on a per-layer basis.

.. note::  Layer security and :ref:`security_service` cannot be combined. For example, it is not possible to specify access to a specific OWS service, only for one specific layer.

Providing access to layers is linked to :ref:`roles <security_rolesystem_roles>`. Layers and roles are linked in a file called ``layers.properties``, which is located in the ``security`` directory in your GeoServer data directory. The file contains the rules that control access to workspaces and layers.

Rules
-----

The syntax for a layer security rule is as follows (``[]`` denotes optional
parameters)::

  workspace.layer.permission=role[,role2,...]

The parameters include::

* ``workspace``—Name of the workspace. The wildcard ``*`` is used to indicate all workspaces.
* ``layer``—Name of a resource (featuretype/coverage/etc...). The wildcard ``*`` is used to indicate all layers.
* ``permission``—Type of access permission/mode. 
   
   * ``r``—Read access
   * ``w``—Write access
   * ``a``—Admin access
   
   See :ref:`access_mode` for more details.
   
* ``role[,role2,...]`` is the name(s) of predefined roles. The wildcard ``*`` is used to indicate the permission is applied to all users, including anonymous users.

.. note:: 

   If a workspace or layer name is supposed to contain dots, they can be escaped using double backslashes (``\\``). For example, if a layer is named ``layer.with.dots`` the following syntax for a rule may be used::

     topp.layer\\.with\\.dots.r=role[,role2,...]

Each entry must have a unique combination of workspace, layer, and permission values. If a permission at the global level is not specified, global permissions are assumed to allow read/write access. If a permission for a workspace is not specified, it inherits permissions from the global specification. If a permission for a layer is not specified, it inherits permissions from its workspace specification. If a user belongs to multiple roles, the **least restrictive** permission they inherit will apply.

Catalog Mode
------------

The ``layers.properties`` file may contain a further directive that specifies how GeoServer will advertise secured layers and behave when a secured layer is accessed without the necessary privileges. The parameter is ``mode`` and is commonly referred to as the "catalog mode".

The syntax is::

   mode=option

``option`` may be one of three values:

.. list-table::
   :widths: 20 80
   :header-rows: 1

   * - Option
     - Description
   * - ``hide``
     - *(Default)* Hides layers that the user does not have read access to, and behaves as if a layer is read only if the user does not have write permissions. The capabilities documents will not contain the layers the current user cannot access. This is the highest security mode. As a result, it may not work very well with clients such as uDig or Google Earth.
   * - ``challenge``
     - Allows free access to metadata, but any attempt at accessing actual data is met by a HTTP 401 code (which forces most clients to show an authentication dialog). The capabilities documents contain the full list of layers. DescribeFeatureType and DescribeCoverage operations work successfully. This mode works fine with clients such as uDig or Google Earth.
   * - ``mixed``
     - Hides the layers the user cannot read from the capabilities documents, but triggers authentication for any other attempt to access the data or the metadata. This option is useful if you don't want the world to see the existence of some of your data, but you still want selected people to who have data access links to get the data after authentication.

.. _access_mode:

Access modes
------------

The access mode defines what level of access should be granted on a specific workspace/layer to a particular role. There are three types of access mode:

* ``r``—**Read mode** (read data from a workspace/layer)
* ``w``—**Write mode** (write data to a workspace/layer)
* ``a``—**Admin mode** (access and modify the configuration of a workspace/layer)

Some notes on the above access modes:

* Write does not imply Read, but Admin implies both Write *and* Read.
* Read and Write apply to the data of a layer, while Admin applies to the configuration of a layer.
* As Admin mode only refers to the configuration of the layer, it is not required for any OGC service request.

.. note:: Currently, it is possible to assign Admin permission only to an entire workspace, and not to specific layers.
   
Examples
--------

The following examples illustrate some possible layer restrictions and the corresponding rules.

Protecting a single workspace and a single layer
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The following example demonstrates how to configure GeoServer as a primarily a read-only server::

   *.*.r=*
   *.*.w=NO_ONE
   private.*.r=TRUSTED_ROLE
   private.*.w=TRUSTED_ROLE
   topp.congress_district.w=STATE_LEGISLATORS

The mapping of roles to permissions is as follows:

.. list-table::
   :widths: 20 20 20 20 20
   :header-rows: 1

   * - Role
     - private.*
     - topp.*
     - topp.congress_district
     - (all other workspaces)
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

The following example demonstrates how to lock down GeoServer::

   *.*.r=TRUSTED_ROLE
   *.*.w=TRUSTED_ROLE
   topp.*.r=*
   army.*.r=MILITARY_ROLE,TRUSTED_ROLE
   army.*.w=MILITARY_ROLE,TRUSTED_ROLE

The mapping of roles to permissions is as follows:

.. list-table::
   :widths: 25 25 25 25
   :header-rows: 1

   * - Role
     - topp.*
     - army.*
     - (All other workspaces)
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

Providing restricted administrative access
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The following provides administrative access on a single workspace to a specific role, in additional to the full administrator role::

  *.*.a=ROLE_ADMINISTRATOR
  topp.*.a=ROLE_TOPP_ADMIN,ROLE_ADMINISTRATOR

Managing multi-level permissions
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The following example demonstrates how to configure GeoServer with global-, workspace--, and layer-level permissions::

   *.*.r=TRUSTED_ROLE
   *.*.w=NO_ONE
   topp.*.r=*
   topp.states.r=USA_CITIZEN_ROLE,LAND_MANAGER_ROLE,TRUSTED_ROLE
   topp.states.w=NO_ONE
   topp.poly_landmarks.w=LAND_MANAGER_ROLE
   topp.military_bases.r=MILITARY_ROLE
   topp.military_bases.w=MILITARY_ROLE

The mapping of roles to permissions is as follows:

.. list-table::
   :widths: 25 15 15 15 15 15
   :header-rows: 1

   * - Role
     - topp.states
     - topp.poly_landmarks
     - topp.military_bases
     - topp.(all other layers)
     - (All other workspaces)
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

.. note:: The entry ``topp.states.w=NO_ONE`` is not required because this permission would be inherited from the global level (the entry ``*.*.w=NO_ONE``).


Invalid configuration
~~~~~~~~~~~~~~~~~~~~~

The following examples are invalid because the workspace, layer, and permission combinations are not unique::

   topp.state.rw=ROLE1
   topp.state.rw=ROLE2,ROLE3

