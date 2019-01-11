.. _quickstart_intellij:

Intellij Quickstart
===================

.. include:: checkout.txt

Import modules into Intellij
----------------------------

#. Run the Intellij IDE
#. Select ``File -> New -> Project from Existing Sources...``. 
#. Navigate to the ``geoserver/src/pom.xml`` directory and click ``Open``.

   .. image:: img/intellij_import.png
      :width: 600

#. Click ``Next``, leaving all profiles unchecked.

   .. image:: img/intellij_import_profile.png
      :width: 600

#. Click ``Next``, leaving the geoserver project checked.

   .. image:: img/intellij_import_project.png
      :width: 600

#. Click ``Next``, selecting the Java 8 JDK of your choice.

   .. image:: img/intellij_import_jdk.png
      :width: 600

#. Click ``Finish``

   .. image:: img/intellij_import_finish.png
      :width: 600

Run GeoServer from Intellij
---------------------------

#. From the Project browser select the ``web-app`` module
#. Navigate to the ``org.geoserver.web`` package
#. Right-click the ``Start`` class and click to ``Run 'Start.main()'``

   .. image:: img/intellij_run.png
      :width: 400

#. The first time you do this, geoserver will fail to start. Navigate to the ``Run`` menu, an click ``Edit Configurations...``.
#. Select the ``Start`` configuration, and append ``web/app`` to the ``Working Directory``.

   .. image:: img/intellij_run_config.png
      :width: 800

#. While you have the ``Edit Configurations`` dialog open, you can fine tune your launch environment (including setting a GEOSERVER_DATA_DIR). When you are happy with your settings, click ``OK``.
#. If there are errors such as "cannot find symbol class ASTAxisId", some generated code is not being included in the build.  Using wcs1_1 as the working directory, run a ``mvn clean install``.  
#. You can now re-run GeoServer. Select ``Run -> Run 'Start'``

.. note::
   
   If you already have a server running on localhost:8080 see the :ref:`eclipse_guide` for instructions on changing to a different port.

Run GeoServer with Extensions
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The above instructions assume you want to run GeoServer without any extensions enabled. In cases where you do need certain extensions, the ``web-app`` module declares a number of profiles that will enable specific extensions when running ``Start``. To enable an extension, open the ``Maven Projects`` window (``View -> Tool Windows -> Maven Projects``) and select the profile(s) you want to enable.

   .. image:: img/intellij_run_profile.png
      :width: 300

The full list of supported profiles can be found in ``src/web/app/pom.xml``.

Access GeoServer front page
---------------------------

* After a few seconds, GeoServer should be accessible at: `<http://localhost:8080/geoserver>`_
* The default ``admin`` password is ``geoserver``.

Run GeoServer from Intellij on Windows
--------------------------------------

#. Add bash to your Windows environment path and restart Intellij.  
#. If there are errors such as "cannot find symbol AbstractUserGroupServiceTest", rebuild the security-tests project in the security module.  Right click on the security-tests project and click Rebuild.  
