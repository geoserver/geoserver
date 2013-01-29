.. _sec_disable:

Disabling security
==================

If you are using an external security subsystem, you may want to disable the built-in security to prevent conflicts. 

.. warning::

   If internal security is disabled, make sure the external security system locks down the :ref:`web_admin`, otherwise it will be completely unlocked, allowing unrestricted administrative access.

To disable GeoServer security, first shut down GeoServer, open the ``web.xml`` file (located inside the ``WEB-INF`` directory) and comment out the two **Spring Security Filter Chain Proxy** filter definition parameters.  These two lines of code should look something like this:

.. code-block:: xml 

   <filter>
      <filter-name>Spring Security Filter Chain Proxy</filter-name>
      <filter-class>org.springframework.security.util.FilterToBeanProxy</filter-class>
      <init-param>
         <param-name>targetClass</param-name>
         <param-value>org.springframework.security.util.FilterChainProxy</param-value>
      </init-param>
   </filter>
   
.. code-block:: xml 

   <filter-mapping>
      <filter-name>Spring Security Chain Proxy</filter-name>
      <url-pattern>/*</url-pattern>
   </filter-mapping>
   
When these sections are commented out, restart GeoServer, and the internal security subsystem will be completely disabled.
