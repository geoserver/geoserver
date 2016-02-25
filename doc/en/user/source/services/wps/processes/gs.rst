.. _wps_gs_processes:

GeoServer processes
===================

GeoServer WPS includes a few processes created especially for use with GeoServer.  These are usually GeoServer-specific functions, such as bounds and reprojection.  They use an internal connection to the GeoServer WFS/WCS, not part of the WPS specification, for reading and writing data.

As with the "geo" processes, the names and definitions of these processes are subject to change, so they have not been included here.  For a full list of GeoServer-specific processes, please see the GeoServer :ref:`WPS capabilities document <wps_getcaps>` (or browse with the :ref:`wps_request_builder`.)

Aggregation process
-------------------

The aggregation process is used to perform common aggregation functions (sum, average, count) on vector data.

WPS aggregate request WITHOUT group by attributes:

XML:

.. code-block:: xml

   <?xml version="1.0" encoding="UTF-8"?>
   <AggregationResults>
       <Max>500.0</Max>
   </AggregationResults>

JSON:

.. code-block:: json

   {
     "AggregationFunctions": ["Max"],
     "AggregationAttribute": "energy_consumption",
     "GroupByAttributes": [],
     "AggregationResults": [
       [500]
     ]
   }

WPS aggregate request WITH group by attributes:

XML:

.. code-block:: xml

   <?xml version="1.0" encoding="UTF-8"?>
   <AggregationResults>
       <GroupByResult>
           <object-array>
               <string>FABRIC</string>
               <big-decimal>500.0</big-decimal>
           </object-array>
           <object-array>
               <string>HOUSE</string>
               <big-decimal>6.0</big-decimal>
           </object-array>
           <object-array>
               <string>SCHOOL</string>
               <big-decimal>60.0</big-decimal>
           </object-array>
       </GroupByResult>
   </AggregationResults>

JSON:

.. code-block:: json

   {
     "AggregationFunctions": ["Max"],
     "AggregationAttribute": "energy_consumption",
     "GroupByAttributes": ["building_type"],
     "AggregationResults": [
       ["FABRIC",500],
       ["HOUSE",6],
       ["SCHOOL",60]
     ]
   }