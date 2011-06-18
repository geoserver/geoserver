.. _data_dir_setting:

Setting the Data Directory
==========================

Setting up a GeoServer data directory is dependent on the type of GeoServer installation. Follow the instructions below specific to the target platform. 

Windows
-------

On Windows platforms the location of the GeoServer data directory is controlled by the ``GEOSERVER_DATA_DIR`` environment variable. 

.. note::
  
   When the ``GEOSERVER_DATA_DIR`` environment variable is not set, the directory ``data_dir`` under the root of the GeoServer installation is used.

To set the ``GEOSERVER_DATA_DIR``:


On **Windows XP** systems:

#. From the Desktop or Start Menu right-click the ``My Computer`` icon and select ``Properties``. 

#. On the resulting dialog select the ``Advanced`` tab and click the ``Environment Variables`` button.

#. Click the ``New`` button and create a environment variable called ``GEOSERVER_DATA_DIR`` and set it to the desired location.

   .. image:: geoserver_data_dir.png
      :align: center


On **Windows Vista** systems:


Linux
-----

On Linux platforms the location of the GeoServer data directory is controlled by the ``GEOSERVER_DATA_DIR`` environment variable. Setting the variable can be achieved with the following command (in a bash shell)::

    % export GEOSERVER_DATA_DIR=/var/lib/geoserver_data

Place the command in the ``.bash_profile`` or ``.bashrc`` file (again assuming a bash shell). Ensure that this done for the user GeoServer will be run by.


Mac OS X
--------

If running the binary version of GeoServer on Mac OS X then the data directory is set in the exact same way as linux. 

If using the Mac OS X binary, then set the GEOSERVER_DATA_DIR environment variable to the file location.  See 
`this page <http://developer.apple.com/mac/library/qa/qa2001/qa1067.html>`_ for details on how to set an environment variable in Mac OS X


Web Archive
-----------

When running GeoServer inside of a servlet container the data directory can be specified in a number of ways. The *recommended* method is to set a *servlet context parameter*. An alternative is to set a Java System Property.

Servlet context parameter
^^^^^^^^^^^^^^^^^^^^^^^^^

Servlet context parameter's are specified in the ``WEB-INF/web.xml`` file for the GeoServer application::

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

Depending on the servlet container used it is also possible to specify the data directory location with a Java System Property. This method can be useful during upgrades, as it prevents the need to set the data directory on every single upgrade.

.. warning::

   Using a system property will typically set the property for all applications running in the servlet container, not just GeoServer.

Setting the Java System Property is dependent on the servlet container. 

In **Tomcat**:

Edit the file ``bin/setclasspath.sh`` under the root of the Tomcat installation. Specify the ``GEOSERVER_DATA_DIR`` system property by setting the ``CATALINA_OPTS`` variable::

   CATALINA_OPTS="-DGEOSERVER_DATA_DIR=/var/lib/geoserver_data"


In **Glassfish**:

Edit the file ``domains/<<domain>>/config/domain.xml`` under the root of the Glassfish installation, where ``<<domain>>`` refers to the domain that the GeoServer web application is deployed under. Add a ``<jvm-options>`` inside of the ``<java-config>`` element::

   ...
   <java-config>
      ...
     <jvm-options>-DGEOSERVER_DATA_DIR=/var/lib/geoserver_data</jvm-options>  
   </java-config>
   ...

