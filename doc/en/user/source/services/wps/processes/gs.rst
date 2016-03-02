.. _wps_gs_processes:

GeoServer processes
===================

GeoServer WPS includes a few processes created especially for use with GeoServer.  These are usually GeoServer-specific functions, such as bounds and reprojection.  They use an internal connection to the GeoServer WFS/WCS, not part of the WPS specification, for reading and writing data.

As with the "geo" processes, the names and definitions of these processes are subject to change, so they have not been included here.  For a full list of GeoServer-specific processes, please see the GeoServer :ref:`WPS capabilities document <wps_getcaps>` (or browse with the :ref:`wps_request_builder`.)

Aggregation process
-------------------

The aggregation process is used to perform common aggregation functions (sum, average, count) on vector data. The available outputs formats for this process are `text/xml` and `application/json`. 

The process parameters are described in the table bellow:

.. list-table::
   :widths: 10 80 5 5

   * - **Parameter**
     - **Description**
     - **Mandatory**
     - **Multiple**
   * - ``features``
     - Input feature collection.
     - yes
     - no
   * - ``aggregationAttribute``
     - Attribute on which to perform aggregation.
     - yes
     - no
   * - ``function``
     - An aggregate function to compute. Functions include Count, Average, Max, Median, Min, StdDev, and Sum.
     - yes
     - yes
   * - ``singlePass``
     - If TRUE computes all aggregation values in a single pass. This will defeat DBMS-specific optimizations. If a group by attribute is provided this parameter will be ignored.
     - yes
     - no
   * - ``groupByAttributes``
     - Group by attribute.
     - no
     - yes

Follow some examples of the invocation of this process using GeoServer shipped `topp:states` layer.

The examples can be tested with CURL:

.. code-block:: bash

   curl -u admin:geoserver -H 'Content-type: xml' -XPOST -d@'wps-request.xml' http://localhost:8080/geoserver/wps

where `wps-request.xml` is the file that contains the request.

Aggregate Example
'''''''''''''''''

Counts the total number of states, sum all the number of persons, computes the average number of persons per state and give the maximum and minimum number of persons in a state.

Request:

.. literalinclude:: ../../../../../../../data/release/demo/WPS_aggregate_1.0.xml
   :language: xml

The result:

.. code-block:: json

  {
    "AggregationAttribute": "PERSONS",
    "AggregationFunctions": ["Max", "Min", "Average", "Sum", "Count"],
    "GroupByAttributes": [],
    "AggregationResults": [
      [29760021, 453588, 5038397.020408163, 246881454, 49]
    ]
  }

The value of `AggregationResults` attribute should be read in a tabular way. The group by attributes come first in the order they appear in `GroupByAttributes` attribute. After comes the result of the aggregation functions in the order they appear in the `AggregationFunctions` attribute. In this case there is no group by attributes so the result only contains a row with the aggregation functions results. This is very similar to the result of an SQL query.

This result should be interpreted like this:

.. list-table::
   :widths: 20 20 20 20 20

   * - **Max**
     - **Min**
     - **Average**
     - **Sum**
     - **Count**
   * - 29760021
     - 453588
     - 5038397.020408163
     - 246881454
     - 49

To obtain the result in the XML format the request `wps:ResponseForm` element needs to be changed to:

.. code-block:: xml

  <wps:ResponseForm>
    <wps:RawDataOutput mimeType="text/xml">
      <ows:Identifier>result</ows:Identifier>
    </wps:RawDataOutput>
  </wps:ResponseForm>

The result in XML format:

.. code-block:: xml

  <?xml version="1.0" encoding="UTF-8"?>
  <AggregationResults>
    <Min>453588.0</Min>
    <Max>2.9760021E7</Max>
    <Average>5038397.020408163</Average>
    <Sum>2.46881454E8</Sum>
    <Count>49</Count>
  </AggregationResults>

Aggregate GroupBy Example
'''''''''''''''''''''''''

This example count the number of states and the population average grouped by region.

Request:

.. literalinclude:: ../../../../../../../data/release/demo/WPS_aggregate_groupby_1.0.xml
   :language: xml

The result:

.. code-block:: json

  { 
    "AggregationAttribute": "PERSONS",
    "AggregationFunctions": ["Average", "Count"],
    "GroupByAttributes": ["SUB_REGION"], 
    "AggregationResults": [ 
      [ "N Eng", 2201157.1666666665, 6 ], 
      [ "W N Cen", 2522812.8571428573, 7 ], 
      [ "Pacific", 12489678, 3 ], 
      [ "Mtn", 1690408.25, 8 ], 
      [ "E S Cen", 3998821.25, 4 ], 
      [ "S Atl", 4837695.666666667, 9 ], 
      [ "Mid Atl", 12534095.333333334, 3 ], 
      [ "E N Cen", 8209477.2, 5 ], 
      [ "W S Cen", 6709575.75, 4 ]
    ]
  }

Since there is a group by attribute the result contains a row for each different value of the group by attribute. Very similar to the result of an SQL query. If there is more that one group by attribute (which is not the case) their values will be in the order they appear in the `GroupByAttributes` attribute.

This result should be interpreted like this:

.. list-table::
   :widths: 34 33 33

   * - **Sub Region**
     - **Average**
     - **count**
   * - N Eng
     - 2201157.1666666665
     - 6
   * - W N Cen
     - 2522812.8571428573
     - 7
   * - Pacific
     - 12489678
     - 3
   * - Mtn
     - 1690408.25
     - 8
   * - E S Cen
     - 3998821.25
     - 4
   * - S Atl
     - 4837695.666666667
     - 9
   * - Mid Atl
     - 12534095.333333334
     - 3
   * - E N Cen
     - 8209477.2
     - 5
   * - W S Cen
     - 6709575.75
     - 4

The result in XML format:

.. code-block:: xml

  <?xml version="1.0" encoding="UTF-8"?>
  <AggregationResults>
    <GroupByResult>
      <object-array>
        <string>N Eng</string>
        <double>2201157.1666666665</double>
        <int>6</int>
      </object-array>
      <object-array>
        <string>W N Cen</string>
        <double>2522812.8571428573</double>
        <int>7</int>
      </object-array>
      <object-array>
        <string>Pacific</string>
        <double>1.2489678E7</double>
        <int>3</int>
      </object-array>
      <object-array>
        <string>Mtn</string>
        <double>1690408.25</double>
        <int>8</int>
      </object-array>
      <object-array>
        <string>E S Cen</string>
        <double>3998821.25</double>
        <int>4</int>
      </object-array>
      <object-array>
        <string>S Atl</string>
        <double>4837695.666666667</double>
        <int>9</int>
      </object-array>
      <object-array>
        <string>Mid Atl</string>
        <double>1.2534095333333334E7</double>
        <int>3</int>
      </object-array>
      <object-array>
        <string>E N Cen</string>
        <double>8209477.2</double>
        <int>5</int>
      </object-array>
      <object-array>
        <string>W S Cen</string>
        <double>6709575.75</double>
        <int>4</int>
      </object-array>
    </GroupByResult>
  </AggregationResults>