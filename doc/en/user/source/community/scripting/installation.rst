.. _scripting_installation:

Installing the Scripting Extension
==================================

Each supported language is distributed as a different download. 

.. note::

   The various language runtime libraries increase GeoServers memory footprint, namely the "PermGen" 
   (Permanent Generation) space. If installing the scripting extension it is recommended that you 
   increase PermGen capacity to 128m. This is done with the option ``-XX:MaxPermSize=128m``. If 
   installing multiple language extensions this size may need to be increased even further.
   
#. Download the language extension of choice from the `GeoServer download page 
   <http://geoserver.org/display/GEOS/Download>`_.

   .. warning:: 

     Ensure the extension matching the version of the GeoServer installation is
     downloaded.

#. Extract the contents of the archive into the ``WEB-INF/lib`` directory of
   the GeoServer installation.

Verifying the Installation
---------------------------

To verify the extension has been installed properly start the GeoServer instance and navigate to the 
data directory. Upon a successful install a new directory named ``scripts`` will be created. You can 
also navigate to the GeoServer web app and a "Scripting" section of the navigation menu should now 
be present.