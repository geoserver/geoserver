.. _scripting_overview:

Scripting Extension Overview
============================

The scripting extension provides a number of extension points called "hooks" 
throughout GeoServer. Each hook provides a way to plug in functionality via 
a script. See the :ref:`scripting_hooks` section for details on each of the
individual scripting hooks.

Scripts are located in the GeoServer data directory under a directory named
``scripts``. Under this directory exist a number of other directories, one 
for each scripting hook::

  GEOSERVER_DATA_DIR/
    ...
    scripts/
      apps/
      lib/
      wps/
        
The ``apps`` directory provides an "application" hook allowing for one to 
provide a script invokable over http.

The ``wps`` directory provides a Web Processing Service (WPS) process 
hook to contribute a process invokable via WPS.

The ``lib`` directory is not a hook but is meant to be a location where 
common scripts may be placed. For instance this directory may be used as 
a common location for data structures and utility functions that may  be 
utilized across many different scripts. 

.. note:: How the ``lib`` directory (or if it is utilized at all) is 
          language specific.

See :ref:`scripting_hooks` for more details.

Creating scripts involves creating a script in one of these hook directories.
New scripts are picked up automatically by GeoServer without a need to ever
restart the server as is the case with a pure Java GeoServer extension.



