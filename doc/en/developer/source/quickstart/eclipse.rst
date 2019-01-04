.. _quickstart_eclipse:

Maven Eclipse Plugin Quickstart
===============================

This guide is designed to get developers up and running as quick as possible. For a more comprehensive guide see the the :ref:`eclipse_guide`.

.. include:: checkout.txt

Generate Eclipse project files with Maven
-----------------------------------------

Generate the eclipse ``.project`` and  ``.classpath`` files::

  mvn eclipse:eclipse

Import modules into Eclipse
---------------------------

#. Run the Eclipse IDE
#. Open the Eclipse ``Preferences``
#. Navigate to ``Java``, ``Build Path``, ``Classpath Variables`` and click
   ``New...``

   .. image:: img/eclipse_m2repo1.jpg

#. Create a classpath variable named "M2_REPO" and set the value to the location
   of the local Maven repository, and click ``Ok``

   .. image:: img/eclipse_m2repo2.jpg

#. Click ``Ok`` to apply the new Eclipse preferences
#. Right-click in the ``Package Explorer`` and click ``Import...``

   .. image:: img/eclipse_import1.jpg
      :width: 300

#. Select ``Existing Projects into Workspace`` and click ``Next``

   .. image:: img/eclipse_import2.jpg
      :width: 400

#. Navigate to the ``geoserver/src`` directory
#. Ensure all modules are selected and click ``Finish``

   .. image:: img/eclipse_import3.jpg
      :width: 350

Run GeoServer from Eclipse
--------------------------

#. From the ``Package Explorer`` select the ``web-app`` module
#. Navigate to the ``org.geoserver.web`` package
#. Right-click the ``Start`` class and navigate to ``Run as``, ``Java Application``

   .. image:: img/eclipse_run1.jpg
      :width: 600

#. After running the first time you can return to the ``Run Configurations`` dialog
   to fine tune your launch environment (including setting a GEOSERVER_DATA_DIR).

.. note::
   
   If you already have a server running on localhost:8080 see the :ref:`eclipse_guide` for instructions on changing to a different port.

Run GeoServer with Extensions
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The above instructions assume you want to run GeoServer without any extensions enabled. In cases where you do need certain extensions, the ``web-app`` module declares a number of profiles that will enable specific extensions when running ``Start``. To enable an extension, re-generate the root eclipse profile with the appropriate maven profile(s) enabled::

  % mvn eclipse:eclipse -P wps

The full list of supported profiles can be found in ``src/web/app/pom.xml``.

Access GeoServer front page
---------------------------

* After a few seconds, GeoServer should be accessible at: `<http://localhost:8080/geoserver>`_
* The default ``admin`` password is ``geoserver``.