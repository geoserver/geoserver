.. geofence_wps_gui:

GeoFence WPS rules setup
========================

This plugin will not change the GUI in any way.
Anyway, you can now use the `subfield` field to select the processes you want to authorize.

For instance, with the following rules:

.. figure:: images/geofence-wps-example.png
   :align: center

you will enable all WPS processes for the `tasmania_cities` layer, but you will prevent to download it via WPS.


Chained processes
-----------------

Please note that this plugin also considers chained WPS processes, when they are running in the same GeoServer instance.  
If a user is running an `execute` request containing more than one chained process,
all processes will be needed to be allowed in order for the request to be run successfully.

For instance, if the user sends a request of this kind::


              /--> Proc2 --> Layer A
     Proc1 --|
              \              /--> Layer B
               \--> Proc3 --|
                             \--> Proc4 --> Layer C


the user will need at least these permissions:

- Proc1: LayerA + LayerB + LayerC
- Proc2: LayerA
- Proc3: LayerB + LayerC
- Proc4: LayerC
