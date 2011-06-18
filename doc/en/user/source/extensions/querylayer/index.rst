.. _extension_querylayer:

Cross layer filtering
=====================

The "querylayer" module adds a few extra filter functions that allow for cross layer filtering, that is, the ability to find in layer A features that have a certain relationship with features in layer B.
This can be used, for example, to find all bus stops within a certain distance from a shop, or all coffe shops in a certain city district.
Since filter functions are widely supported in GeoServer this cross layer filtering can be applied in SLDs, CQL filters and WFS requests alike.

Installing the 'querylayer' module
----------------------------------

#. Download the 'querylayer' extension corresponding to your version of GeoServer.

   .. warning:: Make sure to match the version of the extension to the version of the GeoServer instance!

#. Extract the contents of the archive into the ``WEB-INF/lib`` directory of the GeoServer installation.
#. To check the module is properly installed get the WFS 1.1 capabilities from the GS home page, the Filter_Capabilities section should contain a reference to a new function named 'queryCollection'

.. code-block:: xml 
   :linenos: 

    ...
    <ogc:Filter_Capabilities>
        ...
        <ogc:ArithmeticOperators>
          ...
          <ogc:Functions>
            <ogc:FunctionNames>
              ...
              <ogc:FunctionName nArgs="-1">queryCollection</ogc:FunctionName>
              <ogc:FunctionName nArgs="-1">querySingle</ogc:FunctionName>
              ...
            </ogc:FunctionNames>
          </ogc:Functions>
        </ogc:ArithmeticOperators>
      </ogc:Scalar_Capabilities>
      ...
    </ogc:Filter_Capabilities>
    ...

Function reference
------------------

.. list-table::
   :widths: 20 25 55
   
   
   * - **Name**
     - **Arguments**
     - **Description**
   * - querySingle
     - ``layer``: String, ``attribute``:String, ``filter``:String
     - Queries the specified ``layer``applying the specified (E)CQL ``filter`` and returns the value of ``attribute`` from the first feature in the result set. The layer name should be qualified (e.g. ``topp:states``), the filter can be ``INCLUDE`` if no filtering is desired
   * - queryCollection
     - ``layer``: String, ``attribute``:String, ``filter``:String
     - Queries the specified ``layer``applying the specified (E)CQL ``filter`` and returns the list of the values from ``attribute`` out of every single feature in the result set. The layer name should be qualified (e.g. ``topp:states``), the filter can be ``INCLUDE`` if no filtering is desired. Will throw an exception if too many results are being collected (see the memory limits section for details)
     
   * - collectGeometries
     - ``geometries``: a list of Geometry objects
     - Turns the list of geometries into a single Geometry object, suitable for being used as the reference geometry in spatial filters. Will throw an exception if too many coordinates are being collected (the results of queryCollection cannot be used as is)
     
Optimizing the module speed
---------------------------

In order to have the module run at full speed on the 2.1.x series it is necessary to add the
following parameter as a system variable when starting the Java Virtual Machine::

    -Dorg.geotools.filter.function.simplify=true 
    
This will make sure the functions are evaluated just once per query instead of once per feature matched by the filter. The flag is not necessary on trunk (2.2.x series) and hopefully this behavior will become the default on 2.1.x as well.
     
Memory limits
-------------

The query and geometry collection functions are not really performing a database style join, instead they do execute a query against the layer every time they are executed and load the result fully in memory.
Both ``queryCollection`` and ``collectGeometries`` are thus at risk of filling up the server memory with data if the layer being queries is large, or if the geometries being collected are few but very large.
Since this might threaten the server stability there are built in limits to what can be collected:

* at most ``1000`` features will be collected by ``queryCollection``
* at most ``37000`` coordinates (1MB worth of Coordinate objects) will be collected by ``collectGeometries``

Both limits can be overridden by setting appropriate values either as system variable, servlet context variables, or enviroment variables:

* set ``QUERY_LAYER_MAX_FEATURES`` to alter the max number of features collected by ``queryCollection``
* set ``GEOMETRY_COLLECT_MAX_COORDINATES`` to alter the max number of coordinates collected by ``collectGeometries``

WMS Examples
------------

