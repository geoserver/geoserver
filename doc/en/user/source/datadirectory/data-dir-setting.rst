.. _data_dir_setting:

Setting the Data Directory
==========================

Setting the location of the GeoServer data directory is dependent on the type of GeoServer installation. Follow the instructions below specific to the target platform. 

.. note::
  
   If the location of the GeoServer data directory is not set explicitly, the directory ``data_dir`` under the root of the GeoServer installation is used by default.

Windows
-------

On Windows platforms the location of the GeoServer data directory is controlled by the ``GEOSERVER_DATA_DIR`` environment variable. 

#. Open the System properties dialog
   
   * Windows: From the Desktop or Start Menu right click and select ``Properties`` to open the ``System`` control panel. With the ``System`` control panel open click on the ``Advanced System Settings`` link to open the ``System Properties``.
   * Windows XP: From the Desktop or Start Menu right-click the ``My Computer`` icon and select ``Properties`` to open ``System Properties``.
   
#. From ``System Properties`` click on the ``Advanced`` tab and click the ``Environmental Variables`` button.

#. Click the ``New`` button and create a environment variable called ``GEOSERVER_DATA_DIR`` and set it to the desired location.

   .. image:: geoserver_data_dir.png
      :align: center

Linux
-----

On Linux platforms the location of the GeoServer data directory is controlled by the ``GEOSERVER_DATA_DIR`` environment variable. Setting the variable can be achieved with the following command (in a bash shell)::

    % export GEOSERVER_DATA_DIR=/var/lib/geoserver_data

Place the command in the ``.bash_profile`` or ``.bashrc`` file (again assuming a bash shell). Ensure that this done for the user running GeoServer.

Mac OS X
--------

Binary Install
^^^^^^^^^^^^^^

For the binary install of GeoServer on Mac OS X, the data directory is set in the same way as for Linux. 

Mac OS X Install
^^^^^^^^^^^^^^^^

For the Mac OS X install, set the ``GEOSERVER_DATA_DIR`` environment variable to the desired directory location. 
See `this page <http://developer.apple.com/mac/library/qa/qa2001/qa1067.html>`_ for details on how to set an environment variable in Mac OS X


Web Archive
-----------

When running a GeoServer WAR inside a servlet container the data directory can be specified in a number of ways. The recommended method is to set a **servlet context parameter**. An alternative is to set a **Java system property**.

Servlet context parameter
^^^^^^^^^^^^^^^^^^^^^^^^^

To specify the data directory using a servlet context parameter, create the following ``<context-param>`` element in the ``WEB-INF/web.xml`` file for the GeoServer application::

   <web-app>
     ...
     <context-param>
       <param-name>GEOSERVER_DATA_DIR</param-name>
       <param-value>/var/lib/geoserver_data</param-value>
     </context-param>
     ...
   </web-app>

Java system property
^^^^^^^^^^^^^^^^^^^^

It is also possible to specify the data directory location with a Java system property. This method can be useful during upgrades, as it avoids the need to set the data directory after every upgrade.

.. warning::

   Using a Java system property will typically set the property for all applications running in the servlet container, not just GeoServer.

The method of setting the Java system property is dependent on the servlet container:

For **Tomcat**:

Edit the file ``bin/setclasspath.sh`` under the root of the Tomcat installation. Specify the ``GEOSERVER_DATA_DIR`` system property by setting the ``CATALINA_OPTS`` variable::

   CATALINA_OPTS="-DGEOSERVER_DATA_DIR=/var/lib/geoserver_data"


For **Glassfish**:

Edit the file ``domains/<<domain>>/config/domain.xml`` under the root of the Glassfish installation, where ``<<domain>>`` refers to the domain that the GeoServer web application is deployed under. Add a ``<jvm-options>`` element inside the ``<java-config>`` element::

   ...
   <java-config>
      ...
     <jvm-options>-DGEOSERVER_DATA_DIR=/var/lib/geoserver_data</jvm-options>  
   </java-config>
   ...

