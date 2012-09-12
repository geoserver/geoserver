.. _webadmin_basics:

Interface basics
================

This section will introduce the basic concepts of the web administration interface (generally abbreviated to "web admin" .)

Welcome Page
------------

For most installations, GeoServer will start a web server on localhost at port 8080, accessible at the following URL::

   http://localhost:8080/geoserver/web

.. note:: This URL is dependent on your installation of GeoServer. When using the WAR installation, for example, the URL will be dependent on your container setup.

When correctly configured, a welcome page will open in your browser.

.. figure:: images/web-admin.png
   :align: center
   
   *Welcome Page*
   
The welcome page contains links to various areas of the GeoServer configuration. The :guilabel:`About GeoServer` section in the :guilabel:`Server` menu provides external links to the GeoServer documentation, homepage, and bug tracker. The page also provides login access to the geoserver console. This security measure prevents unauthorized users from making changes to your GeoServer configuration. The default username and password is ``admin`` and ``geoserver``. These can be changed only by editing the :file:`security/users.properties` file in the :ref:`data_directory`. 

.. figure:: images/8080login.png
   :align: center
   
   *Login*

Regardless of authorization access, the web admin menu links to the :guilabel:`Demo` and :guilabel:`Layer Preview` portion of the console. The :ref:`webadmin_demos` page contains links to various information pages, while the :ref:`layerpreview` page provides spatial data in various output formats.

When logged on, additional options will be presented.

.. figure:: images/welcome_logged_in.png
   :align: center
   
   *Additional options when logged in*

Geoserver Web Coverage Service (WCS), Web Feature Service (WFS), and Web Map Service (WMS) configuration specifications can be accessed from this welcome page as well. For further information, please see the section on :ref:`services`.