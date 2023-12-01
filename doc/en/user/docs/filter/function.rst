.. _filter_function:

Filter functions
================

The OGC Filter Encoding specification provides a generic concept of a *filter function*.  
A filter function is a named function with any number of arguments, which can be used in a filter expression to perform specific calculations.  
This provides much richer expressiveness for defining filters. 
Filter functions can be used in both the XML Filter Encoding language and
the textual ECQL language, using the syntax appropriate to the language.

GeoServer provides many different kinds of filter functions,  
covering a wide range of functionality including mathematics, string formatting, and geometric operations.
A complete list is provided in the :ref:`filter_function_reference`.


.. note:: The Filter Encoding specification provides a standard syntax for filter functions, but does not mandate a specific set of functions.  Servers are free to provide whatever functions they want, so some function expressions may work only in specific software.

Examples
--------

The following examples show how filter functions are used. 
The first shows enhanced WFS filtering using the ``geometryType`` function.  
The second shows how to use functions in SLD to get improved label rendering.

WFS filtering
^^^^^^^^^^^^^

Let's assume we have a feature type whose geometry field, ``geom``, can contain any kind of geometry. 
For a certain application we need to extract only the features whose geometry is a simple point or a multipoint.
This can be done using a GeoServer-specific filter function named ``geometryType``.
Here is the WFS request including the filter function:

.. code-block:: xml 

    <wfs:GetFeature service="WFS" version="1.0.0"
      outputFormat="GML2"
      xmlns:wfs="http://www.opengis.net/wfs"
      xmlns:ogc="http://www.opengis.net/ogc"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="http://www.opengis.net/wfs
                          http://schemas.opengis.net/wfs/1.0.0/WFS-basic.xsd">
      <wfs:Query typeName="sf:archsites">
        <ogc:Filter>
           <ogc:PropertyIsEqualTo>
              <ogc:Function name="geometryType">
                 <ogc:PropertyName>geom</ogc:PropertyName>
              </ogc:Function>
              <ogc:Literal>Point</ogc:Literal>
           </ogc:PropertyIsEqualTo>
        </ogc:Filter>
        </wfs:Query>
    </wfs:GetFeature>

WFS 2.0 namespaces
^^^^^^^^^^^^^^^^^^

WFS 2.0 does not depend on any one GML version and thus requires an explicit namespace and schemaLocation for GML.
This POST example selects features using a spatial query. Note the complete declaration of namespace prefixes.
In a GET request, namespaces can be placed on a Filter element.

.. code-block:: xml 

    <?xml version="1.0" encoding="UTF-8"?>
    <wfs:GetFeature service="WFS" version="2.0.0"
        xmlns:wfs="http://www.opengis.net/wfs/2.0"
        xmlns:fes="http://www.opengis.net/fes/2.0"
        xmlns:gml="http://www.opengis.net/gml/3.2"
        xmlns:sf="http://www.openplans.org/spearfish"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.opengis.net/wfs/2.0
                            http://schemas.opengis.net/wfs/2.0/wfs.xsd
                            http://www.opengis.net/gml/3.2
                            http://schemas.opengis.net/gml/3.2.1/gml.xsd">
        <wfs:Query typeNames="sf:bugsites">
            <fes:Filter>
                <fes:Not>
                    <fes:Disjoint>
                        <fes:ValueReference>sf:the_geom</fes:ValueReference>
                        <!-- gml:id is mandatory on GML 3.2 geometry elements -->
                        <gml:Polygon
                                gml:id="polygon.1"
                                srsName='http://www.opengis.net/def/crs/EPSG/0/26713'>
                            <gml:exterior>
                                <gml:LinearRing>
                                    <!-- pairs must form a closed ring -->
                                    <gml:posList>590431 4915204 590430
                                        4915205 590429 4915204 590430
                                        4915203 590431 4915204</gml:posList>
                                </gml:LinearRing>
                            </gml:exterior>
                        </gml:Polygon>
                    </fes:Disjoint>
                </fes:Not>
            </fes:Filter>
        </wfs:Query>
    </wfs:GetFeature>


SLD formatting
^^^^^^^^^^^^^^

We want to display elevation labels in a contour map. The elevations are stored as floating point values, so the raw numeric values may display with unwanted decimal places (such as "150.0" or "149.999999"). 
We want to ensure the numbers are rounded appropriately (i.e. to display "150"). 
To achieve this the ``numberFormat`` filter function can be used in the SLD label content expression:

.. code-block:: xml

     ...
     <TextSymbolizer>
       <Label>
         <ogc:Function name="numberFormat">
           <ogc:Literal>##</ogc:Literal>
           <ogc:PropertyName>ELEVATION</ogc:PropertyName>
         </ogc:Function>
       </Label>
       ...
     </TextSymbolizer>
     ...
     
Performance implications
------------------------

Using filter functions in SLD symbolizer expressions does not have significant overhead, unless the function is performing very heavy computation.

However, using functions in WFS filtering or SLD rule expressions may cause performance issues in certain cases. This is usually because specific filter functions are not recognized by a native data store filter encoder, and thus GeoServer must execute the functions in memory instead.

For example, given a filter like ``BBOX(geom,-10,30,20,45) and geometryType(geom) = 'Point'`` most data stores will split the filter into two separate parts. The bounding box filter will be encoded as a primary filter and executed in SQL, while the ``geometryType`` function will be executed in memory on the results coming from the primary filter.