The following examples use the ``sf:bugsites``, ``sf:roads`` and ``sf:restricted`` demo layers available in the standard GeoServer download.

**Get only the bugsites overlapping the restricted area whose category is ``3``**.
The CQL filter on bugsites is ``INTERSECTS(the_geom, querySingle('restricted', 'the_geom','cat = 3'))``, the full request is::

  http://localhost:8080/geoserver/wms?LAYERS=sf%3Aroads%2Csf%3Arestricted%2Csf%3Abugsites&STYLES=&FORMAT=image%2Fpng&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&EXCEPTIONS=application%2Fvnd.ogc.se_inimage&SRS=EPSG%3A26713&CQL_FILTER=INCLUDE%3BINCLUDE%3BINTERSECTS(the_geom%2C%20querySingle(%27restricted%27%2C%20%27the_geom%27%2C%27cat%20%3D%203%27))&BBOX=589081.6705629,4914128.1213261,609174.02430924,4928177.0717971&WIDTH=512&HEIGHT=358
  
and the result looks like:

.. figure:: images/bugsitesInRestricted.png
   :align: center

**Get all bugsides within 200 meters from roads**. The CQL filter looks like ``DWITHIN(the_geom, collectGeometries(queryCollection('sf:roads','the_geom','INCLUDE')), 200, meters)``, the full request is:

  http://localhost:8080/geoserver/wms?LAYERS=sf%3Aroads%2Csf%3Arestricted%2Csf%3Abugsites&STYLES=&FORMAT=image%2Fpng&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&EXCEPTIONS=application%2Fvnd.ogc.se_inimage&SRS=EPSG%3A26713&CQL_FILTER=INCLUDE%3BINCLUDE%3BDWITHIN(the_geom%2C%20collectGeometries(queryCollection(%27sf%3Aroads%27%2C%27the_geom%27%2C%27INCLUDE%27))%2C%20200%2C%20meters)&BBOX=589042.42768447,4914010.3926913,609134.78143081,4928059.3431623&WIDTH=512&HEIGHT=358
  
and the result looks liie:

.. figure:: images/bugsitesWithin.png
   :align: center

WFS Examples
------------

The following examples use the ``sf:bugsites``, ``sf:roads`` and ``sf:restricted`` demo layers available in the standard GeoServer download.

**Get only the bugsites overlapping the restricted area whose category is ``3``**:

.. code-block:: xml 
   :linenos: 

      <wfs:GetFeature xmlns:wfs="http://www.opengis.net/wfs"
                      xmlns:sf="http://www.openplans.org/spearfish"
                      xmlns:ogc="http://www.opengis.net/ogc"
                      service="WFS" version="1.0.0">
        <wfs:Query typeName="sf:bugsites">
          <ogc:Filter>
            <ogc:Intersects>
              <ogc:PropertyName>the_geom</ogc:PropertyName>
              <ogc:Function name="querySingle">
                 <ogc:Literal>sf:restricted</ogc:Literal>
                 <ogc:Literal>the_geom</ogc:Literal>
                 <ogc:Literal>cat = 3</ogc:Literal>
              </ogc:Function>
            </ogc:Intersects>
          </ogc:Filter>
        </wfs:Query>
      </wfs:GetFeature>

**Get all bugsides within 200 meters from roads**:

.. code-block:: xml 
   :linenos: 
  
      <wfs:GetFeature xmlns:wfs="http://www.opengis.net/wfs"
        xmlns:sf="http://www.openplans.org/spearfish"
        xmlns:ogc="http://www.opengis.net/ogc"
        service="WFS" version="1.0.0">
        <wfs:Query typeName="sf:bugsites">
          <ogc:Filter>
            <ogc:DWithin>
              <ogc:PropertyName>the_geom</ogc:PropertyName>
              <ogc:Function name="collectGeometries">
                <ogc:Function name="queryCollection">
                  <ogc:Literal>sf:roads</ogc:Literal>
                  <ogc:Literal>the_geom</ogc:Literal>
                  <ogc:Literal>INCLUDE</ogc:Literal>
                </ogc:Function>
              </ogc:Function>
              <ogc:Distance units="meter">100</ogc:Distance>
            </ogc:DWithin>
          </ogc:Filter>
        </wfs:Query>
      </wfs:GetFeature>
