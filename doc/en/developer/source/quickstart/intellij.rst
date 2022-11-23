.. _quickstart_intellij:

IntelliJ QuickStart
===================

.. include:: checkout.txt

Import modules into IntelliJ
----------------------------

#. Run the IntelliJ IDE
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

Before Running GeoServer
---------------------------

GeoServer relies on other libraries that are maintained in parallel under the same project umbrella. These are GeoTools and GeoWebCache. So, for easier installation and setup of the GeoServer development environment, you may want to apply the following:

#. Download the code of both and execute ``mvn clean install`` into them.
    `GeoTools <https://github.com/geotools/geotools>`_
    `GeoWebCache <https://github.com/geowebcache/geowebcache>`_
#. Afterwards do the same in the GeoServer src folder

Run GeoServer from IntelliJ
---------------------------

#. From the Project browser select the ``web-app`` module
#. Navigate to the ``org.geoserver.web`` package
#. Right-click the ``Start`` class and click to ``Run 'Start.main()'``

   .. image:: img/intellij_run.png
      :width: 400

#. The first time you do this, GeoServer will fail to start. Navigate to the ``Run`` menu, and click ``Edit Configurations...``.
#. Select the ``Start`` configuration, and append ``web/app`` to the ``Working Directory``.

   .. image:: img/intellij_run_config.png
      :width: 800

#. While you have the ``Edit Configurations`` dialog open, you can fine tune your launch environment (including setting a GEOSERVER_DATA_DIR). When you are happy with your settings, click ``OK``.
#. If there are errors such as "cannot find symbol class ASTAxisId", some generated code is not being included in the build.  Using wcs1_1 as the working directory, run a ``mvn clean install``.
#. If you get a compiler error like java.lang.NoSuchMethodError, it is most likely due to ``Error Prone`` tool which doesn't support Java 8. This tool is switched off by default, but sometimes it turns on after import to IntelliJ. There are two options to fix it:
    #. Go to Maven tool window and uncheck the ``errorprone`` profile, then click ``Reimport All Maven Projects``:

       .. image:: img/intellij_maven_errorprone.png
          :width: 400

    #. If you want to use ``errorprone``, notably to perform the QA checks, install the ``Error Prone Compiler`` plugin, restart the IDE and set ``Javac with error-prone`` as a default compiler for the project. Please note that this will slower the build.
#. If there are errors such as "cannot find symbol AbstractUserGroupServiceTest", rebuild the ``security-tests`` project in the security module.  Right-click on the ``security-tests`` project and click Rebuild.
#. In the last versions of IntelliJ Annotations processors are enabled. If there are errors because of this uncheck this option from compiler settings.

   .. image:: img/intellij_disable_annotation_processors.jpg
      :width: 800
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

Run GeoServer from IntelliJ on Windows
--------------------------------------

#. Add bash to your Windows environment path and restart IntelliJ.  
