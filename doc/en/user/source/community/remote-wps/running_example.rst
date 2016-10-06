.. _extensions_wps_remote_install_example:

A Remote "Gdal Contour" Process Binding Example
===============================================

Before continue reading this section, please be sure to have fully understood and successfully completed all the passages at sections:

- :ref:`extensions_wps_remote_install_geoserver`
- :ref:`extensions_wps_remote_install_xmpp`
- :ref:`extensions_wps_remote_install_python`

Running the Python WPS Agent
++++++++++++++++++++++++++++

In order to start the RemoteWPS Python Wrapper, we need to run an instance of the ``wpsagent.py`` using the configuration files defined at section :ref:`extensions_wps_remote_install_python`

.. code-block:: bash

  $> cd C:\work\RemoteWPS
  
  $> python wpsagent.py -r .\xmpp_data\configs\remote.config -s .\xmpp_data\configs\myservice\service.config service
  
Few instants after the execution of the command, you should be able to see con ``invite`` message on the prompt 

.. figure:: images/run_example001.jpg
  :align: center

and the ``default.GdalContour`` instance successfully connected and authenticated into the XMPP Server channels

.. figure:: images/run_example002.jpg
  :align: center

.. figure:: images/run_example003.jpg
  :align: center


The new GeoServer WPS Process should be now available among the GeoServer Processes

.. figure:: images/run_example004.jpg
  :align: center

The GeoServer Remote Process Factory automatically creates the WPS interface for the new process, exposing through the OGC WPS Protocol the Inputs and Outputs definitions like shown in the illustration below

.. figure:: images/run_example005.jpg
  :align: center

At the Execute Request the Remote WPS Python framework starts a new thread and assigns to it the unique **execution_id** provided by GeoServer.

.. figure:: images/run_example006.jpg
  :align: center

The logs of the execution are stored into the **working directory**

.. figure:: images/run_example007.jpg
  :align: center

From the log file is possible to recognize the full command line executed by the Remote WPS Python wrapper along with the lines received through the standard output

.. figure:: images/run_example008.jpg
  :align: center

The main window shows the received XMPP messages and the actions taken accordingly

.. figure:: images/run_example009.jpg
  :align: center

.. note:: The same information can be found into the log file specified into the “logger.properties” file (see above).

On GeoServer side, it is possible to follow the process execution by following the messages sent via XMPP to the GeoServer logs

.. code-block:: bash

  $> tail -F -n 200 /storage/data/logs/geoserver.log
  
.. figure:: images/run_example010.jpg
  :align: center
