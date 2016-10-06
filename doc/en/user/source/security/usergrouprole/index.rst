.. _security_rolesystem:

Role system
===========

Security in GeoServer is based on a **role-based system**, with roles created to serve particular functions. Examples of roles sporting a particular function are those accessing the Web Feature Service (WFS), administering the :ref:`web_admin`, and reading a specific layer. Roles are assigned to users and groups of users, and determine what actions those users or groups are permitted to do. A user is authorized through :ref:`security_auth`.

.. toctree::
   :maxdepth: 2

   usergroups
   usergroupservices
   roles
   roleservices
   rolesource
   interaction
