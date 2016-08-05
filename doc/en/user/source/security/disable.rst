.. _security_disable:

Disabling security
==================

If you are using an external security subsystem, you may want to disable the built-in security to 
prevent conflicts. Disabling security is possible for each security filter chain individually. The
security filter chains are listed on the GeoServer authentication page.
 

.. warning::

   Disabling security for a filter chain results in administrator privileges for each HTTP request
   matching this chain. As an example, disabling security on the **web** chain gives administrative
   access to each user accessing the :ref:`web_admin` interface.

