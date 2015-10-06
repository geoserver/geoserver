.. _security:

Security
========

This section details the security subsystem in GeoServer, which is based on `Spring Security <http://static.springsource.org/spring-security/site/>`_. For web-based configuration, please see the section on :ref:`webadmin_security` in the :ref:`web_admin`.

As of GeoServer 2.2.0, the security subsystem has been completely re-engineered, providing a more secure and flexible authentication framework. This rework is largely based on a Christian Müeller's 
masters thesis entitled `Flexible Authentication for Stateless Web Services <http://old.geoserver.org/attachments/52625410/52756481.pdf>`_. It is good reading to help understanding many of the new concepts introduced. 

.. toctree::
   :maxdepth: 2

   usergrouprole/index
   auth/index
   passwd
   root
   service
   layer
   rest
   disable
   tutorials/index
