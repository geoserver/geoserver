.. _sec_disable:

Disabling security
==================

If you are using an external security subsystem, you may want to disable the built-in security to prevent conflicts. 

.. warning::

  Beware!  If security is disabled, you'll have to make sure the external one locks down the administration interface, otherwise it will be completely unlocked!

To disable GeoServer security, first shut down GeoServer, open the ``web.xml`` file (located inside the ``WEB-INF`` directory) and comment out the "Spring Security Filter Chain Proxy" filter definition parameters.  These two pieces of code should look something like this:

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
   
Comment these sections out.  When GeoServer is restarted, the internal security subsystem will be completely disabled.
